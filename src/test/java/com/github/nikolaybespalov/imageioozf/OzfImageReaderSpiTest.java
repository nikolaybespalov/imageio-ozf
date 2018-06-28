package com.github.nikolaybespalov.imageioozf;

import org.junit.Test;

import javax.imageio.ImageIO;

import static org.junit.Assert.assertTrue;

public class OzfImageReaderSpiTest {

    @Test
    public void testGetImageReader()/* throws IOException*/ {
        assertTrue(ImageIO.getImageReadersBySuffix("ozf2").hasNext());
        assertTrue(ImageIO.getImageReadersBySuffix("ozf3").hasNext());

        assertTrue(ImageIO.getImageReadersByFormatName("OziExplorer Image File").hasNext());

        assertTrue(ImageIO.getImageReadersByMIMEType("image/ozf2").hasNext());
        assertTrue(ImageIO.getImageReadersByMIMEType("image/ozf3").hasNext());

        //assertNotNull(ImageIO.read(Resources.getResource("com/github/nikolaybespalov/imageioozf/test-data/World.ozf2")));
        //assertNotNull(ImageIO.read(Resources.getResource("com/github/nikolaybespalov/imageioozf/test-data/World.ozf3")));
    }
}