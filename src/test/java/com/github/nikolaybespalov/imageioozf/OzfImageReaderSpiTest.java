package com.github.nikolaybespalov.imageioozf;

import com.google.common.io.Resources;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class OzfImageReaderSpiTest {

    @Test
    public void testGetImageReader() throws IOException {
        assertTrue(ImageIO.getImageReadersBySuffix("ozf2").hasNext());
        assertTrue(ImageIO.getImageReadersBySuffix("ozf3").hasNext());

        assertTrue(ImageIO.getImageReadersByFormatName("OziExplorer Image File").hasNext());
        assertEquals("OziExplorer Image File Reader", ImageIO.getImageReadersByFormatName("OziExplorer Image File").next().getOriginatingProvider().getDescription(Locale.getDefault()));

        assertTrue(ImageIO.getImageReadersByMIMEType("image/ozf2").hasNext());
        assertTrue(ImageIO.getImageReadersByMIMEType("image/ozf3").hasNext());

        // Checks ability to read from a File.
        assertNotNull(ImageIO.read(FileUtils.toFile(Resources.getResource("com/github/nikolaybespalov/imageioozf/test-data/World.ozf2"))));
        assertNotNull(ImageIO.read(FileUtils.toFile(Resources.getResource("com/github/nikolaybespalov/imageioozf/test-data/World.ozf3"))));

        // Checks if a .ozf4 cannot be read.
        Assertions.assertThrows(IOException.class, () -> ImageIO.read(FileUtils.toFile(Resources.getResource("com/github/nikolaybespalov/imageioozf/test-data/World.ozf4"))));

        // Checks if URL cannot be read.
        // Reading from a URL leads to the creation of FileCacheImageInputStream,
        // and this leads to the impossibility to know the size of the stream.
        Assertions.assertThrows(IllegalArgumentException.class, () -> assertNotNull(ImageIO.read(Resources.getResource("com/github/nikolaybespalov/imageioozf/test-data/World.ozf2"))));

        // Checks not .ozf
        assertNull(ImageIO.read(FileUtils.toFile(Resources.getResource("com/github/nikolaybespalov/imageioozf/test-data/test.txt"))));

        // Checks "short" .ozf3
        assertNull(ImageIO.read(FileUtils.toFile(Resources.getResource("com/github/nikolaybespalov/imageioozf/test-data/Short.ozf3"))));
    }
}