package io.quarkiverse.cxf.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import org.jboss.logging.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.vertx.core.runtime.VertxBufferImpl;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

public class VertxServletOutputStream extends ServletOutputStream {

    private static final Logger log = Logger.getLogger(VertxServletOutputStream.class);

    private final HttpServerRequest request;
    protected HttpServerResponse response;
    private ByteBuf pooledBuffer;
    private boolean committed;
    protected boolean waitingForDrain;
    protected boolean drainHandlerRegistered;
    private boolean closed;
    protected boolean first = true;
    protected Throwable throwable;
    private ByteArrayOutputStream overflow;

    private Object LOCK = new Object();

    /**
     * Construct a new instance.No write timeout is configured.
     *
     * @param request
     * @param response
     */
    public VertxServletOutputStream(HttpServerRequest request, HttpServerResponse response) {
        this.response = response;
        this.request = request;
        request.response().exceptionHandler(event -> {
            throwable = event;
            log.debugf(event, "IO Exception ");
            request.connection().close();
            synchronized (LOCK) {
                if (waitingForDrain) {
                    LOCK.notifyAll();
                }
            }
        });

        request.response().endHandler(unused -> {
            synchronized (LOCK) {
                if (waitingForDrain) {
                    LOCK.notifyAll();
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final int b) throws IOException {
        write(new byte[] { (byte) b }, 0, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        if (len < 1) {
            return;
        }
        if (closed) {
            throw new IOException("Stream is closed");
        }

        int rem = len;
        int idx = off;
        ByteBuf buffer = pooledBuffer;
        try {
            if (buffer == null) {
                pooledBuffer = buffer = PooledByteBufAllocator.DEFAULT.directBuffer();
            }
            while (rem > 0) {
                int toWrite = Math.min(rem, buffer.writableBytes());
                buffer.writeBytes(b, idx, toWrite);
                rem -= toWrite;
                idx += toWrite;
                if (!buffer.isWritable()) {
                    ByteBuf tmpBuf = buffer;
                    this.pooledBuffer = buffer = PooledByteBufAllocator.DEFAULT.directBuffer();
                    writeBlocking(tmpBuf, false);
                }
            }
        } catch (IOException | RuntimeException e) {
            if (buffer != null && buffer.refCnt() > 0) {
                buffer.release();
            }
            throw new IOException(e);
        }
    }

    public void writeBlocking(ByteBuf buffer, boolean finished) throws IOException {
        prepareWrite(buffer, finished);
        write(buffer, finished);
    }

    private void prepareWrite(ByteBuf buffer, boolean finished) {
        if (!committed) {
            committed = true;
            if (finished) {
                if (buffer == null) {
                    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, "0");
                } else {
                    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, "" + buffer.readableBytes());
                }
            } else if (!request.response().headers().contains(HttpHeaderNames.CONTENT_LENGTH)) {
                request.response().setChunked(true);
            }
        }
    }

    public void write(ByteBuf data, boolean last) throws IOException {
        if (last && data == null) {
            request.response().end();
            return;
        }
        //do all this in the same lock
        synchronized (LOCK) {
            try {
                boolean bufferRequired = awaitWriteable() || (overflow != null && overflow.size() > 0);
                if (bufferRequired) {
                    //just buffer everything
                    registerDrainHandler();
                    if (overflow == null) {
                        overflow = new ByteArrayOutputStream();
                    }
                    if (data != null && data.hasArray()) {
                        overflow.write(data.array(), data.arrayOffset() + data.readerIndex(), data.readableBytes());
                    } else if (data != null) {
                        data.getBytes(data.readerIndex(), overflow, data.readableBytes());
                    }
                    if (last) {
                        closed = true;
                    }
                    if (data != null) {
                        data.release();
                    }
                } else {
                    if (last) {
                        request.response().end(createBuffer(data));
                    } else {
                        request.response().write(createBuffer(data));
                    }
                }
            } catch (IOException | RuntimeException e) {
                if (data != null && data.refCnt() > 0) {
                    data.release();
                }
                throw new IOException("Failed to write", e);
            }
        }
    }

    private boolean awaitWriteable() throws IOException {
        if (Context.isOnEventLoopThread()) {
            return request.response().writeQueueFull();
        }
        if (first) {
            first = false;
            return false;
        }
        assert Thread.holdsLock(LOCK);
        while (request.response().writeQueueFull()) {
            if (throwable != null) {
                throw new IOException(throwable);
            }
            if (request.response().closed()) {
                throw new IOException("Connection has been closed");
            }
            registerDrainHandler();
            try {
                waitingForDrain = true;
                LOCK.wait();
            } catch (InterruptedException e) {
                throw new InterruptedIOException(e.getMessage());
            } finally {
                waitingForDrain = false;
            }
        }
        return false;
    }

    private void registerDrainHandler() {
        if (!drainHandlerRegistered) {
            drainHandlerRegistered = true;
            Handler<Void> handler = event -> {
                synchronized (LOCK) {
                    if (waitingForDrain) {
                        LOCK.notifyAll();
                    }
                    if (overflow != null && overflow.size() > 0) {
                        if (closed) {
                            request.response().end(Buffer.buffer(overflow.toByteArray()));
                        } else {
                            request.response().write(Buffer.buffer(overflow.toByteArray()));
                        }
                        overflow.reset();
                    }
                }
            };
            request.response().drainHandler(handler);
            request.response().closeHandler(handler);
        }
    }

    Buffer createBuffer(ByteBuf data) {
        return new VertxBufferImpl(data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        if (pooledBuffer != null) {
            try {
                writeBlocking(pooledBuffer, false);
                pooledBuffer = null;
            } catch (IOException | RuntimeException e) {
                pooledBuffer.release();
                pooledBuffer = null;
                throw new IOException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        if (closed)
            return;
        try {
            writeBlocking(pooledBuffer, true);
        } catch (IOException | RuntimeException e) {
            throw new IOException(e);
        } finally {
            closed = true;
            pooledBuffer = null;
        }
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
