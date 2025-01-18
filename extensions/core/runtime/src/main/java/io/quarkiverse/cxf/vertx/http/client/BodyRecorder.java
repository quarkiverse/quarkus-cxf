package io.quarkiverse.cxf.vertx.http.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkiverse.cxf.CxfConfig.RetransmitCacheConfig;
import io.quarkiverse.cxf.vertx.http.client.TempStore.InitializedTempStore;
import io.quarkiverse.cxf.vertx.http.client.TempStore.InitializedTempStore.TempPath;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.impl.ContextInternal;

/**
 * A non-blocking facility for storing request bodies in memory or on disk for the sake of retransmission.
 */
class BodyRecorder {
    private static final Logger log = Logger.getLogger(VertxHttpClientHTTPConduit.class);

    /**
     * Schedules an opening of a non-blocking {@link BodyWriter} on a Vert.x event loop thread.
     *
     * @param ctx
     * @param retransmitCacheConfig
     * @return a new {@link BodyWriter} {@link Future}
     */
    public static Future<BodyWriter> openWriter(ContextInternal ctx, RetransmitCacheConfig retransmitCacheConfig) {
        final Promise<BodyWriter> result = ctx.promise();
        ctx.runOnContext(v -> {
            final long maxSize = retransmitCacheConfig.maxSize().isPresent()
                    ? retransmitCacheConfig.maxSize().get().asLongValue()
                    : -1;
            result.complete(
                    new MemoryBodyWriter(
                            ctx,
                            retransmitCacheConfig.threshold().asLongValue(),
                            maxSize,
                            retransmitCacheConfig.directory(),
                            retransmitCacheConfig.gcDelay().toMillis(),
                            retransmitCacheConfig.gcOnShutDown()));
        });
        return result.future();
    }

    /**
     * A non-blocking writer. There are implementations for storing in memory or on disk.
     */
    interface BodyWriter {
        public Future<BodyWriter> write(Buffer buffer);

        public Future<StoredBody> close();
    }

    /**
     * A request body readable multiple times stored in memory or on disk.
     */
    interface StoredBody {
        /**
         * @return the length of the stored body in bytes
         */
        public long length();

        /**
         * Pipe this {@link StoredBody} to the given {@code request}.
         *
         * @param request
         * @return a {@link Future} holding the operation outcome
         */
        public Future<Void> pipeTo(HttpClientRequest request);

        /**
         * @return release any system resources held by this {@link StoredBody}, such as in memory buffers or temporary files.
         */
        public Future<Void> discard();
    }

    /**
     * A {@link BodyWriter} storing in memory.
     */
    static class MemoryBodyWriter implements BodyWriter {

        private final ContextInternal ctx;
        private final long threshold;
        private final long maxSize;
        private final Optional<String> tempDir;
        private final long fileDelayMs;
        private final boolean gcOnShutDown;
        private final String threadName;

        /* Read and written only on one specific event loop thread */
        private List<Buffer> buffers;
        private long length = 0;

        public MemoryBodyWriter(ContextInternal ctx, long threshold, long maxSize, Optional<String> tempDir, long fileDelayMs,
                boolean gcOnShutDown) {
            this.ctx = ctx;
            this.threshold = threshold;
            this.maxSize = maxSize;
            this.tempDir = tempDir;
            this.fileDelayMs = fileDelayMs;
            this.gcOnShutDown = gcOnShutDown;
            boolean asserting = false;
            assert asserting = true;
            this.threadName = asserting ? Thread.currentThread().getName() : null;
        }

        @Override
        public Future<BodyWriter> write(Buffer buffer) {
            assert Thread.currentThread().getName().equals(threadName)
                    : "Expected " + threadName + "; found " + Thread.currentThread().getName();

            List<Buffer> buffs = buffers;
            length += buffer.length();
            if (length > threshold) {
                if (maxSize >= 0 && length > maxSize) {
                    return Future.failedFuture(new IOException(
                            "Request body size " + length + " bytes exceeded the max-size limit " + maxSize + " bytes"));
                }
                buffers = null; // avoid damaging the list by subsequent writes
                Future<BodyWriter> diskWriter = DiskBodyWriter.open(ctx, threshold, maxSize, tempDir, fileDelayMs, gcOnShutDown,
                        buffs,
                        threadName);
                return diskWriter.compose(bw -> bw.write(buffer));
            }
            /* Not big enough for the file system -> keep it in memory */
            if (buffs == null) {
                buffs = buffers = new ArrayList<>();
            }
            buffs.add(buffer);
            return Future.succeededFuture(this);
        }

        @Override
        public Future<StoredBody> close() {
            assert Thread.currentThread().getName().equals(threadName)
                    : "Expected " + threadName + "; found " + Thread.currentThread().getName();

            final List<Buffer> buffs = buffers;
            buffers = null; // avoid damaging the list by subsequent writes
            return Future.succeededFuture(new MemoryStoredBody(buffs, length, threadName));
        }
    }

    /**
     * A {@link StoredBody} in memory.
     */
    static class MemoryStoredBody implements StoredBody {

        private List<Buffer> buffers;
        private final long length;
        private final String threadName;

        MemoryStoredBody(List<Buffer> buffers, long length, String threadName) {
            this.buffers = buffers;
            this.length = length;
            this.threadName = threadName;
        }

        @Override
        public long length() {
            assert Thread.currentThread().getName().equals(threadName)
                    : "Expected " + threadName + "; found " + Thread.currentThread().getName();

            return length;
        }

        @Override
        public Future<Void> pipeTo(HttpClientRequest req) {
            assert Thread.currentThread().getName().equals(threadName)
                    : "Expected " + threadName + "; found " + Thread.currentThread().getName();

            final List<Buffer> buffs = buffers;
            if (buffs != null) {
                final int last = buffs.size() - 1;
                if (last == 0) {
                    /* Single buffer recorded */
                    return req.end(buffs.get(0).slice());
                } else if (last == -1) {
                    /* Empty body */
                    return req.end();
                } else {
                    /* Multiple buffers recorded */
                    //req.setChunked(true);
                    // TODO: consider letting MemoryStoredBody implement ReadStream<Buffer> so that we can pipe to  HttpClientRequest with back pressure
                    Future<Void> result = Future.succeededFuture();
                    for (int i = 0; i < last; i++) {
                        final int fi = i;
                        result = result.compose(v -> req.write(buffs.get(fi).slice()));
                    }
                    return result.compose(v -> req.end(buffs.get(last).slice()));
                }
            }
            /* Empty body */
            return req.end();
        }

        @Override
        public Future<Void> discard() {
            assert Thread.currentThread().getName().equals(threadName)
                    : "Expected " + threadName + "; found " + Thread.currentThread().getName();

            buffers = null;
            return Future.succeededFuture();
        }

    }

    static class DiskBodyWriter implements BodyWriter {

        private final ContextInternal ctx;
        private final long maxSize;
        private final AsyncFile tempFile;
        private final TempPath tempPath;
        private final String threadName;

        /* Read and written only on one specific event loop thread */
        private long length = 0;

        private DiskBodyWriter(ContextInternal ctx, long maxSize, TempPath tempPath, AsyncFile tempFile, String threadName) {
            this.ctx = ctx;
            this.maxSize = maxSize;
            this.tempPath = tempPath;
            this.tempFile = tempFile;
            this.threadName = threadName;
        }

        public static Future<BodyWriter> open(ContextInternal ctx, long threshold, long maxSize, Optional<String> tempDir,
                long fileDelayMs, boolean gcOnShutDown, List<Buffer> buffs, String threadName) {
            final Future<InitializedTempStore> tempStore = TempStore.fromContext(ctx, tempDir, fileDelayMs, gcOnShutDown);
            Future<BodyWriter> result = tempStore
                    .compose(ts -> ts.newTempPath())
                    .compose(tempPath -> {
                        log.debugf("Offloading request body exceeding %s bytes to disk: %s", threshold, tempPath.getPath());

                        final Future<AsyncFile> fileFuture = ctx.owner().fileSystem()
                                .open(
                                        tempPath.getPath().toString(),
                                        new OpenOptions().setWrite(true).setCreate(true));
                        return fileFuture
                                .compose(file -> Future
                                        .succeededFuture(new DiskBodyWriter(ctx, maxSize, tempPath, file, threadName)));
                    });
            if (buffs != null) {
                for (Buffer b : buffs) {
                    result = result.compose(bw -> bw.write(b));
                }
            }
            return result;
        }

        @Override
        public Future<BodyWriter> write(Buffer buffer) {
            assert Thread.currentThread().getName().equals(threadName)
                    : "Expected " + threadName + "; found " + Thread.currentThread().getName();

            length += buffer.length();
            if (maxSize >= 0 && length > maxSize) {
                return Future.failedFuture(new IOException(
                        "Request body size " + length + " bytes exceeded the max-size limit " + maxSize + " bytes"));
            }

            return tempFile
                    .write(buffer)
                    .compose(v -> Future.succeededFuture(this));
        }

        @Override
        public Future<StoredBody> close() {
            assert Thread.currentThread().getName().equals(threadName)
                    : "Expected " + threadName + "; found " + Thread.currentThread().getName();

            return tempFile
                    .close()
                    .compose(v -> Future.succeededFuture(new DiskStoredBody(ctx, tempPath, length, threadName)));
        }
    }

    /**
     * A {@link StoredBody} on disk.
     */
    static class DiskStoredBody implements StoredBody {
        private final ContextInternal ctx;
        private final TempPath tempPath;
        private final String threadName;
        private final long length;

        public DiskStoredBody(ContextInternal ctx, TempPath tempPath, long length, String threadName) {
            super();
            this.ctx = ctx;
            this.tempPath = tempPath;
            this.length = length;
            this.threadName = threadName;
        }

        @Override
        public Future<Void> discard() {
            assert Thread.currentThread().getName().equals(threadName)
                    : "Expected " + threadName + "; found " + Thread.currentThread().getName();

            return tempPath.discard();
        }

        @Override
        public Future<Void> pipeTo(HttpClientRequest req) {
            assert Thread.currentThread().getName().equals(threadName)
                    : "Expected " + threadName + "; found " + Thread.currentThread().getName();

            return ctx.owner()
                    .fileSystem()
                    .open(tempPath.getPath().toString(), new OpenOptions().setRead(true))
                    .compose(f -> {
                        return f.pipeTo(req);
                    });
        }

        @Override
        public long length() {
            assert Thread.currentThread().getName().equals(threadName)
                    : "Expected " + threadName + "; found " + Thread.currentThread().getName();

            return length;
        }

    }

}
