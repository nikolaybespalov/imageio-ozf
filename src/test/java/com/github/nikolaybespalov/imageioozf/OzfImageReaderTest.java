package com.github.nikolaybespalov.imageioozf;

import com.github.davidcarboni.ResourceUtils;
import org.junit.Test;

import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class OzfImageReaderTest {
    private boolean imageStarted = false;
    private boolean imageProgress = false;
    private boolean imageComplete = false;

    @Test
    public void testListeners() throws IOException {
        try (ImageInputStream is = new FileImageInputStream(ResourceUtils.getFile("/com/github/nikolaybespalov/imageioozf/test-data/World.ozf2"))) {
            ImageReader reader = new OzfImageReader(null);

            reader.setInput(is);

            reader.addIIOReadProgressListener(new IIOReadProgressListener() {
                @Override
                public void sequenceStarted(ImageReader imageReader, int i) {
                }

                @Override
                public void sequenceComplete(ImageReader imageReader) {
                }

                @Override
                public void imageStarted(ImageReader imageReader, int i) {
                    imageStarted = true;
                }

                @Override
                public void imageProgress(ImageReader imageReader, float v) {
                    imageProgress = true;
                }

                @Override
                public void imageComplete(ImageReader imageReader) {
                    imageComplete = true;
                }

                @Override
                public void thumbnailStarted(ImageReader imageReader, int i, int i1) {
                }

                @Override
                public void thumbnailProgress(ImageReader imageReader, float v) {
                }

                @Override
                public void thumbnailComplete(ImageReader imageReader) {
                }

                @Override
                public void readAborted(ImageReader imageReader) {
                }
            });

            reader.read(0);

            assertTrue(imageStarted);
            assertTrue(imageProgress);
            assertTrue(imageComplete);
        }
    }
}