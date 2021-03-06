package com.github.nikolaybespalov.imageioozf;

import com.github.davidcarboni.ResourceUtils;
import org.junit.Test;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testing reading capabilities.
 */
public class OzfImageReadTest {

    /**
     * This test checks OZF2 image.
     */
    @Test
    public void readWorldOzf2() throws IOException {
        try (ImageInputStream is = new FileImageInputStream(ResourceUtils.getFile("/com/github/nikolaybespalov/imageioozf/test-data/World.ozf2"))) {
            ImageReader reader = new OzfImageReader(null);

            reader.setInput(is);

            // checks basic reader capabilities
            assertNull(reader.getStreamMetadata());

            assertEquals(8, reader.getNumImages(false));
            assertThrows(IndexOutOfBoundsException.class, () -> reader.read(123));
            assertNotNull(reader.readTile(0, 0, 0));
            assertThrows(IllegalArgumentException.class, () -> reader.readTile(0, 0, 123));
            assertThrows(IllegalArgumentException.class, () -> reader.readTile(0, 123, 0));
            assertEquals(64, reader.getTileWidth(0));
            assertEquals(64, reader.getTileHeight(0));
            assertEquals(0, reader.getTileGridXOffset(0));
            assertEquals(0, reader.getTileGridYOffset(0));

            assertTrue(reader.readerSupportsThumbnails());
            assertEquals(300, reader.getThumbnailWidth(0, 0));
            assertEquals(150, reader.getThumbnailHeight(0, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> reader.readThumbnail(0, 123));
            assertThrows(UnsupportedOperationException.class, () -> reader.readThumbnail(0, 0));

            // Image 0
            ImageReadParam param0 = new ImageReadParam();
            param0.setSourceRegion(new Rectangle(0, 0, 499, 250));
            BufferedImage image0 = reader.read(0, param0);
            assertEquals(499, image0.getWidth());
            assertEquals(250, image0.getHeight());
            assertTrue(reader.hasThumbnails(0));
            assertEquals(1, reader.getNumThumbnails(0));
            assertTrue(reader.isImageTiled(0));
            assertNull(reader.getImageMetadata(0));

            ImageReadParam param1 = new ImageReadParam();
            param0.setSourceRegion(new Rectangle(0, 0, 374, 187));
            BufferedImage image1 = reader.read(1, param1);
            assertEquals(374, image1.getWidth());
            assertEquals(187, image1.getHeight());
            assertTrue(reader.hasThumbnails(1));
            assertEquals(1, reader.getNumThumbnails(1));
            assertTrue(reader.isImageTiled(1));
            assertNull(reader.getImageMetadata(1));

            ImageReadParam param2 = new ImageReadParam();
            param0.setSourceRegion(new Rectangle(0, 0, 249, 125));
            BufferedImage image2 = reader.read(2, param2);
            assertEquals(249, image2.getWidth());
            assertEquals(125, image2.getHeight());
            assertTrue(reader.hasThumbnails(2));
            assertEquals(1, reader.getNumThumbnails(2));
            assertTrue(reader.isImageTiled(2));
            assertNull(reader.getImageMetadata(2));

            ImageReadParam param3 = new ImageReadParam();
            param0.setSourceRegion(new Rectangle(0, 0, 166, 83));
            BufferedImage image3 = reader.read(3, param3);
            assertEquals(166, image3.getWidth());
            assertEquals(83, image3.getHeight());
            assertTrue(reader.hasThumbnails(3));
            assertEquals(1, reader.getNumThumbnails(3));
            assertTrue(reader.isImageTiled(3));
            assertNull(reader.getImageMetadata(3));

            ImageReadParam param4 = new ImageReadParam();
            param0.setSourceRegion(new Rectangle(0, 0, 99, 50));
            BufferedImage image4 = reader.read(4, param4);
            assertEquals(99, image4.getWidth());
            assertEquals(50, image4.getHeight());
            assertTrue(reader.hasThumbnails(4));
            assertEquals(1, reader.getNumThumbnails(4));
            assertTrue(reader.isImageTiled(4));
            assertNull(reader.getImageMetadata(4));

            ImageReadParam param5 = new ImageReadParam();
            param0.setSourceRegion(new Rectangle(0, 0, 49, 25));
            BufferedImage image5 = reader.read(5, param5);
            assertEquals(49, image5.getWidth());
            assertEquals(25, image5.getHeight());
            assertTrue(reader.hasThumbnails(5));
            assertEquals(1, reader.getNumThumbnails(5));
            assertTrue(reader.isImageTiled(5));
            assertNull(reader.getImageMetadata(5));

            ImageReadParam param6 = new ImageReadParam();
            param0.setSourceRegion(new Rectangle(0, 0, 24, 12));
            BufferedImage image6 = reader.read(6, param6);
            assertEquals(24, image6.getWidth());
            assertEquals(12, image6.getHeight());
            assertTrue(reader.hasThumbnails(6));
            assertEquals(1, reader.getNumThumbnails(6));
            assertTrue(reader.isImageTiled(6));
            assertNull(reader.getImageMetadata(6));

            ImageReadParam param7 = new ImageReadParam();
            param0.setSourceRegion(new Rectangle(0, 0, 12, 6));
            BufferedImage image7 = reader.read(7, param7);
            assertEquals(12, image7.getWidth());
            assertEquals(6, image7.getHeight());
            assertTrue(reader.hasThumbnails(7));
            assertEquals(1, reader.getNumThumbnails(7));
            assertTrue(reader.isImageTiled(7));
            assertNull(reader.getImageMetadata(7));

            ImageReadParam param = new ImageReadParam();
            param.setSourceRegion(new Rectangle(12, 21, 21, 12));
            BufferedImage image = reader.read(0, param);
            assertNotNull(image);
            assertEquals(21, image.getWidth());
            assertEquals(12, image.getHeight());
        }
    }

    /**
     * This test checks OZF3 image.
     */
    @Test
    public void readWorldOzf3() throws IOException {
        try (ImageInputStream is = new FileImageInputStream(ResourceUtils.getFile("/com/github/nikolaybespalov/imageioozf/test-data/World.ozf3"))) {
            ImageReader reader = new OzfImageReader(null);

            reader.setInput(is);

            // checks basic reader capabilities
            assertEquals(2108, reader.getWidth(0));
            assertEquals(2048, reader.getHeight(0));
            assertEquals(5, reader.getNumImages(false));
            assertNotNull(reader.readTile(0, 0, 0));

            ImageReadParam param0 = new ImageReadParam();
            param0.setSourceRegion(new Rectangle(0, 0, 2108, 2048));
            BufferedImage image0 = reader.read(0, param0);
            assertEquals(2108, image0.getWidth());
            assertEquals(2048, image0.getHeight());

            ImageReadParam param1 = new ImageReadParam();
            param0.setSourceRegion(new Rectangle(0, 0, 527, 512));
            BufferedImage image1 = reader.read(1, param1);
            assertEquals(527, image1.getWidth());
            assertEquals(512, image1.getHeight());

            ImageReadParam param2 = new ImageReadParam();
            param0.setSourceRegion(new Rectangle(0, 0, 211, 205));
            BufferedImage image2 = reader.read(2, param2);
            assertEquals(211, image2.getWidth());
            assertEquals(205, image2.getHeight());

            ImageReadParam param3 = new ImageReadParam();
            param0.setSourceRegion(new Rectangle(0, 0, 105, 102));
            BufferedImage image3 = reader.read(3, param3);
            assertEquals(105, image3.getWidth());
            assertEquals(102, image3.getHeight());

            ImageReadParam param4 = new ImageReadParam();
            param0.setSourceRegion(new Rectangle(0, 0, 53, 51));
            BufferedImage image4 = reader.read(4, param4);
            assertEquals(53, image4.getWidth());
            assertEquals(51, image4.getHeight());

            // check for expected exception when image index is out of bounds
            try {
                reader.read(5);
                fail();
            } catch (IndexOutOfBoundsException e) {
                // do nothing
            }
        }
    }

//    /**
//     * This test checks OZF4 image.
//     */
//    @Ignore
//    public void readWorldOzf4() throws IOException {
//        try (ImageInputStream is = new FileImageInputStream(FileUtils.toFile(Resources.getResource("com/github/nikolaybespalov/imageioozf/test-data/World.ozf4")))) {
//            ImageReader reader = new OzfImageReader(null);
//
//            reader.setInput(is);
//
//            // checks basic reader capabilities
//            assertEquals(2108, reader.getWidth(0));
//            assertEquals(2048, reader.getHeight(0));
//            assertEquals(5, reader.getNumImages(false));
//        }
//    }

    /**
     * This test checks "short" OZF3 file.
     * <p>
     * This means that the size of the initialization key table is less than 0x94.
     */
    @Test
    public void readShortOzf3() throws IOException {
        try (ImageInputStream is = new FileImageInputStream(ResourceUtils.getFile("/com/github/nikolaybespalov/imageioozf/test-data/Short.ozf3"))) {
            ImageReader reader = new OzfImageReader(null);

            reader.setInput(is);

            // Codacy says "JUnit tests should include assert() or fail()".
            assertNotNull(reader);

            assertThrows(IllegalArgumentException.class, () -> assertNotNull(reader.readTile(0, 0, 0)));
        }
    }

    /**
     * This test checks "corrupted" OZF2 file.
     * <p>
     * This means that the header contains invalid values.
     * 78 77 00 00 00 00 40 00 01 00 36 04 00 00 vs
     * 78 77 00 00 00 00 FF 00 FF 00 FF FF 00 00
     */
    @Test
    public void testCorruptedOzf2() throws IOException {
        try (ImageInputStream is = new FileImageInputStream(ResourceUtils.getFile("/com/github/nikolaybespalov/imageioozf/test-data/Corrupted.ozf2"))) {
            ImageReader reader = new OzfImageReader(null);

            reader.setInput(is);

            // Codacy says "JUnit tests should include assert() or fail()".
            assertNotNull(reader);

            assertThrows(IllegalArgumentException.class, () -> assertNotNull(reader.readTile(0, 0, 0)));
        }
    }
}