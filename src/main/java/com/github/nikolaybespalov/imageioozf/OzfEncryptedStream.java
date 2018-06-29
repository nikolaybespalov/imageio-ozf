package com.github.nikolaybespalov.imageioozf;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageInputStreamImpl;
import java.io.IOException;

import static com.github.nikolaybespalov.imageioozf.OzfDecrypter.decrypt;

class OzfEncryptedStream extends ImageInputStreamImpl {
    private final ImageInputStream stream;
    private final byte key;

    OzfEncryptedStream(ImageInputStream stream, byte key) {
        this.stream = stream;
        this.key = key;
    }

    @Override
    public int read() throws IOException {
        byte[] oneByte = new byte[1];

        if (read(oneByte, 0, 1) == -1) {
            return -1;
        }

        return oneByte[0];
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        int n = stream.read(bytes, offset, length);

        decrypt(bytes, offset, length, key);

        return n;
    }

    @Override
    public void seek(long pos) throws IOException {
        stream.seek(pos);
    }


}
