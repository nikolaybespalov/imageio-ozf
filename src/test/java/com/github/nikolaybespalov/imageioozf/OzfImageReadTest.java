package com.github.nikolaybespalov.imageioozf;

import com.google.common.io.Resources;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Testing reading capabilities.
 */
public class OzfImageReadTest {

    /**
     * This test checks OZF2 image.
     */
    @Test
    public void readWorldOzf2() throws IOException {
        try (ImageInputStream is = new FileImageInputStream(FileUtils.toFile(Resources.getResource("com/github/nikolaybespalov/imageioozf/test-data/World.ozf2")))) {
            ImageReader reader = new OzfImageReader(null);

            reader.setInput(is);

            // checks basic reader capabilities
            assertEquals(499, reader.getWidth(0));
            assertEquals(250, reader.getHeight(0));
            assertEquals(8, reader.getNumImages(false));

            ImageReadParam param0 = new ImageReadParam();
            param0.setSourceRegion(new Rectangle(0, 0, 499, 250));
            BufferedImage image0 = reader.read(0, param0);
            assertEquals(499, image0.getWidth());
            assertEquals(250, image0.getHeight());

            ImageReadParam param1 = new ImageReadParam();
            param0.setSourceRegion(new Rectangle(0, 0, 374, 187));
            BufferedImage image1 = reader.read(1, param1);
            assertEquals(374, image1.getWidth());
            assertEquals(187, image1.getHeight());

            ImageReadParam param2 = new ImageReadParam();
            param0.setSourceRegion(new Rectangle(0, 0, 249, 125));
            BufferedImage image2 = reader.read(2, param2);
            assertEquals(249, image2.getWidth());
            assertEquals(125, image2.getHeight());

            ImageReadParam param3 = new ImageReadParam();
            param0.setSourceRegion(new Rectangle(0, 0, 166, 83));
            BufferedImage image3 = reader.read(3, param3);
            assertEquals(166, image3.getWidth());
            assertEquals(83, image3.getHeight());

            ImageReadParam param4 = new ImageReadParam();
            param0.setSourceRegion(new Rectangle(0, 0, 99, 50));
            BufferedImage image4 = reader.read(4, param4);
            assertEquals(99, image4.getWidth());
            assertEquals(50, image4.getHeight());

            ImageReadParam param5 = new ImageReadParam();
            param0.setSourceRegion(new Rectangle(0, 0, 49, 25));
            BufferedImage image5 = reader.read(5, param5);
            assertEquals(49, image5.getWidth());
            assertEquals(25, image5.getHeight());

            ImageReadParam param6 = new ImageReadParam();
            param0.setSourceRegion(new Rectangle(0, 0, 24, 12));
            BufferedImage image6 = reader.read(6, param6);
            assertEquals(24, image6.getWidth());
            assertEquals(12, image6.getHeight());

            ImageReadParam param7 = new ImageReadParam();
            param0.setSourceRegion(new Rectangle(0, 0, 12, 6));
            BufferedImage image7 = reader.read(7, param7);
            assertEquals(12, image7.getWidth());
            assertEquals(6, image7.getHeight());

            // check for expected exception when image index is out of bounds
            try {
                reader.read(8);
                fail();
            } catch (IndexOutOfBoundsException e) {
                // do nothing
            }
        }
    }

    /**
     * This test checks OZF3 image.
     */
    @Test
    public void readWorldOzf3() throws IOException {
        try (ImageInputStream is = new FileImageInputStream(FileUtils.toFile(Resources.getResource("com/github/nikolaybespalov/imageioozf/test-data/World.ozf3")))) {
            ImageReader reader = new OzfImageReader(null);

            reader.setInput(is);

            // checks basic reader capabilities
            assertEquals(2108, reader.getWidth(0));
            assertEquals(2048, reader.getHeight(0));
            assertEquals(5, reader.getNumImages(false));

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

    /**
     * This test checks OZF4 image.
     */
    @Ignore
    public void readWorldOzf4() throws IOException {
        try (ImageInputStream is = new FileImageInputStream(FileUtils.toFile(Resources.getResource("com/github/nikolaybespalov/imageioozf/test-data/World.ozf4")))) {
            ImageReader reader = new OzfImageReader(null);

            reader.setInput(is);

            // checks basic reader capabilities
            assertEquals(2108, reader.getWidth(0));
            assertEquals(2048, reader.getHeight(0));
            assertEquals(5, reader.getNumImages(false));
        }
    }
}