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

import static com.github.nikolaybespalov.imageioozf.OzfDecrypter.decrypt;

/**
 * https://docs.oracle.com/javase/8/docs/technotes/guides/imageio/spec/imageio_guideTOC.fm.html
 * <p>
 * TODO: Listeners for long operations
 *
 * @see <a href="https://trac.osgeo.org/gdal/browser/sandbox/klokan/ozf/ozf-binary-format-description.txt">ozf-binary-format-description.txt</a>
 */
class OzfImageReader extends ImageReader {
    private static final int FILE_HEADER_SIZE = 14;
    private static final int INITIAL_KEY_INDEX = 0x93;
    private static final int OZF_TILE_WIDTH = 64;
    private static final int OZF_TILE_HEIGHT = 64;
    private static final int OZF_ENCRYPTION_DEPTH = 16;
    private ImageInputStream stream;
    private ImageInputStream encryptedStream;
    private boolean headerRead = false;
    private boolean isOzf3;
    private byte key;
    //    private int width;
//    private int height;
//    private int bpp;
//    private int depth;
    private List<ZoomLevel> zoomLevels = new ArrayList<>();
    private List<ZoomLevel> thumbnails = new ArrayList<>();

    private class ZoomLevel {
        private final int width;
        private final int height;
        private final int xTiles;
        private final int yTiles;
        private final int[] tileOffsetTable;
        private final ColorModel cm;

        ZoomLevel(int width, int height, int xTiles, int yTiles, byte[] palette, int[] tileOffsetTable) {
            this.width = width;
            this.height = height;
            this.xTiles = xTiles;
            this.yTiles = yTiles;
            this.tileOffsetTable = tileOffsetTable;

            byte[] r = new byte[256];
            byte[] g = new byte[256];
            byte[] b = new byte[256];

            // B(0)G(1)R(2)_(3)
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
    public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
        stream = (ImageInputStream) input;
        stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public int getNumImages(boolean allowSearch) throws IOException {
        readHeader();

        return zoomLevels.size();
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        readHeader();

        checkImageIndex(imageIndex);

        return zoomLevels.get(imageIndex).width;
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        readHeader();

        checkImageIndex(imageIndex);

        return zoomLevels.get(imageIndex).height;
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        readHeader();

        checkImageIndex(imageIndex);

        ZoomLevel zoomLevel = this.zoomLevels.get(imageIndex);

        int[] bandOffset = new int[]{0};
        SampleModel sm = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, 1, 1, 1, 1, bandOffset);

        ImageTypeSpecifier imageTypeSpecifier = new ImageTypeSpecifier(zoomLevel.cm, sm);

        List<ImageTypeSpecifier> imageTypeSpecifiers = new ArrayList<>();

        imageTypeSpecifiers.add(imageTypeSpecifier);

        return imageTypeSpecifiers.iterator();
    }

    @Override
    public IIOMetadata getStreamMetadata() throws IOException {
        readHeader();

        return null;
    }

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
        readHeader();

        checkImageIndex(imageIndex);

        return null;
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        readHeader();

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

                if (y2 < d) {
                    ty2 = OZF_TILE_HEIGHT - (d - y2);
                }

                assert (ix2 - ix1) == (tx2 - tx1);

                copyPixels(tile, result, tx1, ty1, tx2, ty2, iy1, ix1, sourceRegion.width);
            }
        }

        Iterator<ImageTypeSpecifier> it = getImageTypes(imageIndex);

        assert it.hasNext();

        ImageTypeSpecifier its = it.next();

        ColorModel cm = its.getColorModel();
        SampleModel sm = its.getSampleModel(sourceRegion.width, sourceRegion.height);

        DataBuffer tileDataBuffer = new DataBufferByte(result, sourceRegion.width * sourceRegion.height);

        WritableRaster writableRaster = Raster.createWritableRaster(sm, tileDataBuffer, null);

        return new BufferedImage(cm, writableRaster, false, null);
    }

    @Override
    public boolean isImageTiled(int imageIndex) throws IOException {
        readHeader();

        checkImageIndex(imageIndex);

        return true;
    }

    @Override
    public int getTileWidth(int imageIndex) throws IOException {
        readHeader();

        checkImageIndex(imageIndex);

        return OZF_TILE_WIDTH;
    }

    @Override
    public int getTileHeight(int imageIndex) throws IOException {
        readHeader();

        checkImageIndex(imageIndex);

        return OZF_TILE_HEIGHT;
    }

    @Override
    public int getTileGridXOffset(int imageIndex) throws IOException {
        readHeader();

        checkImageIndex(imageIndex);

        return 0;
    }

    @Override
    public int getTileGridYOffset(int imageIndex) throws IOException {
        readHeader();

        checkImageIndex(imageIndex);

        return 0;
    }

    @Override
    public BufferedImage readTile(int imageIndex, int x, int y) throws IOException {
        readHeader();

        checkImageIndex(imageIndex);

        if (zoomLevels.get(imageIndex).xTiles < x || x < 0) {
            throw new IllegalArgumentException("bad x!");
        }

        if (zoomLevels.get(imageIndex).yTiles < y || y < 0) {
            throw new IllegalArgumentException("bad y!");
        }

        Iterator<ImageTypeSpecifier> it = getImageTypes(imageIndex);

        assert it.hasNext();

        ImageTypeSpecifier its = it.next();

        ColorModel cm = its.getColorModel();
        SampleModel sm = its.getSampleModel(OZF_TILE_WIDTH, OZF_TILE_HEIGHT);

        byte[] tileData = getTile(imageIndex, x, y);

        DataBuffer tileDataBuffer = new DataBufferByte(tileData, OZF_TILE_WIDTH * OZF_TILE_HEIGHT);

        WritableRaster writableRaster = Raster.createWritableRaster(sm, tileDataBuffer, null);

        return new BufferedImage(cm, writableRaster, false, null);
    }

    @Override
    public boolean readerSupportsThumbnails() {
        return true;
    }

    @Override
    public boolean hasThumbnails(int imageIndex) throws IOException {
        readHeader();

        checkImageIndex(imageIndex);

        return readerSupportsThumbnails();
    }

    @Override
    public int getNumThumbnails(int imageIndex) {
        return thumbnails.size();
    }

    @Override
    public int getThumbnailWidth(int imageIndex, int thumbnailIndex) throws IOException {
        readHeader();

        checkImageIndex(imageIndex);

        checkThumbnailIndex(thumbnailIndex);

        return thumbnails.get(thumbnailIndex).width;
    }

    @Override
    public int getThumbnailHeight(int imageIndex, int thumbnailIndex) throws IOException {
        readHeader();

        checkImageIndex(imageIndex);

        checkThumbnailIndex(thumbnailIndex);

        return thumbnails.get(thumbnailIndex).height;
    }

    @Override
    public BufferedImage readThumbnail(int imageIndex, int thumbnailIndex) throws IOException {
        readHeader();

        checkImageIndex(imageIndex);

        checkThumbnailIndex(thumbnailIndex);

        // TODO: implement me
        return super.readThumbnail(imageIndex, thumbnailIndex);
    }

    private void checkImageIndex(int imageIndex) {
        if (imageIndex < 0 || imageIndex >= zoomLevels.size()) {
            throw new IndexOutOfBoundsException("bad imageIndex!");
        }
    }

    private void checkThumbnailIndex(int thumbnailIndex) {
        if (thumbnailIndex < 0 || thumbnailIndex >= thumbnails.size()) {
            throw new IndexOutOfBoundsException("bad thumbnailIndex!");
        }
    }

    private void readHeader() throws IOException {
        if (headerRead) {
            return;
        }

        byte[] header = readFileHeader();

        isOzf3 = (header[0] == (byte) 0x80) && (header[1] == (byte) 0x77);

        if (isOzf3) {
            byte[] keyTable = readKeyTable();

            int keyTableSize = keyTable.length;

            if (keyTableSize < INITIAL_KEY_INDEX + 1) {
                throw new IllegalArgumentException("too few data!");
            }

            byte initialKey = keyTable[INITIAL_KEY_INDEX];

            key = (byte) ((initialKey + 0x8A) & 0xFF);

            encryptedStream = new OzfEncryptedStream(stream, key);

            decrypt(header, 0, 14, initialKey);

            encryptedStream.seek(14 + 1 + keyTableSize);
        }

        if (!(header[6] == (byte) 0x40 && header[7] == (byte) 0x00 &&
                header[8] == (byte) 0x01 && header[9] == (byte) 0x00 &&
                header[10] == (byte) 0x36 && header[11] == (byte) 0x04 &&
                header[12] == (byte) 0x00 && header[13] == (byte) 0x00)) {
            throw new IllegalArgumentException("an actual header is not equals expected");
        }

        readImagesInformation();

        headerRead = true;
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
        int zoomLevelTableOffset = readZoomLevelTableOffset();

        int imageTableSize = (int) stream.length() - zoomLevelTableOffset - 4;

        if (imageTableSize < 0) {
            throw new IOException("an actual table size is less than zero");
        }

        int images = imageTableSize / 4;

        stream.seek(zoomLevelTableOffset);

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

            if (isOzf3) {
                int tileSize = tileOffsetTable[1] - tileOffsetTable[0];

                stream.seek(tileOffsetTable[0]);

                byte[] tile = new byte[tileSize];

                stream.readFully(tile);
            }

            ZoomLevel zoomLevel = new ZoomLevel(width, height, xTiles, xyTiles, palette, tileOffsetTable);

            int maxWidthOrHeight = Math.max(width, height);

            if (maxWidthOrHeight == 300 || maxWidthOrHeight == 130) {
                thumbnails.add(zoomLevel);
            } else {
                zoomLevels.add(zoomLevel);
            }
        }
    }

    private int readZoomLevelTableOffset() throws IOException {
        stream.seek(stream.length() - 4);

        if (isOzf3) {
            return Integer.reverseBytes(encryptedStream.readInt());
        }

        return stream.readInt();
    }

    private int decompressTile(byte[] source, byte[] dest) throws IOException {
        InputStream inf = new InflaterInputStream(new ByteArrayInputStream(source));

        ByteArrayOutputStream bas = new ByteArrayOutputStream(OZF_TILE_WIDTH * OZF_TILE_HEIGHT);

        int decompressedBytes = IOUtils.copy(inf, bas);

        assert decompressedBytes == OZF_TILE_WIDTH * OZF_TILE_HEIGHT;

        System.arraycopy(bas.toByteArray(), 0, dest, 0, OZF_TILE_WIDTH * OZF_TILE_HEIGHT);

        return decompressedBytes;
    }

    private byte[] getTile(int imageIndex, int x, int y) throws IOException {
        ZoomLevel zoomLevel = this.zoomLevels.get(imageIndex);

        int i = y * zoomLevel.xTiles + x;

        int tileSize = zoomLevel.tileOffsetTable[i + 1] - zoomLevel.tileOffsetTable[i];

        byte[] tile = new byte[tileSize];

        stream.seek(zoomLevel.tileOffsetTable[i]);
        stream.readFully(tile);

        if (isOzf3) {
            decrypt(tile, 0, OZF_ENCRYPTION_DEPTH, key);
        }

        byte[] decompressedTile = new byte[OZF_TILE_WIDTH * OZF_TILE_HEIGHT];

        int n = decompressTile(tile, decompressedTile);

        assert n != -1;

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

    private void copyPixels(byte[] source, byte[] dest, int tx1, int ty1, int tx2, int ty2, int iy1, int ix1, int imageWidth) {
        int j = 0;

        for (int i = ty1; i < ty2; i++) {
            System.arraycopy(source, i * OZF_TILE_WIDTH + tx1, dest, (iy1 + j++) * imageWidth + ix1, tx2 - tx1);
        }
    }
}
