package io.quarkiverse.cxf.vertx.http.client;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit.RequestBodyEvent;
import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit.RequestBodyEvent.RequestBodyEventType;
import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit.RequestBodyOutputStream;
import io.vertx.core.buffer.Buffer;

public class RequestBodyOutputStreamTest {

    @Test
    void writeSingleByte() throws IOException {

        /* No chunking */
        assertWriteByte(0, List.of("0", "1", "2"), List.of(new ExpectedEvent("012", RequestBodyEventType.COMPLETE_BODY)));
        {
            final List<String> writes = singleByteWrites(8 * 1024);
            assertWriteByte(0, writes, List.of(
                    new ExpectedEvent(writes.stream().collect(Collectors.joining("")), RequestBodyEventType.COMPLETE_BODY)));
        }
        assertWriteByte(0, List.of(), List.of(new ExpectedEvent("", RequestBodyEventType.COMPLETE_BODY)));

        /* Chunking */
        assertWriteByte(8, List.of("0", "1", "2"), List.of(new ExpectedEvent("012", RequestBodyEventType.COMPLETE_BODY)));
        assertWriteByte(2, List.of("0", "1", "2"),
                List.of(
                        new ExpectedEvent("01", RequestBodyEventType.NON_FINAL_CHUNK),
                        new ExpectedEvent("2", RequestBodyEventType.FINAL_CHUNK))

        );
        assertWriteByte(2, List.of("0", "1", "2", "3"),
                List.of(
                        new ExpectedEvent("01", RequestBodyEventType.NON_FINAL_CHUNK),
                        new ExpectedEvent("23", RequestBodyEventType.FINAL_CHUNK))

        );
        assertWriteByte(2, List.of(), List.of(new ExpectedEvent("", RequestBodyEventType.COMPLETE_BODY)));
    }

    @Test
    void writeByteArray() throws IOException {

        /* No chunking */
        assertWriteArray(0,
                List.of("0123456789", "0123456789", "0123456789"),
                List.of(new ExpectedEvent("012345678901234567890123456789", RequestBodyEventType.COMPLETE_BODY)));
        {
            final List<String> writes = byteArrayWrites(1024, 32);
            assertWriteArray(0, writes, List.of(
                    new ExpectedEvent(writes.stream().collect(Collectors.joining("")), RequestBodyEventType.COMPLETE_BODY)));
        }

        /* Chunking */
        assertWriteArray(8,
                List.of("01", "23", "4"),
                List.of(new ExpectedEvent("01234", RequestBodyEventType.COMPLETE_BODY)));

        assertWriteArray(4,
                List.of("0123"),
                List.of(new ExpectedEvent("0123", RequestBodyEventType.COMPLETE_BODY)));

        assertWriteArray(6, List.of("012", "3456"),
                List.of(
                        new ExpectedEvent("012345", RequestBodyEventType.NON_FINAL_CHUNK),
                        new ExpectedEvent("6", RequestBodyEventType.FINAL_CHUNK)));

        assertWriteArray(4, List.of("0123", "4567"),
                List.of(
                        new ExpectedEvent("0123", RequestBodyEventType.NON_FINAL_CHUNK),
                        new ExpectedEvent("4567", RequestBodyEventType.FINAL_CHUNK)));
    }

    static List<String> singleByteWrites(int length) {
        List<String> result = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            result.add(String.valueOf(i % 10));
        }
        return result;
    }

    static List<String> byteArrayWrites(int arrayLength, int iterations) {
        final List<String> result = new ArrayList<>(iterations);
        final StringBuilder sb = new StringBuilder(arrayLength);
        for (int j = 0; j < arrayLength; j++) {
            sb.append(String.valueOf(j % 10));
        }
        final String str = sb.toString();
        for (int i = 0; i < iterations; i++) {
            result.add(str);
        }
        return result;
    }

    static void assertWriteByte(int chunkSize, List<String> writes, List<ExpectedEvent> expectedEvents) throws IOException {
        assertWriteByte(chunkSize, writes, expectedEvents, (s, out) -> {
            try {
                out.write(s.getBytes(StandardCharsets.UTF_8)[0]);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        /* Must work in the same way when called over the array method */
        assertWriteArray(chunkSize, writes, expectedEvents);
    }

    static void assertWriteArray(int chunkSize, List<String> writes, List<ExpectedEvent> expectedEvents) throws IOException {
        assertWriteByte(chunkSize, writes, expectedEvents, (s, out) -> {
            try {
                out.write(s.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    static void assertWriteByte(int chunkSize, List<String> writes, List<ExpectedEvent> expectedEvents,
            BiConsumer<String, OutputStream> writer) throws IOException {
        final List<RequestBodyEvent> events = new ArrayList<>();
        try (RequestBodyOutputStream out = new RequestBodyOutputStream(chunkSize, events::add)) {
            for (String write : writes) {
                writer.accept(write, out);
            }
        }
        Assertions.assertThat(events)
                .satisfiesExactly(expectedEvents.toArray(new ExpectedEvent[0]))
                .hasSize(expectedEvents.size());
    }

    record ExpectedEvent(String expectedBuffer, RequestBodyEventType expectedEventType)
            implements
                ThrowingConsumer<RequestBodyEvent> {

        @Override
        public void acceptThrows(RequestBodyEvent actualEvent) throws Throwable {
            final String actualBuf = bufToString(actualEvent.buffer());
            Assertions.assertThat(actualBuf).isEqualTo(expectedBuffer);
            Assertions.assertThat(actualEvent.eventType()).isEqualTo(expectedEventType);
        }
    }

    static String bufToString(Buffer buf) {
        return new String(buf.getBytes(), StandardCharsets.UTF_8);
    }

}
