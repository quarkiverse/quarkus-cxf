package io.quarkiverse.cxf.transport;

import java.io.IOException;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

import io.quarkus.vertx.utils.VertxJavaIoContext;
import io.quarkus.vertx.utils.VertxOutputStream;

public class VertxServletOutputStream extends ServletOutputStream {
    private final VertxOutputStream delegate;

    public VertxServletOutputStream(VertxJavaIoContext context) {
        super();
        this.delegate = new VertxOutputStream(context);
    }

    @Override
    public void write(int b) throws IOException {
        delegate.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        delegate.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        delegate.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public boolean isReady() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        throw new UnsupportedOperationException();
    }

}
