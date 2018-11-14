package com.github.nikolaybespalov.imageioozf;

import com.github.davidcarboni.ResourceUtils;
import org.junit.Test;

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
        assertFalse(ImageIO.getImageReadersByFormatName("OziExplorer Image File").next().getOriginatingProvider().canDecodeInput("this is not an ImageInputStream"));


        assertTrue(ImageIO.getImageReadersByMIMEType("image/ozf2").hasNext());
        assertTrue(ImageIO.getImageReadersByMIMEType("image/ozf3").hasNext());

        // Checks ability to read from a File.
        assertNotNull(ImageIO.read(ResourceUtils.getFile("/com/github/nikolaybespalov/imageioozf/test-data/World.ozf2")));
        assertNotNull(ImageIO.read(ResourceUtils.getFile("/com/github/nikolaybespalov/imageioozf/test-data/World.ozf3")));

        // Checks if a .ozf4 cannot be read.
        assertThrows(IOException.class, () -> ImageIO.read(ResourceUtils.getFile("/com/github/nikolaybespalov/imageioozf/test-data/World.ozf4")));

        // Checks not .ozf
        assertNull(ImageIO.read(ResourceUtils.getFile("/com/github/nikolaybespalov/imageioozf/test-data/test.txt")));

        // Checks "short" .ozf3
        assertNull(ImageIO.read(ResourceUtils.getFile("/com/github/nikolaybespalov/imageioozf/test-data/Short.ozf3")));
    }
}