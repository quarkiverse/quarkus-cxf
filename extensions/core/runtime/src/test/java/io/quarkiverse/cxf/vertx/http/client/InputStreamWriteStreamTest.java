package io.quarkiverse.cxf.vertx.http.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit.InputStreamWriteStream;
import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit.TimeoutSpec;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.ContextInternal;

public class InputStreamWriteStreamTest {

    @Test
    void readBeforeWriteTimeout() throws IOException, InterruptedException {
        ContextInternal ctx = (ContextInternal) Vertx.vertx().getOrCreateContext();
        try (InputStreamWriteStream ws = new InputStreamWriteStream(ctx, timeoutSpec(), 2)) {
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch readFinished = new CountDownLatch(1);
            Thread t = new Thread(() -> {
                started.countDown();
                try {
                    ws.read(new byte[8]); // should block
                    readFinished.countDown(); // should never be reached
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, "test-worker");
            t.start();
            Assertions.assertThat(started.await(500, TimeUnit.MILLISECONDS)).isTrue();
            /*
             * ws.read(new byte[8]) should block so readFinished.countDown() will never be reached, so the
             * awaiting readFinished latch should timeout
             */
            Assertions.assertThat(readFinished.await(500, TimeUnit.MILLISECONDS)).isFalse();
        }
    }

    static TimeoutSpec timeoutSpec() {
        try {
            return TimeoutSpec.create(2000, new URI("http://acme.com"));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void readBeforeWrite() throws IOException, InterruptedException {

        ContextInternal ctx = (ContextInternal) Vertx.vertx().getOrCreateContext();
        try (InputStreamWriteStream ws = new InputStreamWriteStream(ctx, timeoutSpec(), 2)) {
            final CountDownLatch started = new CountDownLatch(1);

            final CountDownLatch read1Finished = new CountDownLatch(1);
            final AtomicInteger read1 = new AtomicInteger();
            final byte[] arr1 = new byte[8];

            final CountDownLatch write2Finished = new CountDownLatch(1);
            final CountDownLatch read2Finished = new CountDownLatch(1);
            final AtomicInteger read2 = new AtomicInteger();
            final byte[] arr2 = new byte[14];

            final CountDownLatch read3Finished = new CountDownLatch(1);
            final AtomicInteger read3 = new AtomicInteger();
            final byte[] arr3 = new byte[1];

            Thread t = new Thread(() -> {
                started.countDown();
                try {
                    read1.set(ws.read(arr1));
                    read1Finished.countDown();

                    write2Finished.await(500, TimeUnit.MILLISECONDS);
                    read2.set(ws.read(arr2));
                    read2Finished.countDown();

                    read3.set(ws.read(arr3));
                    read3Finished.countDown();

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, "test-worker");
            t.start();

            Assertions.assertThat(started.await(500, TimeUnit.MILLISECONDS)).isTrue();

            ws.write(Buffer.buffer("abcd"));
            Assertions.assertThat(read1Finished.await(500, TimeUnit.MILLISECONDS)).isTrue();
            Assertions.assertThat(read1.get()).isEqualTo(4);
            Assertions.assertThat(arr1).isEqualTo("abcd\0\0\0\0".getBytes(StandardCharsets.UTF_8));

            ws.write(Buffer.buffer("efgh"));
            ws.write(Buffer.buffer("ijkl"));
            ws.write(Buffer.buffer("mnop"));
            write2Finished.countDown();

            Assertions.assertThat(read2Finished.await(500, TimeUnit.MILLISECONDS)).isTrue();
            Assertions.assertThat(read2.get()).isEqualTo(12);
            Assertions.assertThat(arr2).isEqualTo("efghijklmnop\0\0".getBytes(StandardCharsets.UTF_8));

            ws.end();
            Assertions.assertThat(read3Finished.await(500, TimeUnit.MILLISECONDS)).isTrue();
            Assertions.assertThat(read3.get()).isEqualTo(-1);
            Assertions.assertThat(arr3).isEqualTo("\0".getBytes(StandardCharsets.UTF_8));

        }
    }

    @Test
    void available0AfterEnd() throws IOException {

        ContextInternal ctx = (ContextInternal) Vertx.vertx().getOrCreateContext();
        try (InputStreamWriteStream ws = new InputStreamWriteStream(ctx, timeoutSpec(), 2)) {
            ws.end();
            Assertions.assertThat(ws.available()).isEqualTo(0);
            Assertions.assertThat(ws.read(new byte[8])).isEqualTo(-1);

        }
    }

    @Test
    void available0BeforeEnd() throws IOException {

        ContextInternal ctx = (ContextInternal) Vertx.vertx().getOrCreateContext();
        try (InputStreamWriteStream ws = new InputStreamWriteStream(ctx, timeoutSpec(), 2)) {
            Assertions.assertThat(ws.available()).isEqualTo(0);
            ws.end();
            Assertions.assertThat(ws.read(new byte[8])).isEqualTo(-1);
        }
    }

    @Test
    void readEmpty() throws IOException {

        ContextInternal ctx = (ContextInternal) Vertx.vertx().getOrCreateContext();
        try (InputStreamWriteStream ws = new InputStreamWriteStream(ctx, timeoutSpec(), 2)) {
            ws.end();
            Assertions.assertThat(ws.read(new byte[8])).isEqualTo(-1);
        }
    }

    @Test
    void singleBuffer() throws IOException {

        ContextInternal ctx = (ContextInternal) Vertx.vertx().getOrCreateContext();
        try (InputStreamWriteStream ws = new InputStreamWriteStream(ctx, timeoutSpec(), 2)) {
            final String INPUT = "abcd";
            final Buffer b = Buffer.buffer(INPUT);
            ws.write(b);
            ws.end();
            byte[] arr = new byte[8];
            Assertions.assertThat(ws.read(arr)).isEqualTo(4);
            Assertions.assertThat(arr).isEqualTo((INPUT + "\0\0\0\0").getBytes(StandardCharsets.UTF_8));
            Assertions.assertThat(ws.read(arr)).isEqualTo(-1);
            Assertions.assertThat(arr).isEqualTo((INPUT + "\0\0\0\0").getBytes(StandardCharsets.UTF_8));
        }

        try (InputStreamWriteStream ws = new InputStreamWriteStream(ctx, timeoutSpec(), 2)) {
            final String INPUT = "abcd";
            final Buffer b = Buffer.buffer(INPUT);
            ws.write(b);
            ws.end();
            byte[] arr = new byte[4];
            Assertions.assertThat(ws.read(arr)).isEqualTo(4);
            Assertions.assertThat(arr).isEqualTo(INPUT.getBytes(StandardCharsets.UTF_8));
            Assertions.assertThat(ws.read(arr)).isEqualTo(-1);
            Assertions.assertThat(arr).isEqualTo(INPUT.getBytes(StandardCharsets.UTF_8));
        }

        try (InputStreamWriteStream ws = new InputStreamWriteStream(ctx, timeoutSpec(), 2)) {
            final String INPUT = "abcd";
            final Buffer b = Buffer.buffer(INPUT);
            ws.write(b);
            ws.end();
            byte[] arr = new byte[2];
            Assertions.assertThat(ws.read(arr)).isEqualTo(2);
            Assertions.assertThat(arr).isEqualTo("ab".getBytes(StandardCharsets.UTF_8));
            Assertions.assertThat(ws.read(arr)).isEqualTo(2);
            Assertions.assertThat(arr).isEqualTo("cd".getBytes(StandardCharsets.UTF_8));
            Assertions.assertThat(ws.read(arr)).isEqualTo(-1);
            Assertions.assertThat(arr).isEqualTo("cd".getBytes(StandardCharsets.UTF_8));
        }

    }

    @Test
    void twoBuffers() throws IOException {

        ContextInternal ctx = (ContextInternal) Vertx.vertx().getOrCreateContext();
        try (InputStreamWriteStream ws = new InputStreamWriteStream(ctx, timeoutSpec(), 2)) {
            final String INPUT1 = "abcd";
            final String INPUT2 = "efgh";
            ws.write(Buffer.buffer(INPUT1));
            ws.write(Buffer.buffer(INPUT2));
            ws.end();
            byte[] arr = new byte[8];
            Assertions.assertThat(ws.read(arr)).isEqualTo(8);
            Assertions.assertThat(arr).isEqualTo((INPUT1 + INPUT2).getBytes(StandardCharsets.UTF_8));
            Assertions.assertThat(ws.read(arr)).isEqualTo(-1);
            Assertions.assertThat(arr).isEqualTo((INPUT1 + INPUT2).getBytes(StandardCharsets.UTF_8));
        }

        try (InputStreamWriteStream ws = new InputStreamWriteStream(ctx, timeoutSpec(), 2)) {
            final String INPUT1 = "abcd";
            final String INPUT2 = "efgh";
            ws.write(Buffer.buffer(INPUT1));
            ws.write(Buffer.buffer(INPUT2));
            ws.end();
            byte[] arr = new byte[10];
            Assertions.assertThat(ws.read(arr)).isEqualTo(8);
            Assertions.assertThat(arr).isEqualTo((INPUT1 + INPUT2 + "\0\0").getBytes(StandardCharsets.UTF_8));
            Assertions.assertThat(ws.read(arr)).isEqualTo(-1);
            Assertions.assertThat(arr).isEqualTo((INPUT1 + INPUT2 + "\0\0").getBytes(StandardCharsets.UTF_8));
        }

        try (InputStreamWriteStream ws = new InputStreamWriteStream(ctx, timeoutSpec(), 2)) {
            final String INPUT1 = "abcd";
            final String INPUT2 = "efgh";
            ws.write(Buffer.buffer(INPUT1));
            ws.write(Buffer.buffer(INPUT2));
            ws.end();
            byte[] arr = new byte[6];
            Assertions.assertThat(ws.read(arr)).isEqualTo(6);
            Assertions.assertThat(arr).isEqualTo(("abcdef").getBytes(StandardCharsets.UTF_8));
            Assertions.assertThat(ws.read(arr)).isEqualTo(2);
            Assertions.assertThat(arr).isEqualTo(("ghcdef").getBytes(StandardCharsets.UTF_8));
            Assertions.assertThat(ws.read(arr)).isEqualTo(-1);
            Assertions.assertThat(arr).isEqualTo(("ghcdef").getBytes(StandardCharsets.UTF_8));
        }

        try (InputStreamWriteStream ws = new InputStreamWriteStream(ctx, timeoutSpec(), 2)) {
            final String INPUT1 = "abcd";
            final String INPUT2 = "efgh";
            ws.write(Buffer.buffer(INPUT1));
            ws.write(Buffer.buffer(INPUT2));
            ws.end();
            byte[] arr = new byte[2];
            Assertions.assertThat(ws.read(arr)).isEqualTo(2);
            Assertions.assertThat(arr).isEqualTo(("ab").getBytes(StandardCharsets.UTF_8));
            Assertions.assertThat(ws.read(arr)).isEqualTo(2);
            Assertions.assertThat(arr).isEqualTo(("cd").getBytes(StandardCharsets.UTF_8));
            Assertions.assertThat(ws.read(arr)).isEqualTo(2);
            Assertions.assertThat(arr).isEqualTo(("ef").getBytes(StandardCharsets.UTF_8));
            Assertions.assertThat(ws.read(arr)).isEqualTo(2);
            Assertions.assertThat(arr).isEqualTo(("gh").getBytes(StandardCharsets.UTF_8));
            Assertions.assertThat(ws.read(arr)).isEqualTo(-1);
            Assertions.assertThat(arr).isEqualTo(("gh").getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    void readAfterClose() throws IOException {
        ContextInternal ctx = (ContextInternal) Vertx.vertx().getOrCreateContext();
        InputStreamWriteStream ws = new InputStreamWriteStream(ctx, timeoutSpec(), 2);
        final String INPUT1 = "abcd";
        final String INPUT2 = "efgh";
        final String INPUT3 = "ijkl";
        ws.write(Buffer.buffer(INPUT1));
        ws.write(Buffer.buffer(INPUT2));
        ws.write(Buffer.buffer(INPUT3));
        ws.end();
        byte[] arr = new byte[12];
        Assertions.assertThat(ws.read(arr)).isEqualTo(12);
        Assertions.assertThat(arr).isEqualTo((INPUT1 + INPUT2 + INPUT3).getBytes(StandardCharsets.UTF_8));
        Assertions.assertThat(ws.read(arr)).isEqualTo(-1);
        ws.close();
        Assertions.assertThat(ws.read(arr)).isEqualTo(-1);
        ws.close();
    }

    @Test
    void threeBuffers() throws IOException {

        ContextInternal ctx = (ContextInternal) Vertx.vertx().getOrCreateContext();
        try (InputStreamWriteStream ws = new InputStreamWriteStream(ctx, timeoutSpec(), 2)) {
            final String INPUT1 = "abcd";
            final String INPUT2 = "efgh";
            final String INPUT3 = "ijkl";
            ws.write(Buffer.buffer(INPUT1));
            ws.write(Buffer.buffer(INPUT2));
            ws.write(Buffer.buffer(INPUT3));
            ws.end();
            byte[] arr = new byte[12];
            Assertions.assertThat(ws.read(arr)).isEqualTo(12);
            Assertions.assertThat(arr).isEqualTo((INPUT1 + INPUT2 + INPUT3).getBytes(StandardCharsets.UTF_8));
            Assertions.assertThat(ws.read(arr)).isEqualTo(-1);
            Assertions.assertThat(arr).isEqualTo((INPUT1 + INPUT2 + INPUT3).getBytes(StandardCharsets.UTF_8));
        }

        try (InputStreamWriteStream ws = new InputStreamWriteStream(ctx, timeoutSpec(), 2)) {
            final String INPUT1 = "abcd";
            final String INPUT2 = "efgh";
            final String INPUT3 = "ijkl";
            ws.write(Buffer.buffer(INPUT1));
            ws.write(Buffer.buffer(INPUT2));
            ws.write(Buffer.buffer(INPUT3));
            ws.end();
            byte[] arr = new byte[14];
            Assertions.assertThat(ws.read(arr)).isEqualTo(12);
            Assertions.assertThat(arr).isEqualTo((INPUT1 + INPUT2 + INPUT3 + "\0\0").getBytes(StandardCharsets.UTF_8));
            Assertions.assertThat(ws.read(arr)).isEqualTo(-1);
            Assertions.assertThat(arr).isEqualTo((INPUT1 + INPUT2 + INPUT3 + "\0\0").getBytes(StandardCharsets.UTF_8));
        }

        try (InputStreamWriteStream ws = new InputStreamWriteStream(ctx, timeoutSpec(), 2)) {
            final String INPUT1 = "abcd";
            final String INPUT2 = "efgh";
            final String INPUT3 = "ijkl";
            ws.write(Buffer.buffer(INPUT1));
            ws.write(Buffer.buffer(INPUT2));
            ws.write(Buffer.buffer(INPUT3));
            ws.end();
            byte[] arr = new byte[10];
            Assertions.assertThat(ws.read(arr)).isEqualTo(10);
            Assertions.assertThat(arr).isEqualTo(("abcdefghij").getBytes(StandardCharsets.UTF_8));
            Assertions.assertThat(ws.read(arr)).isEqualTo(2);
            Assertions.assertThat(arr).isEqualTo(("klcdefghij").getBytes(StandardCharsets.UTF_8));
            Assertions.assertThat(ws.read(arr)).isEqualTo(-1);
            Assertions.assertThat(arr).isEqualTo(("klcdefghij").getBytes(StandardCharsets.UTF_8));
        }

    }
}
