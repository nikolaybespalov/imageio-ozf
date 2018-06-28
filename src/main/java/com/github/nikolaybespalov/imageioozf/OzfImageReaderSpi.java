package com.github.nikolaybespalov.imageioozf;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

public final class OzfImageReaderSpi extends ImageReaderSpi {
    private static final String[] suffixes = {"ozf2", "ozf3"};

    private static final String[] formatNames = {"OziExplorer Image File"};

    private static final String[] MIMETypes = {"image/ozf2", "image/ozf3"};

    private static final String version = "1.0";

    private static final String readerCN = "com.github.nikolaybespalov.imageioozf.OzfImageReader";

    private static final String vendorName = "Nikolay Bespalov";

    public OzfImageReaderSpi() {
        super(vendorName, version, formatNames, suffixes, MIMETypes, readerCN, new Class[]{File.class, ImageInputStream.class}, null, false, null, null, null, null, true, null, null, null, null);
    }

    @Override
    public boolean canDecodeInput(Object input) throws IOException {
        if (!(input instanceof ImageInputStream)) {
            return false;
        }

        ImageInputStream stream = (ImageInputStream) input;

        byte[] b = new byte[14];

        stream.mark();
        stream.readFully(b);
        stream.reset();

        // ozfx3
        if (b[0] == (byte) 0x80 && b[1] == (byte) 0x77) {
            return true;
        }

        return b[0] == (byte) 0x78 && b[1] == (byte) 0x77 &&
                b[6] == (byte) 0x40 && b[7] == (byte) 0x00 &&
                b[8] == (byte) 0x01 && b[9] == (byte) 0x00 &&
                b[10] == (byte) 0x36 && b[11] == (byte) 0x04 &&
                b[12] == (byte) 0x00 && b[13] == (byte) 0x00;
    }

    @Override
    public ImageReader createReaderInstance(Object extension) {
        return new OzfImageReader(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "ASDASDASD";
    }
}
