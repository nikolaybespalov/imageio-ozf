package com.github.nikolaybespalov.imageioozf;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageInputStreamImpl;
import java.io.IOException;
import java.nio.ByteOrder;

public final class OzfEncryptedStream extends ImageInputStreamImpl {
    private static final byte abyKey[] = {
            (byte) 0x2D, (byte) 0x4A, (byte) 0x43, (byte) 0xF1, (byte) 0x27, (byte) 0x9B, (byte) 0x69, (byte) 0x4F,
            (byte) 0x36, (byte) 0x52, (byte) 0x87, (byte) 0xEC, (byte) 0x5F, (byte) 0x42, (byte) 0x53, (byte) 0x22,
            (byte) 0x9E, (byte) 0x8B, (byte) 0x2D, (byte) 0x83, (byte) 0x3D, (byte) 0xD2, (byte) 0x84, (byte) 0xBA,
            (byte) 0xD8, (byte) 0x5B
    };

    private final ImageInputStream stream;
    private final byte key;

    public OzfEncryptedStream(ImageInputStream stream, byte key) {
        this.stream = stream;
        this.key = key;
    }

    @Override
    public int read() throws IOException {
        return 0;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        int n = stream.read(bytes, offset, length);

        if (stream.getByteOrder() == ByteOrder.LITTLE_ENDIAN) {

        }

        decrypt(bytes, offset, length, key);

        return n;
    }

    @Override
    public void seek(long pos) throws IOException {
        stream.seek(pos);
    }

    private static void decrypt(byte[] bytes, int offset, int length, byte key) {
        for (int i = offset; i < length; ++i) {
            bytes[i] = (byte) (((int) bytes[i] ^ (abyKey[i % abyKey.length] + key)) & 0xFF);
        }
    }
}
