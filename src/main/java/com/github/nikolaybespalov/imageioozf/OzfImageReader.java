package com.github.nikolaybespalov.imageioozf;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.*;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
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
    private static final int OZF3_HEADER_SIZE = 16;
    private static final int THUMBNAILS = 2;
    private static final int OZF_TILE_WIDTH = 64;
    private static final int OZF_TILE_HEIGHT = 64;
    private ImageInputStream stream;
    private ImageInputStream encryptedStream;
    private boolean isOzf3;
    private byte key;
    private int width;
    private int height;
    //private int bpp;
    //private int depth;
    private List<ImageInfo> imageInfos = new ArrayList<>();

    class ImageInfo {
        final int offset;
        final int width;
        final int height;
        final int xTiles;
        final int yTiles;
        final byte[] palette; // B(0)G(1)R(2)_(3)
        final int[] tileOffsetTable;
        final int encryptionDepth;

        ImageInfo(int offset, int width, int height, int xTiles, int yTiles, byte[] palette, int[] tileOffsetTable, int encryptionDepth) {
            this.offset = offset;
            this.width = width;
            this.height = height;
            this.xTiles = xTiles;
            this.yTiles = yTiles;
            this.palette = palette;
            this.tileOffsetTable = tileOffsetTable;
            this.encryptionDepth = encryptionDepth;
        }
    }

    OzfImageReader(ImageReaderSpi imageReaderSpi) {
        super(imageReaderSpi);
    }

    @Override
    public int getNumImages(boolean allowSearch) {
        return imageInfos.size() - THUMBNAILS;
    }

    @Override
    public int getWidth(int imageIndex) {
        checkImageIndex(imageIndex);

        return imageInfos.get(imageIndex).width;
    }

    @Override
    public int getHeight(int imageIndex) {
        checkImageIndex(imageIndex);

        return imageInfos.get(imageIndex).height;
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) {
        checkImageIndex(imageIndex);

        ImageInfo imageInfo = imageInfos.get(imageIndex);

        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];

        for (int i = 0; i < 256; i++) {
            r[i] = imageInfo.palette[i * 4 + 2];
            g[i] = imageInfo.palette[i * 4 + 1];
            b[i] = imageInfo.palette[i * 4];
        }

        ColorModel cm = new IndexColorModel(8, 256, r, g, b);

        int[] bandOffset = new int[]{0};
        SampleModel sm = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, 1, 1, 1, 1, bandOffset);

        ImageTypeSpecifier imageTypeSpecifier = new ImageTypeSpecifier(cm, sm);

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

        return readTile(imageIndex, 0, 7);

//        // Compute initial source region, clip against destination later
//        Rectangle sourceRegion = getSourceRegion(param, width, height);
//
//        // Set everything to default values
//        int sourceXSubsampling = 1;
//        int sourceYSubsampling = 1;
//        int[] sourceBands = null;
//        int[] destinationBands = null;
//        Point destinationOffset = new Point(0, 0);
//
//        // Get values from the ImageReadParam, if any
//        if (param != null) {
//            sourceXSubsampling = param.getSourceXSubsampling();
//            sourceYSubsampling = param.getSourceYSubsampling();
//            sourceBands = param.getSourceBands();
//            destinationBands = param.getDestinationBands();
//            destinationOffset = param.getDestinationOffset();
//        }
//
//        // Get the specified detination image or create a new one
//        BufferedImage dst = getDestination(param, getImageTypes(0), width, height);
//        // Enure band settings from param are compatible with images
//        int inputBands = 1;
//        checkReadParamBandSettings(param, inputBands, dst.getSampleModel().getNumBands());
//
//        int[] bandOffsets = new int[inputBands];
//        for (int i = 0; i < inputBands; i++) {
//            bandOffsets[i] = i;
//        }
//        int bytesPerRow = width * inputBands;
//        DataBufferByte rowDB = new DataBufferByte(bytesPerRow);
//        WritableRaster rowRas =
//                Raster.createInterleavedRaster(rowDB,
//                        width, 1, bytesPerRow,
//                        inputBands, bandOffsets,
//                        new Point(0, 0));
//        byte[] rowBuf = rowDB.getData();
//
//        // Create an int[] that can a single pixel
//        int[] pixel = rowRas.getPixel(0, 0, (int[]) null);
//
//        WritableRaster imRas = dst.getWritableTile(0, 0);
//        int dstMinX = imRas.getMinX();
//        int dstMaxX = dstMinX + imRas.getWidth() - 1;
//        int dstMinY = imRas.getMinY();
//        int dstMaxY = dstMinY + imRas.getHeight() - 1;
//
//        // Create a child raster exposing only the desired source bands
//        if (sourceBands != null) {
//            rowRas = rowRas.createWritableChild(0, 0,
//                    width, 1,
//                    0, 0,
//                    sourceBands);
//        }
//
//        // Create a child raster exposing only the desired dest bands
//        if (destinationBands != null) {
//            imRas = imRas.createWritableChild(0, 0,
//                    imRas.getWidth(),
//                    imRas.getHeight(),
//                    0, 0,
//                    destinationBands);
//        }
//
//        for (int srcY = 0; srcY < height; srcY++) {
//            // Read the row
////            try {
////                stream.readFully(rowBuf);
////            } catch (IOException e) {
////                throw new IIOException("Error reading line " + srcY, e);
////            }
//
//            // Reject rows that lie outside the source region,
//            // or which aren't part of the subsampling
//            if ((srcY < sourceRegion.y) ||
//                    (srcY >= sourceRegion.y + sourceRegion.height) ||
//                    (((srcY - sourceRegion.y) %
//                            sourceYSubsampling) != 0)) {
//                continue;
//            }
//
//            // Determine where the row will go in the destination
//            int dstY = destinationOffset.y +
//                    (srcY - sourceRegion.y) / sourceYSubsampling;
//            if (dstY < dstMinY) {
//                continue; // The row is above imRas
//            }
//            if (dstY > dstMaxY) {
//                break; // We're done with the image
//            }
//
//            // Copy each (subsampled) source pixel into imRas
//            for (int srcX = sourceRegion.x;
//                 srcX < sourceRegion.x + sourceRegion.width;
//                 srcX++) {
//                if (((srcX - sourceRegion.x) % sourceXSubsampling) != 0) {
//                    continue;
//                }
//                int dstX = destinationOffset.x +
//                        (srcX - sourceRegion.x) / sourceXSubsampling;
//                if (dstX < dstMinX) {
//                    continue;  // The pixel is to the left of imRas
//                }
//                if (dstX > dstMaxX) {
//                    break; // We're done with the row
//                }
//
//                // Copy the pixel, sub-banding is done automatically
//                rowRas.getPixel(srcX, 0, pixel);
//                imRas.setPixel(dstX, dstY, pixel);
//            }
//        }
//
//        return dst;
    }

    @Override
    public void setInput(Object input) {
        super.setInput(input);

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

                readOzf3Header(keyTableSize);

                int imageTableOffset = readImageTableOffset();

                int imageTableSize = (int) stream.length() - imageTableOffset - 4;

                int images = imageTableSize / 4;

                stream.seek(imageTableOffset);

                int[] imageOffsetTable = new int[images];

                for (int imageIndex = 0; imageIndex < images; imageIndex++) {
                    imageOffsetTable[imageIndex] = Integer.reverseBytes(encryptedStream.readInt());
                }

                for (int imageIndex = 0; imageIndex < images; imageIndex++) {
                    int imageOffset = imageOffsetTable[imageIndex];

                    stream.seek(imageOffset);

                    int width = Integer.reverseBytes(encryptedStream.readInt());
                    int height = Integer.reverseBytes(encryptedStream.readInt());
                    short xTiles = Short.reverseBytes(encryptedStream.readShort());
                    short xyTiles = Short.reverseBytes(encryptedStream.readShort());
                    byte[] palette = new byte[1024];
                    encryptedStream.readFully(palette);

                    int tiles = xTiles * xyTiles + 1;

                    int[] tileOffsetTable = new int[tiles];

                    for (int tileIndex = 0; tileIndex < tiles; tileIndex++) {
                        tileOffsetTable[tileIndex] = Integer.reverseBytes(encryptedStream.readInt());
                    }

                    int tileSize = tileOffsetTable[1] - tileOffsetTable[0];

                    stream.seek(tileOffsetTable[0]);

                    byte[] tile = new byte[tileSize];

                    stream.readFully(tile);

                    int encryptionDepth = getEncryptionDepth(tile, tileSize, key);

                    imageInfos.add(new ImageInfo(imageOffset, width, height, xTiles, xyTiles, palette, tileOffsetTable, encryptionDepth));
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

    private void readOzf3Header(int keyTableSize) throws IOException {
        encryptedStream.seek(FILE_HEADER_SIZE + keyTableSize + 1 + 4);

        //encryptedStream.skipBytes(4);

        byte[] ozf3Header = new byte[OZF3_HEADER_SIZE];

        encryptedStream.readFully(ozf3Header);

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(ozf3Header));

        // skip size field
        dis.skipBytes(4);
        width = Integer.reverseBytes(dis.readInt());
        height = Integer.reverseBytes(dis.readInt());
//        depth = Short.reverseBytes(dis.readShort());
//        bpp = Short.reverseBytes(dis.readShort());
    }

    private int readImageTableOffset() throws IOException {
        stream.seek(stream.length() - 4);

        return Integer.reverseBytes(encryptedStream.readInt());
    }

    private int getEncryptionDepth(byte[] data, int size, byte key) {
        int encryptionDepth = -1;

        byte[] decompressed = new byte[OZF_TILE_WIDTH * OZF_TILE_HEIGHT];

        byte[] dataCopy = new byte[size];

        for (int i = 4; i <= size; i++) {
            System.arraycopy(data, 0, dataCopy, 0, size);

            decrypt(dataCopy, 0, i, key);

            if (decompressTile(decompressed, dataCopy) != -1) {
                encryptionDepth = i;
                break;
            }
        }

        if (encryptionDepth == size) {
            encryptionDepth = -1;
        }

        return encryptionDepth;
    }

    private int decompressTile(byte[] dest, byte[] source) {
        InputStream inf = new InflaterInputStream(new ByteArrayInputStream(source));

        try {
            return inf.read(dest);
        } catch (IOException e) {
            return -1;
        }
    }

    private void checkImageIndex(int imageIndex) {
        if (imageIndex < 0 || imageIndex >= imageInfos.size() - THUMBNAILS) {
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

        ImageInfo imageInfo = imageInfos.get(imageIndex);

        int i = y * imageInfo.xTiles + x;

        int tileSize = imageInfo.tileOffsetTable[i + 1] - imageInfo.tileOffsetTable[i];

        byte[] tile = new byte[tileSize];

        stream.seek(imageInfo.tileOffsetTable[i]);
        stream.readFully(tile);

        if (imageInfo.encryptionDepth == -1) {
            decrypt(tile, 0, tileSize, key);
        } else {
            decrypt(tile, 0, imageInfo.encryptionDepth, key);
        }

        byte[] decompressedTile = new byte[OZF_TILE_WIDTH * OZF_TILE_HEIGHT];

        int n = decompressTile(decompressedTile, tile);

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
}
