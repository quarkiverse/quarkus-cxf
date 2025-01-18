package io.quarkiverse.cxf.vertx.http.client;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.logging.Logger;

import io.vertx.core.Closeable;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.file.FileSystem;
import io.vertx.core.impl.ContextInternal;

/**
 * A disk store for temporary files.
 */
public class TempStore {
    private static final Logger log = Logger.getLogger(TempStore.class);
    private static final long MIN_DELAY = 2000; /* 2 seconds */
    public static final String CONTEXT_KEY = TempStore.class.getName();

    /**
     *
     * @param ctx the {@link Context} to bind the returned {@link InitializedTempStore} to
     * @param tempDir the temporary directory where to create temporary files
     * @param fileDelayMs number of milliseconds since the creation of the given file after which the file can be deleted
     * @param gcOnShutDown if {@code true} the temporary files will be removed on {@link InitializedTempStore#close(Promise)}
     * @return a {@link Future} holding an {@link InitializedTempStore} - either a new one or one retrieved from the given
     *         {@link Context}
     */
    public static Future<InitializedTempStore> fromContext(Context ctx, Optional<String> tempDir, long fileDelayMs,
            boolean gcOnShutDown) {
        validateDelayMs(fileDelayMs, "quarkus.cxf.retransmit-cache.gc-delay");
        return fromContext(ctx, tempDir, fileDelayMs >> 1, fileDelayMs, gcOnShutDown);
    }

    /**
     * @param ctx the {@link Context} to bind the returned {@link InitializedTempStore} to
     * @param tempDir the temporary directory where to create temporary files
     * @param gcDelayMs delay in milliseconds for periodic cleaning of the temporary files
     * @param fileDelayMs number of milliseconds since the creation of the given file after which the file can be deleted
     * @param gcOnShutDown if {@code true} the temporary files will be removed on {@link InitializedTempStore#close(Promise)}
     * @return a {@link Future} holding an {@link InitializedTempStore} - either a new one or one retrieved from the given
     *         {@link Context}
     */
    public static Future<InitializedTempStore> fromContext(Context ctx, Optional<String> tempDir, long gcDelayMs,
            long fileDelayMs, boolean gcOnShutDown) {
        ContextInternal ctxi = (ContextInternal) ctx;
        return ((TempStore) ctxi
                .contextData()
                .computeIfAbsent(CONTEXT_KEY,
                        k -> new TempStore(
                                ctxi,
                                Path.of(tempDir.orElse(System.getProperty("java.io.tmpdir"))),
                                gcDelayMs,
                                fileDelayMs, gcOnShutDown)))
                .initializeIfNeeded();
    }

    private final ContextInternal ctx;
    private final Path directory;
    private final long gcDelayMs;
    private final long fileDelayMs;
    private final boolean gcOnShutDown;

    /*
     * No need for volatile as TempStore instances are per Vet.x Context and this this field is both read and written from the
     * same thread
     */
    private Future<InitializedTempStore> initializedTempStore;

    TempStore(ContextInternal ctx, Path directory, long gcDelayMs, long fileDelayMs, boolean gcOnShutDown) {
        super();
        this.ctx = ctx;
        this.directory = directory;
        this.gcDelayMs = gcDelayMs;
        this.fileDelayMs = fileDelayMs;
        this.gcOnShutDown = gcOnShutDown;
    }

    public static long validateDelayMs(long delayMs, String label) {
        if (delayMs < MIN_DELAY && delayMs != 0) {
            throw new IllegalArgumentException("The value of " + label
                    + " must be >= " + MIN_DELAY + " or 0 to disable regular deletion of stale temporary files");
        }
        return delayMs;
    }

    Future<InitializedTempStore> initializeIfNeeded() {

        if (initializedTempStore != null) {
            /* A shortcut to avoid checking the tempDir existence if we checked already */
            return initializedTempStore;
        }
        final FileSystem fs = ctx.owner().fileSystem();
        return fs
                .exists(directory.toString())
                .compose(exists -> {
                    if (initializedTempStore != null) {
                        return initializedTempStore;
                    }
                    if (!exists) {
                        return initializedTempStore = fs
                                .mkdirs(directory.toString())
                                .compose(dir -> Future
                                        .succeededFuture(new InitializedTempStore(ctx, directory, gcDelayMs, fileDelayMs,
                                                gcOnShutDown)));
                    } else {
                        return initializedTempStore = Future
                                .succeededFuture(
                                        new InitializedTempStore(ctx, directory, gcDelayMs, fileDelayMs, gcOnShutDown));
                    }
                });
    }

    public static class InitializedTempStore implements Closeable {
        private final ContextInternal ctx;
        private final Path directory;
        private final long gcDelayMs;
        private final long fileDelayMs;
        private final boolean gcOnShutDown;
        private final String prefix;
        private final String threadName;

        /* Read/written from a single specific thread */
        private int counter = 0;

        /*
         * Typically read/written from a single specific thread
         * but the cleaning ops on close, can be done from another thread, such as main.
         * We are deliberately not making it volatile for that case,
         * because we hold race conditions after close for highly unlikely
         */
        private long timerId = -1;
        private List<TempPath> tempFiles = new CopyOnWriteArrayList<>();

        InitializedTempStore(ContextInternal ctx, Path directory, long gcDelayMs, long fileDelayMs, boolean gcOnShutDown) {
            super();
            ctx.addCloseHook(this);
            this.ctx = ctx;
            this.directory = directory;
            this.gcDelayMs = gcDelayMs;
            this.fileDelayMs = fileDelayMs;
            this.gcOnShutDown = gcOnShutDown;
            final String threadName = Thread.currentThread().getName();
            final String shortThreadName = threadName.replace("vert.x-eventloop-thread-", "evtloop-");
            this.prefix = "qcxf-TempStore-" + ProcessHandle.current().pid() + "-" + shortThreadName + "-";

            boolean asserting = false;
            assert asserting = true;
            this.threadName = asserting ? threadName : null;

            log.debugf("Initialized a new TempStore %s/%s*", directory, prefix);

        }

        public Path getDirectory() {
            return directory;
        }

        public Future<TempPath> newTempPath() {
            assert Thread.currentThread().getName().equals(threadName)
                    : "Expected " + threadName + "; found " + Thread.currentThread().getName();

            if (tempFiles != null) {
                final TempPath newPath = new TempPath(
                        directory.resolve(prefix + (counter++)),
                        System.currentTimeMillis() + fileDelayMs);
                tempFiles.add(newPath);
                if (fileDelayMs >= MIN_DELAY && timerId < 0) {
                    timerId = ctx.owner().setPeriodic(gcDelayMs, tid -> gc());
                }
                log.debugf("Created new temporary path %s", newPath);
                return Future.succeededFuture(newPath);
            }
            return Future.failedFuture("Cannot get new TempPath: TempStore closed already");
        }

        public void gc() {
            if (tempFiles != null) {
                ctx.runOnContext(v -> {
                    assert Thread.currentThread().getName().equals(threadName)
                            : "Expected " + threadName + "; found " + Thread.currentThread().getName();

                    if (tempFiles != null) {
                        delete(new ArrayList<>(tempFiles), System.currentTimeMillis())
                                .onSuccess(dels -> log.debugf("Gc'd %d temporary files in TempStore %s/%s*", dels.size(),
                                        directory, prefix))
                                .onFailure(e -> log.errorf(e, "Could not gc some temporary files in TempStore %s/%s*",
                                        directory, prefix));
                    }
                });
            }
        }

        @Override
        public void close(Promise<Void> completion) {
            if (timerId >= 0) {
                ctx.owner().cancelTimer(timerId);
                timerId = -1;
            }
            ctx.contextData().remove(CONTEXT_KEY);
            if (tempFiles != null) {
                final List<TempPath> tempFiles = this.tempFiles;
                this.tempFiles = null; // disallow adding new files
                if (!gcOnShutDown) {
                    log.debugf("Skipping deletion of %d temporary files in TempStore %s/%s* on close", tempFiles.size(),
                            directory,
                            prefix);
                    completion.complete();
                    return;
                }
                delete(tempFiles, 0 /* 0 to delete all files immediately */)
                        .onSuccess(dels -> {
                            log.debugf("Deleted %d files on close in TempStore %s/%s*", dels.size(), directory,
                                    prefix);
                            completion.complete();
                        })
                        .onFailure(e -> {
                            log.errorf(e, "Could not delete some temporary files on close in TempStore %s/%s*", directory,
                                    prefix);
                            completion.fail(e);
                        });
            } else {
                /* If tempFiles == null there is nothing to cleanup */
                log.debugf("Nothing to cleanup on close in TempStore %s/%s*", directory, prefix);
                completion.complete();
            }
        }

        CompositeFuture delete(final List<TempPath> tempFiles, long currentTimeMillis) {
            final List<Future<Void>> deletions = new ArrayList<>(tempFiles.size());
            for (TempPath path : tempFiles) {
                if (path.gcTime >= currentTimeMillis) {
                    deletions.add(ctx.owner().fileSystem()
                            .exists(path.path.toString())
                            .compose(
                                    exists -> {
                                        if (exists) {
                                            return ctx.owner().fileSystem()
                                                    .delete(path.path.toString())
                                                    .andThen(ar -> {
                                                        if (ar.succeeded()) {
                                                            log.debugf("Deleted temporary file %s", path.path);
                                                        } else {
                                                            log.warnf(ar.cause(), "Could not delete temporary file %s",
                                                                    path.path);
                                                        }
                                                    });
                                        } else {
                                            log.debugf("Temporary file %s did not exist when attempting to delete it",
                                                    path.path);
                                            return Future.succeededFuture();
                                        }
                                    }));
                }
            }
            return Future.all(deletions);
        }

        public class TempPath {
            private final Path path;
            /** Unix era time at or after which this {@link TempPath} can be deleted */
            private final long gcTime;

            public TempPath(Path path, long gcTime) {
                super();
                this.path = path;
                this.gcTime = gcTime;
            }

            public Future<Void> discard() {
                final String p = path.toString();
                final FileSystem fs = ctx.owner().fileSystem();
                return fs
                        .exists(p)
                        .compose(exists -> {
                            if (exists) {
                                return fs
                                        .delete(p)
                                        .andThen(ar -> {
                                            if (ar.succeeded()) {
                                                log.debugf("Deleted temporary file %s", p);
                                            } else {
                                                log.warnf(ar.cause(), "Could not delete temporary file %s", p);
                                            }
                                        });
                            } else {
                                log.debugf("Temporary file %s did not exist when attempting to delete it", p);
                                return Future.succeededFuture();
                            }
                        })
                        .andThen(f -> {
                            final List<TempPath> tfs = tempFiles;
                            /* tempFiles can be null when TempStore.close() was called */
                            if (tfs != null) {
                                tfs.remove(this);
                            }
                        });
            }

            public Path getPath() {
                return path;
            }

            @Override
            public String toString() {
                return path + "@" + gcTime;
            }
        }

    }

}
