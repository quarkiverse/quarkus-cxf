package io.quarkiverse.cxf.it.ws.mtom.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import jakarta.activation.DataSource;

public class RandomBytesDataSource implements DataSource {
    private final int sizeInBytes;

    public static int count(InputStream inputStream) {
        byte[] buffer = new byte[1024];
        int result = 0;
        int bytesRead;

        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                result += bytesRead;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public RandomBytesDataSource(int sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new RandomBytesInputStream(sizeInBytes);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException("Writing to this DataSource is not supported");
    }

    @Override
    public String getContentType() {
        return "application/octet-stream";
    }

    @Override
    public String getName() {
        return "RandomBytesDataSource";
    }

    private static class RandomBytesInputStream extends InputStream {
        private final int size;
        private int bytesRead;
        private final Random random;

        public RandomBytesInputStream(int size) {
            this.size = size;
            this.bytesRead = 0;
            this.random = new Random();
        }

        @Override
        public int read() throws IOException {
            if (bytesRead >= size) {
                return -1;
            }
            bytesRead++;
            return random.nextInt(256);
        }
    }
}