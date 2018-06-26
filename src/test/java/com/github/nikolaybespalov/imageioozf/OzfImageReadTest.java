package com.github.nikolaybespalov.imageioozf;

import com.google.common.io.Resources;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
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
        final ImageReadParam param = new ImageReadParam();
        param.setSourceRegion(new Rectangle(0, 0, 2108, 2048));

        try (ImageInputStream is = new FileImageInputStream(FileUtils.toFile(Resources.getResource("World.ozf2")))) {
            ImageReader reader = new OzfImageReader(null);

            reader.setInput(is);

            assertEquals(2108, reader.getWidth(0));
            assertEquals(2048, reader.getHeight(0));
            assertEquals(5, reader.getNumImages(false));

            reader.read(0, param);
        }
    }
}