package io.quarkiverse.cxf.vertx.http.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.test.VertxTestUtil;
import io.quarkiverse.cxf.vertx.http.client.TempStore.InitializedTempStore;
import io.quarkiverse.cxf.vertx.http.client.TempStore.InitializedTempStore.TempPath;
import io.quarkus.logging.Log;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;

public class TempStoreTest {
    private static final int FILE_DELAY = 10;

    private static final int GC_DELAY = 360000;

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.log.category.\"" + TempStore.class.getName() + "\".level", "DEBUG");

    @Inject
    Vertx vertx;

    @Test
    void initializeExistingTempDir() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        Log.info("TempPathTest.initializeExistingTempDir()");

        final Path tempDir = Path.of("target/" + TempStoreTest.class.getSimpleName() + "-" + UUID.randomUUID() + "/temp");
        Files.createDirectories(tempDir);
        Assertions.assertThat(tempDir).exists();

        final CompletableFuture<AsyncResult<InitializedTempStore>> initializedCF = new CompletableFuture<>();
        final CompletableFuture<ContextInternal> ctxCF = new CompletableFuture<>();
        final CompletableFuture<AsyncResult<TempPath>> tempPathCF = new CompletableFuture<>();

        vertx.runOnContext(v -> {
            final ContextInternal ct = (ContextInternal) vertx.getOrCreateContext();
            ctxCF.complete(ct);
            final Future<InitializedTempStore> initialized = TempStore
                    .fromContext(vertx.getOrCreateContext(), Optional.of(tempDir.toString()), GC_DELAY, FILE_DELAY, true)
                    .andThen(initializedCF::complete);

            initialized.compose(i -> i.newTempPath())
                    .andThen(tempPathCF::complete);

        });
        final ContextInternal ctx = ctxCF.get(5, TimeUnit.SECONDS);

        final AsyncResult<InitializedTempStore> initialized = initializedCF.get(5, TimeUnit.SECONDS);
        VertxTestUtil.assertSuccess(initialized);
        Assertions.assertThat(initialized.result().getDirectory()).isEqualTo(tempDir);

        /* Get a tempFile path */
        final AsyncResult<TempPath> tempPath = tempPathCF.get(5, TimeUnit.SECONDS);
        VertxTestUtil.assertSuccess(tempPath);
        final Path p = tempPath.result().getPath();
        Assertions.assertThat(p.toString()).startsWith(tempDir.toString());

        Promise<Void> close = ctx.promise();
        /* Check InitializedTempStore.close() works */
        ctx.runOnContext(v -> initialized.result().close(close));
        close.future().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);

        final Promise<Object> tmpStoreAfterCloseP = ctx.promise();
        ctx.runOnContext(v -> {
            tmpStoreAfterCloseP.complete(ctx.contextData().get(TempStore.CONTEXT_KEY));
        });

        final Object tmpStoreAfterClose = tmpStoreAfterCloseP.future().toCompletionStage().toCompletableFuture().get(5,
                TimeUnit.SECONDS);
        Assertions.assertThat(tmpStoreAfterClose).isNull();

        Log.info("Finished testing TempStore.fromContext(Context, Path, long, long)");
    }

    @Test
    void initializeNewTempDir() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        Log.info("TempPathTest.initializeNewTempDir()");
        final Path tempDir = Path.of("target/" + TempStoreTest.class.getSimpleName() + "-" + UUID.randomUUID() + "/temp");
        Assertions.assertThat(tempDir).doesNotExist();

        final CompletableFuture<AsyncResult<InitializedTempStore>> initializedCF = new CompletableFuture<>();
        final CompletableFuture<ContextInternal> ctxCF = new CompletableFuture<>();
        final CompletableFuture<AsyncResult<TempPath>> tempPathCF = new CompletableFuture<>();

        vertx.runOnContext(v -> {
            final ContextInternal ct = (ContextInternal) vertx.getOrCreateContext();
            ctxCF.complete(ct);
            final Future<InitializedTempStore> initialized = TempStore
                    .fromContext(ct, Optional.of(tempDir.toString()), GC_DELAY, FILE_DELAY, true)
                    .andThen(initializedCF::complete);

            initialized.compose(i -> i.newTempPath())
                    .andThen(tempPathCF::complete);

        });

        final AsyncResult<InitializedTempStore> initialized = initializedCF.get(5, TimeUnit.SECONDS);
        VertxTestUtil.assertSuccess(initialized);
        Assertions.assertThat(initialized.result().getDirectory()).isDirectory();

        final ContextInternal ctx = ctxCF.get(5, TimeUnit.SECONDS);
        //Assertions.assertThat((Object) ctx.get(TempStore.CONTEXT_KEY)).isInstanceOf(TempStore.class);
        final Future<InitializedTempStore> initialized2 = TempStore.fromContext(ctx, Optional.of(tempDir.toString()), GC_DELAY,
                FILE_DELAY, true);
        Assertions.assertThat(initialized.result()).isSameAs(initialized2.result());

        {
            /* Test TempPath.delete() */
            Log.info("Testing TempPath.delete()");

            /* Get a tempFile path */
            final TempPath tempPath = VertxTestUtil.assertSuccess(tempPathCF.get(5, TimeUnit.SECONDS));
            final Path p = tempPath.getPath();
            Assertions.assertThat(p).doesNotExist();

            /* Use the path to create the file */
            Files.write(p, new byte[] { 42 });
            Assertions.assertThat(p).isRegularFile();

            /* Check the direct deletion works */
            tempPath.discard().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
            Assertions.assertThat(p).doesNotExist();
        }

        {
            /* Test InitializedTempStore.gc() */
            Log.info("Testing InitializedTempStore.gc()");

            /* Get a tempFile path */
            final CompletableFuture<AsyncResult<TempPath>> tempPathCF1 = new CompletableFuture<>();
            vertx.runOnContext(v -> {
                initialized.result().newTempPath()
                        .andThen(tempPathCF1::complete);
            });
            final TempPath tp1 = VertxTestUtil.assertSuccess(tempPathCF1.get(5, TimeUnit.SECONDS));
            final Path p1 = tp1.getPath();
            Assertions.assertThat(p1).doesNotExist();

            /* Use the path to create the file */
            Files.write(p1, new byte[] { 42 });
            Assertions.assertThat(p1).isRegularFile();

            /*
             * Get a path but do not create the file - the InitializedTempStore.gc() should not get confused if the leased path
             * does
             * not exist
             */
            final CompletableFuture<AsyncResult<TempPath>> tempPathCF2 = new CompletableFuture<>();
            vertx.runOnContext(v -> {
                initialized.result().newTempPath()
                        .andThen(tempPathCF2::complete);
            });
            final TempPath tp2 = VertxTestUtil.assertSuccess(tempPathCF2.get(5, TimeUnit.SECONDS));

            /* Invoke gc() manually, so that we do not need to mess with waiting for the timer */
            initialized.result().gc();

            Awaitility.await()
                    .atMost(10, TimeUnit.SECONDS)
                    .until(() -> {
                        boolean f1Exists = Files.exists(p1);
                        boolean f2Exists = Files.exists(tp2.getPath());
                        Log.infof("%s exists: %s, %s exists: %s", p1, f1Exists, tp2.getPath(), f2Exists);
                        return !f1Exists && !f2Exists;
                    });

        }

        {
            /* Test InitializedTempStore.close(Promise<Void>) */
            Log.info("Testing InitializedTempStore.close(Promise<Void>)");

            /* Get a tempFile path */
            final CompletableFuture<AsyncResult<TempPath>> tempPathCF1 = new CompletableFuture<>();
            vertx.runOnContext(v -> {
                initialized.result().newTempPath()
                        .andThen(tempPathCF1::complete);
            });
            final TempPath tp1 = VertxTestUtil.assertSuccess(tempPathCF1.get(5, TimeUnit.SECONDS));
            final Path p1 = tp1.getPath();
            Assertions.assertThat(p1).doesNotExist();

            /* Use the path to create the file */
            Files.write(p1, new byte[] { 42 });
            Assertions.assertThat(p1).isRegularFile();

            /*
             * Get a path but do not create the file - the TempStore.shutdown() should not get confused if the leased path does
             * not exist
             */
            final CompletableFuture<AsyncResult<TempPath>> tempPathCF2 = new CompletableFuture<>();
            vertx.runOnContext(v -> {
                initialized.result().newTempPath()
                        .andThen(tempPathCF2::complete);
            });
            final TempPath tp2 = VertxTestUtil.assertSuccess(tempPathCF2.get(5, TimeUnit.SECONDS));

            Promise<Void> close = ctx.promise();
            /* Check TempStore.shutdown() works */
            ctx.runOnContext(v -> initialized.result().close(close));
            close.future().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);

            final Promise<Object> tmpStoreAfterCloseP = ctx.promise();
            ctx.runOnContext(v -> {
                tmpStoreAfterCloseP.complete(ctx.contextData().get(TempStore.CONTEXT_KEY));
            });

            final Object tmpStoreAfterClose = tmpStoreAfterCloseP.future().toCompletionStage().toCompletableFuture().get(5,
                    TimeUnit.SECONDS);
            Assertions.assertThat(tmpStoreAfterClose).isNull();
            Assertions.assertThat(p1).doesNotExist();
            Assertions.assertThat(tp2.getPath()).doesNotExist();

            Log.info("Finished testing InitializedTempStore.close(Promise<Void>)");

        }

    }

    @Test
    void closeWithoutDelete() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        Log.info("TempPathTest.closeWithoutDelete()");
        final Path tempDir = Path.of("target/" + TempStoreTest.class.getSimpleName() + "-" + UUID.randomUUID() + "/temp");
        Assertions.assertThat(tempDir).doesNotExist();

        final CompletableFuture<AsyncResult<InitializedTempStore>> initializedCF = new CompletableFuture<>();
        final CompletableFuture<ContextInternal> ctxCF = new CompletableFuture<>();
        final CompletableFuture<AsyncResult<TempPath>> tempPathCF1 = new CompletableFuture<>();

        vertx.runOnContext(v -> {
            final ContextInternal ct = (ContextInternal) vertx.getOrCreateContext();
            ctxCF.complete(ct);
            final Future<InitializedTempStore> initialized = TempStore
                    .fromContext(ct, Optional.of(tempDir.toString()), GC_DELAY, FILE_DELAY, false)
                    .andThen(initializedCF::complete);

            initialized.compose(i -> i.newTempPath())
                    .andThen(tempPathCF1::complete);

        });

        final AsyncResult<InitializedTempStore> initialized = initializedCF.get(5, TimeUnit.SECONDS);
        VertxTestUtil.assertSuccess(initialized);
        Assertions.assertThat(initialized.result().getDirectory()).isDirectory();
        final ContextInternal ctx = ctxCF.get(5, TimeUnit.SECONDS);

        {

            final TempPath tp1 = VertxTestUtil.assertSuccess(tempPathCF1.get(5, TimeUnit.SECONDS));
            final Path p1 = tp1.getPath();
            Assertions.assertThat(p1).doesNotExist();

            /* Use the path to create the file */
            Files.write(p1, new byte[] { 42 });
            Assertions.assertThat(p1).isRegularFile();

            Promise<Void> close = ctx.promise();
            /* Check TempStore.shutdown() works */
            ctx.runOnContext(v -> initialized.result().close(close));
            close.future().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);

            Assertions.assertThat(p1).isRegularFile();

            Log.info("Finished testing InitializedTempStore.close(Promise<Void>)");

        }

    }
}
