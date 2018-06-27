package com.github.nikolaybespalov.imageioozf;

import com.google.common.io.Resources;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Testing reading capabilities.
 */
public class OzfImageReadTest {

    /**
     * This test checks...
     */
    @Test
    public void readWorld() throws IOException {
        try (ImageInputStream is = new FileImageInputStream(FileUtils.toFile(Resources.getResource("World.ozf2")))) {
            ImageReader reader = new OzfImageReader(null);

            reader.setInput(is);

            // checks basic reader capabilities
            assertEquals(2108, reader.getWidth(0));
            assertEquals(2048, reader.getHeight(0));
            assertEquals(5, reader.getNumImages(false));

//            ImageReadParam param0 = new ImageReadParam();
//            param0.setSourceRegion(new Rectangle(1024 + 128, 1024, 128, 64));
//            BufferedImage image0 = reader.read(0, param0);
//            assertEquals(2108, image0.getWidth());
//            assertEquals(2048, image0.getHeight());
        }
    }
}