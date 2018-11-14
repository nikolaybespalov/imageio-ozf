package com.github.nikolaybespalov.imageioozf;

import com.github.davidcarboni.ResourceUtils;
import org.junit.Test;

import javax.imageio.stream.FileImageInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class OzfEncryptedStreamTest {

    @Test
    public void testRead() throws IOException {
        OzfEncryptedStream stream = new OzfEncryptedStream(new FileImageInputStream(ResourceUtils.getFile("/com/github/nikolaybespalov/imageioozf/test-data/test.txt")), (byte) 11);

        int b = stream.read();

        assertEquals(108, b);

        while (b != -1) {
            b = stream.read();
        }
    }
}