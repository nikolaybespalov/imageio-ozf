package com.github.nikolaybespalov.imageioozf;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import static com.github.nikolaybespalov.imageioozf.OzfEncryptedStream.decrypt;

/**
 * @see <a href="https://trac.osgeo.org/gdal/browser/sandbox/klokan/ozf/ozf-binary-format-description.txt">ozf-binary-format-description.txt</a>
 */
class OzfImageReader extends ImageReader {
    private static final int FILE_HEADER_SIZE = 14;
    private static final int INITIAL_KEY_INDEX = 0x93;
    private static final int OZF3_HEADER_SIZE = 16;
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
        final byte[] palette;
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
        return imageInfos.size() - 2;
    }

    @Override
    public int getWidth(int i) {
        return width;
    }

    @Override
    public int getHeight(int i) {
        return height;
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int i) {
        ImageTypeSpecifier imageType = null;
        int datatype = DataBuffer.TYPE_BYTE;
        java.util.List<ImageTypeSpecifier> l = new ArrayList<>();
        int colorType = ColorSpace.TYPE_RGB;
        switch (colorType) {
            case ColorSpace.TYPE_RGB:
                ColorSpace rgb = ColorSpace.getInstance(ColorSpace.CS_sRGB);
                int[] bandOffsets = new int[3];
                bandOffsets[0] = 0;
                bandOffsets[1] = 1;
                bandOffsets[2] = 2;
                imageType = ImageTypeSpecifier.createInterleaved(rgb, bandOffsets, datatype, false, false);
                break;
        }
        l.add(imageType);
        return l.iterator();
    }

    @Override
    public IIOMetadata getStreamMetadata() {
        return null;
    }

    @Override
    public IIOMetadata getImageMetadata(int i) {
        return null;
    }

    @Override
    public BufferedImage read(int i, ImageReadParam imageReadParam) {
        return null;
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
                    throw new IOException("Too few data");
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

        byte[] decompressed = new byte[64 * 64];

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
        Inflater inflater = new Inflater();

        inflater.setInput(source);

        try {
            return inflater.inflate(dest);
        } catch (DataFormatException e) {
            return -1;
        }
    }
}
