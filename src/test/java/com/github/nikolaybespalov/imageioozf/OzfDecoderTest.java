package com.github.nikolaybespalov.imageioozf;

import org.junit.Test;

import static com.github.nikolaybespalov.imageioozf.OzfDecoder.decode;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OzfDecoderTest {
    @Test
    public void testDecrypt() {
        byte[] bytes = {1, 12, 123, 12, 1};

        decode(bytes, 0, bytes.length, (byte) 11);

        assertEquals(57, bytes[0]);
        assertEquals(89, bytes[1]);
        assertEquals(53, bytes[2]);
        assertEquals(-16, bytes[3]);
        assertEquals(51, bytes[4]);
    }
}