package com.github.nikolaybespalov.imageioozf;

import org.apache.commons.io.IOUtils;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.InflaterInputStream;

import static com.github.nikolaybespalov.imageioozf.OzfEncryptedStream.decrypt;

/**
 * https://docs.oracle.com/javase/8/docs/technotes/guides/imageio/spec/imageio_guideTOC.fm.html
 *
 * @see <a href="https://trac.osgeo.org/gdal/browser/sandbox/klokan/ozf/ozf-binary-format-description.txt">ozf-binary-format-description.txt</a>
 */
class OzfImageReader extends ImageReader {
    private static final int FILE_HEADER_SIZE = 14;
    private static final int INITIAL_KEY_INDEX = 0x93;
    private static final int OZF_TILE_WIDTH = 64;
    private static final int OZF_TILE_HEIGHT = 64;
    private ImageInputStream stream;
    private ImageInputStream encryptedStream;
    private boolean isOzf3;
    private byte key;
    private int thumbnails;
    //    private int width;
//    private int height;
//    private int bpp;
//    private int depth;
    private List<ImageInfo> imageInfo = new ArrayList<>();

    class ImageInfo {
        final int offset;
        final int width;
        final int height;
        final int xTiles;
        final int yTiles;
        final byte[] palette; // B(0)G(1)R(2)_(3)
        final int[] tileOffsetTable;
        final int encryptionDepth;
        final ColorModel cm;

        ImageInfo(int offset, int width, int height, int xTiles, int yTiles, byte[] palette, int[] tileOffsetTable, int encryptionDepth) {
            this.offset = offset;
            this.width = width;
            this.height = height;
            this.xTiles = xTiles;
            this.yTiles = yTiles;
            this.palette = palette;
            this.tileOffsetTable = tileOffsetTable;
            this.encryptionDepth = encryptionDepth;

            byte[] r = new byte[256];
            byte[] g = new byte[256];
            byte[] b = new byte[256];

            for (int i = 0; i < 256; i++) {
                r[i] = palette[i * 4 + 2];
                g[i] = palette[i * 4 + 1];
                b[i] = palette[i * 4];
            }

            this.cm = new IndexColorModel(8, 256, r, g, b);
        }
    }

    OzfImageReader(ImageReaderSpi imageReaderSpi) {
        super(imageReaderSpi);
    }

    @Override
    public int getNumImages(boolean allowSearch) {
        return imageInfo.size() - thumbnails;
    }

    @Override
    public int getWidth(int imageIndex) {
        checkImageIndex(imageIndex);

        return imageInfo.get(imageIndex).width;
    }

    @Override
    public int getHeight(int imageIndex) {
        checkImageIndex(imageIndex);

        return imageInfo.get(imageIndex).height;
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) {
        checkImageIndex(imageIndex);

        ImageInfo imageInfo = this.imageInfo.get(imageIndex);

        int[] bandOffset = new int[]{0};
        SampleModel sm = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, 1, 1, 1, 1, bandOffset);

        ImageTypeSpecifier imageTypeSpecifier = new ImageTypeSpecifier(imageInfo.cm, sm);

        java.util.List<ImageTypeSpecifier> imageTypeSpecifiers = new ArrayList<>();

        imageTypeSpecifiers.add(imageTypeSpecifier);

        return imageTypeSpecifiers.iterator();
    }

    @Override
    public IIOMetadata getStreamMetadata() {
        return null;
    }

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) {
        checkImageIndex(imageIndex);

        return null;
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        checkImageIndex(imageIndex);

        Rectangle sourceRegion = getSourceRegion(param, getWidth(imageIndex), getHeight(imageIndex));

        byte[] result = new byte[sourceRegion.width * sourceRegion.height];

        int xTiles = (sourceRegion.x + sourceRegion.width + OZF_TILE_WIDTH - 1) / OZF_TILE_WIDTH;
        int yTiles = (sourceRegion.y + sourceRegion.height + OZF_TILE_HEIGHT - 1) / OZF_TILE_HEIGHT;

        int xTileIndex = sourceRegion.x / OZF_TILE_WIDTH;
        int yTileIndex = sourceRegion.y / OZF_TILE_HEIGHT;

        int x1 = sourceRegion.x;
        int y1 = sourceRegion.y;

        int x2 = sourceRegion.x + sourceRegion.width;
        int y2 = sourceRegion.y + sourceRegion.height;

        for (int y = yTileIndex; y < yTiles; y++) {
            for (int x = xTileIndex; x < xTiles; x++) {
                byte[] tile = getTile(imageIndex, x, y);

                int a = x * OZF_TILE_WIDTH;
                int b = y * OZF_TILE_HEIGHT;
                int c = (x + 1) * OZF_TILE_WIDTH;
                int d = (y + 1) * OZF_TILE_HEIGHT;

                int tx1 = 0;
                int ix1 = 0;

                if (a < x1) {
                    tx1 = x1 - a;
                } else {
                    ix1 = a - x1;
                }

                int tx2 = OZF_TILE_WIDTH;
                int ix2 = c - x1;

                if (x2 < c) {
                    tx2 = OZF_TILE_WIDTH - (c - x2);
                    ix2 = sourceRegion.width;
                }

                int ty1 = 0;
                int iy1 = 0;

                if (b < y1) {
                    ty1 = y1 - b;
                } else {
                    iy1 = b - y1;
                }

                int ty2 = OZF_TILE_HEIGHT;
                int iy2 = d - y1;

                if (y2 < d) {
                    ty2 = OZF_TILE_HEIGHT - (d - y2);
                    iy2 = sourceRegion.height;
                }

                assert (ix2 - ix1) == (tx2 - tx1);

                copyPixels(tile, result, tx1, ty1, tx2, ty2, iy1, ix1, iy2, sourceRegion.width);
            }
        }

        Iterator<ImageTypeSpecifier> it = getImageTypes(imageIndex);

        if (!it.hasNext()) {
            throw new IllegalArgumentException("bad iterator!");
        }

        ImageTypeSpecifier its = it.next();

        ColorModel cm = its.getColorModel();
        SampleModel sm = its.getSampleModel(sourceRegion.width, sourceRegion.height);

        DataBuffer tileDataBuffer = new DataBufferByte(result, sourceRegion.width * sourceRegion.height);

        WritableRaster writableRaster = Raster.createWritableRaster(sm, tileDataBuffer, null);

        return new BufferedImage(cm, writableRaster, false, null);
    }

    @Override
    public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
        super.setInput(input, seekForwardOnly, ignoreMetadata);

        if (!(input instanceof ImageInputStream)) {
            throw new IllegalArgumentException("input not an ImageInputStream!");
        }

        stream = (ImageInputStream) input;
        stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);

        try {
            byte[] header = readFileHeader();

            isOzf3 = (header[0] == (byte) 0x80) && (header[1] == (byte) 0x77);

            if (isOzf3) {
                byte[] keyTable = readKeyTable();

                int keyTableSize = keyTable.length;

                if (keyTableSize < INITIAL_KEY_INDEX + 1) {
                    throw new IOException("too few data!");
                }

                byte initialKey = keyTable[INITIAL_KEY_INDEX];

                key = (byte) ((initialKey + 0x8A) & 0xFF);

                encryptedStream = new OzfEncryptedStream(stream, key);
            }

            readImagesInformation();

            for (ImageInfo imageInfo : imageInfo) {
                int maxHeightOrWidth = Math.max(imageInfo.width, imageInfo.height);

                if (maxHeightOrWidth == 300 || maxHeightOrWidth == 130) {
                    thumbnails++;
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private byte[] readFileHeader() throws IOException {
        stream.seek(0);

        byte[] header = new byte[14];

        stream.readFully(header);

        return header;
    }

    private byte[] readKeyTable() throws IOException {
        stream.seek(FILE_HEADER_SIZE);

        int n = stream.read();

        byte[] b = new byte[n];

        stream.readFully(b);

        return b;
    }

    private void readImagesInformation() throws IOException {
        int imageTableOffset = readImageTableOffset();

        int imageTableSize = (int) stream.length() - imageTableOffset - 4;

        int images = imageTableSize / 4;

        stream.seek(imageTableOffset);

        int[] imageOffsetTable = new int[images];

        for (int imageIndex = 0; imageIndex < images; imageIndex++) {
            if (isOzf3) {
                imageOffsetTable[imageIndex] = Integer.reverseBytes(encryptedStream.readInt());
            } else {
                imageOffsetTable[imageIndex] = stream.readInt();
            }
        }

        for (int imageIndex = 0; imageIndex < images; imageIndex++) {
            int imageOffset = imageOffsetTable[imageIndex];

            stream.seek(imageOffset);

            int width;
            int height;
            short xTiles;
            short xyTiles;
            byte[] palette = new byte[1024];

            if (isOzf3) {
                width = Integer.reverseBytes(encryptedStream.readInt());
                height = Integer.reverseBytes(encryptedStream.readInt());
                xTiles = Short.reverseBytes(encryptedStream.readShort());
                xyTiles = Short.reverseBytes(encryptedStream.readShort());
                encryptedStream.readFully(palette);
            } else {
                width = stream.readInt();
                height = stream.readInt();
                xTiles = stream.readShort();
                xyTiles = stream.readShort();
                stream.readFully(palette);
            }

            int tiles = xTiles * xyTiles + 1;

            int[] tileOffsetTable = new int[tiles];

            for (int tileIndex = 0; tileIndex < tiles; tileIndex++) {
                if (isOzf3) {
                    tileOffsetTable[tileIndex] = Integer.reverseBytes(encryptedStream.readInt());
                } else {
                    tileOffsetTable[tileIndex] = stream.readInt();
                }
            }

            int encryptionDepth = -1;

            if (isOzf3) {
                int tileSize = tileOffsetTable[1] - tileOffsetTable[0];

                stream.seek(tileOffsetTable[0]);

                byte[] tile = new byte[tileSize];

                stream.readFully(tile);

                encryptionDepth = getEncryptionDepth(tile, tileSize, key);
            }

            imageInfo.add(new ImageInfo(imageOffset, width, height, xTiles, xyTiles, palette, tileOffsetTable, encryptionDepth));
        }
    }

    private int readImageTableOffset() throws IOException {
        stream.seek(stream.length() - 4);

        if (isOzf3) {
            return Integer.reverseBytes(encryptedStream.readInt());
        }

        return stream.readInt();
    }

    private int getEncryptionDepth(byte[] data, int size, byte key) {
        int encryptionDepth = -1;

        byte[] decompressed = new byte[OZF_TILE_WIDTH * OZF_TILE_HEIGHT];

        byte[] dataCopy = new byte[size];

        for (int i = 4; i <= size; i++) {
            System.arraycopy(data, 0, dataCopy, 0, size);

            decrypt(dataCopy, 0, i, key);

            if (decompressTile(dataCopy, decompressed) != -1) {
                encryptionDepth = i;
                break;
            }
        }

        if (encryptionDepth == size) {
            encryptionDepth = -1;
        }

        return encryptionDepth;
    }

    private int decompressTile(byte[] source, byte[] dest) {
        InputStream inf = new InflaterInputStream(new ByteArrayInputStream(source));

        ByteArrayOutputStream bas = new ByteArrayOutputStream(OZF_TILE_WIDTH * OZF_TILE_HEIGHT);

        try {
            int decompressedBytes = IOUtils.copy(inf, bas);

            assert decompressedBytes == OZF_TILE_WIDTH * OZF_TILE_HEIGHT;

            System.arraycopy(bas.toByteArray(), 0, dest, 0, OZF_TILE_WIDTH * OZF_TILE_HEIGHT);

            return decompressedBytes;
        } catch (IOException e) {
            return -1;
        }
    }

    private void checkImageIndex(int imageIndex) {
        if (imageIndex < 0 || imageIndex >= imageInfo.size() - thumbnails) {
            throw new IndexOutOfBoundsException("bad index!");
        }
    }

    @Override
    public BufferedImage readTile(int imageIndex, int x, int y) throws IOException {
        checkImageIndex(imageIndex);

        Iterator<ImageTypeSpecifier> it = getImageTypes(imageIndex);

        if (!it.hasNext()) {
            throw new IllegalArgumentException("bad iterator!");
        }

        ImageTypeSpecifier its = it.next();

        ColorModel cm = its.getColorModel();
        SampleModel sm = its.getSampleModel(OZF_TILE_WIDTH, OZF_TILE_HEIGHT);

        byte[] tileData = getTile(imageIndex, x, y);

        DataBuffer tileDataBuffer = new DataBufferByte(tileData, OZF_TILE_WIDTH * OZF_TILE_HEIGHT);

        WritableRaster writableRaster = Raster.createWritableRaster(sm, tileDataBuffer, null);

        return new BufferedImage(cm, writableRaster, false, null);
    }

    private byte[] getTile(int imageIndex, int x, int y) throws IOException {
        checkImageIndex(imageIndex);

        ImageInfo imageInfo = this.imageInfo.get(imageIndex);

        int i = y * imageInfo.xTiles + x;

        int tileSize = imageInfo.tileOffsetTable[i + 1] - imageInfo.tileOffsetTable[i];

        byte[] tile = new byte[tileSize];

        stream.seek(imageInfo.tileOffsetTable[i]);
        stream.readFully(tile);

        if (isOzf3) {
            if (imageInfo.encryptionDepth == -1) {
                decrypt(tile, 0, tileSize, key);
            } else {
                decrypt(tile, 0, imageInfo.encryptionDepth, key);
            }
        }

        byte[] decompressedTile = new byte[OZF_TILE_WIDTH * OZF_TILE_HEIGHT];

        int n = decompressTile(tile, decompressedTile);

        if (n == -1) {
            return null;
        }

        // flip vertical
        for (int lineIndex = 0; lineIndex < OZF_TILE_HEIGHT / 2; lineIndex++) {
            byte[] temp = new byte[OZF_TILE_WIDTH];

            int topPosition = lineIndex * OZF_TILE_WIDTH;
            int downPosition = (OZF_TILE_HEIGHT - 1 - lineIndex) * OZF_TILE_WIDTH;

            System.arraycopy(decompressedTile, topPosition, temp, 0, OZF_TILE_WIDTH);

            System.arraycopy(decompressedTile, downPosition, decompressedTile, topPosition, OZF_TILE_WIDTH);

            System.arraycopy(temp, 0, decompressedTile, downPosition, OZF_TILE_WIDTH);
        }

        return decompressedTile;
    }

    private void copyPixels(byte[] source, byte[] dest, int tx1, int ty1, int tx2, int ty2, int iy1, int ix1, int iy2, int imageWidth) {
        for (int i = ty1, j = 0; i < ty2 && j < iy2 - iy1; i++, j++) {
            System.arraycopy(source, i * OZF_TILE_WIDTH + tx1, dest, (iy1 + j) * imageWidth + ix1, tx2 - tx1);
        }
    }
}
