package com.github.nikolaybespalov.imageioozf;

import org.junit.Test;

import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OzfImageReaderTest {

    /**
     * This test checks...
     */
    @Test
    public void testWorld() throws IOException {
        try (ImageInputStream is = new FileImageInputStream(new File("C:\\Users\\Nikolay Bespalov\\Documents\\github.com\\nikolaybespalov\\imageio-ozf\\src\\test\\resources\\World.ozf2"))) {
            ImageReader reader = new OzfImageReader(null);

            reader.setInput(is);

            assertEquals(2108, reader.getWidth(0));
            assertEquals(2048, reader.getHeight(0));
            assertEquals(1, reader.getNumImages(false));
        }
        assertTrue(true);
    }
}