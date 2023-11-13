package io.quarkiverse.cxf.it.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.ConnectionClosedException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.MalformedChunkCodingException;
import org.apache.http.TruncatedChunkException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.MessageConstraints;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpConnectionFactory;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.DefaultHttpResponseParserFactory;
import org.apache.http.impl.conn.DefaultManagedHttpClientConnection;
import org.apache.http.impl.conn.ManagedHttpClientConnectionFactory;
import org.apache.http.impl.entity.LaxContentLengthStrategy;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
import org.apache.http.impl.io.AbstractMessageParser;
import org.apache.http.impl.io.ContentLengthInputStream;
import org.apache.http.impl.io.DefaultHttpRequestWriterFactory;
import org.apache.http.impl.io.EmptyInputStream;
import org.apache.http.impl.io.IdentityInputStream;
import org.apache.http.io.BufferInfo;
import org.apache.http.io.HttpMessageParserFactory;
import org.apache.http.io.HttpMessageWriterFactory;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.util.Args;
import org.apache.http.util.CharArrayBuffer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ChunkedTest {

    @Test
    public void chunked() throws IOException {

        final int outputBufferSize = QuarkusCxfClientTestUtil.getClient(LargeEntityService.class, "/soap/large-entity")
                .outputBufferSize();

        Assertions.assertThat(outputBufferSize).isEqualTo(8191);

        List<Long> chunks = new CopyOnWriteArrayList<>();

        BasicHttpClientConnectionManager connManager = new BasicHttpClientConnectionManager(
                getDefaultRegistry(),
                new CustomManagedHttpClientConnectionFactory(chunks::add),
                null,
                null);

        try (CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connManager).build()) {
            HttpPost httpPost = new HttpPost("http://localhost:8081/soap/large-entity");
            byte[] body = largeRequest(1024, 1024);
            httpPost.setEntity(new ByteArrayEntity(body, org.apache.http.entity.ContentType.TEXT_XML));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();

                Assertions.assertThat(statusCode).isEqualTo(200);

                HttpEntity entity = response.getEntity();
                InputStream stream = entity.getContent();

                byte[] payload = stream.readAllBytes();
                final String payloadStr = new String(payload, StandardCharsets.UTF_8);
                Assertions.assertThat(payloadStr).startsWith(
                        "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns2:itemsResponse xmlns:ns2=\"http://server.it.cxf.quarkiverse.io/\"><return>01234567890");
                Assertions.assertThat(payload.length).isEqualTo(1066181);

                /* We add one for the final zero length chunk */
                final int expectedChunksCount = 1 + (int) Math.ceil((double) payload.length / outputBufferSize);
                Assertions.assertThat(chunks.size()).isLessThanOrEqualTo(expectedChunksCount);
            }
        }
    }

    private static Registry<ConnectionSocketFactory> getDefaultRegistry() {
        return RegistryBuilder.<ConnectionSocketFactory> create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", SSLConnectionSocketFactory.getSocketFactory())
                .build();
    }

    private byte[] largeRequest(int count, int itemLength) {
        return ("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "  <soap:Body>\n"
                + "    <ns2:items xmlns:ns2=\"http://server.it.cxf.quarkiverse.io/\">\n"
                + "      <count>" + count + "</count>\n"
                + "      <itemLength>" + itemLength + "</itemLength>\n"
                + "    </ns2:items>\n"
                + "  </soap:Body>\n"
                + "</soap:Envelope>").getBytes(StandardCharsets.UTF_8);
    }

    /*
     * Below are some custom HTTP Client classes to be able to count chunks.
     * It is HTTP 4.5 because that one is brought by REST-assured
     */

    static class CustomManagedHttpClientConnectionFactory
            implements HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> {

        private static final AtomicLong COUNTER = new AtomicLong();

        public static final ManagedHttpClientConnectionFactory INSTANCE = new ManagedHttpClientConnectionFactory();

        private final Log log = LogFactory.getLog(DefaultManagedHttpClientConnection.class);
        private final Log headerLog = LogFactory.getLog("org.apache.http.headers");
        private final Log wireLog = LogFactory.getLog("org.apache.http.wire");

        private final HttpMessageWriterFactory<HttpRequest> requestWriterFactory;
        private final HttpMessageParserFactory<HttpResponse> responseParserFactory;
        private final ContentLengthStrategy incomingContentStrategy;
        private final ContentLengthStrategy outgoingContentStrategy;
        private final Consumer<Long> chunkConsumer;

        /**
         * @since 4.4
         */
        public CustomManagedHttpClientConnectionFactory(
                final HttpMessageWriterFactory<HttpRequest> requestWriterFactory,
                final HttpMessageParserFactory<HttpResponse> responseParserFactory,
                final ContentLengthStrategy incomingContentStrategy,
                final ContentLengthStrategy outgoingContentStrategy,
                Consumer<Long> chunkConsumer) {
            super();
            this.requestWriterFactory = requestWriterFactory != null ? requestWriterFactory
                    : DefaultHttpRequestWriterFactory.INSTANCE;
            this.responseParserFactory = responseParserFactory != null ? responseParserFactory
                    : DefaultHttpResponseParserFactory.INSTANCE;
            this.incomingContentStrategy = incomingContentStrategy != null ? incomingContentStrategy
                    : LaxContentLengthStrategy.INSTANCE;
            this.outgoingContentStrategy = outgoingContentStrategy != null ? outgoingContentStrategy
                    : StrictContentLengthStrategy.INSTANCE;
            this.chunkConsumer = chunkConsumer;
        }

        public CustomManagedHttpClientConnectionFactory(Consumer<Long> chunkConsumer) {
            this(null, null, null, null, chunkConsumer);
        }

        @Override
        public ManagedHttpClientConnection create(HttpRoute route, ConnectionConfig config) {
            final ConnectionConfig cconfig = config != null ? config : ConnectionConfig.DEFAULT;
            CharsetDecoder charDecoder = null;
            CharsetEncoder charEncoder = null;
            final Charset charset = cconfig.getCharset();
            final CodingErrorAction malformedInputAction = cconfig.getMalformedInputAction() != null
                    ? cconfig.getMalformedInputAction()
                    : CodingErrorAction.REPORT;
            final CodingErrorAction unmappableInputAction = cconfig.getUnmappableInputAction() != null
                    ? cconfig.getUnmappableInputAction()
                    : CodingErrorAction.REPORT;
            if (charset != null) {
                charDecoder = charset.newDecoder();
                charDecoder.onMalformedInput(malformedInputAction);
                charDecoder.onUnmappableCharacter(unmappableInputAction);
                charEncoder = charset.newEncoder();
                charEncoder.onMalformedInput(malformedInputAction);
                charEncoder.onUnmappableCharacter(unmappableInputAction);
            }
            final String id = "http-outgoing-" + Long.toString(COUNTER.getAndIncrement());
            return new DefaultManagedHttpClientConnection(
                    id,
                    cconfig.getBufferSize(),
                    cconfig.getFragmentSizeHint(),
                    charDecoder,
                    charEncoder,
                    cconfig.getMessageConstraints(),
                    incomingContentStrategy,
                    outgoingContentStrategy,
                    requestWriterFactory,
                    responseParserFactory) {

                @Override
                protected InputStream createInputStream(
                        final long len,
                        final SessionInputBuffer inBuffer) {
                    if (len == ContentLengthStrategy.CHUNKED) {
                        return new CountingChunkedInputStream(inBuffer, null, chunkConsumer);
                    } else if (len == ContentLengthStrategy.IDENTITY) {
                        return new IdentityInputStream(inBuffer);
                    } else if (len == 0L) {
                        return EmptyInputStream.INSTANCE;
                    } else {
                        return new ContentLengthInputStream(inBuffer, len);
                    }
                }
            };
        }

    }

    static class CountingChunkedInputStream extends InputStream {

        private static final int CHUNK_LEN = 1;
        private static final int CHUNK_DATA = 2;
        private static final int CHUNK_CRLF = 3;
        private static final int CHUNK_INVALID = Integer.MAX_VALUE;

        private static final int BUFFER_SIZE = 2048;

        /** The session input buffer */
        private final SessionInputBuffer in;
        private final CharArrayBuffer buffer;
        private final MessageConstraints constraints;

        private final Consumer<Long> chunkConsumer;

        private int state;

        /** The chunk size */
        private long chunkSize;

        /** The current position within the current chunk */
        private long pos;

        /** True if we've reached the end of stream */
        private boolean eof = false;

        /** True if this stream is closed */
        private boolean closed = false;

        private Header[] footers = new Header[] {};

        /**
         * Wraps session input stream and reads chunk coded input.
         *
         * @param in The session input buffer
         * @param constraints Message constraints. If {@code null}
         *        {@link MessageConstraints#DEFAULT} will be used.
         *
         * @since 4.4
         */
        public CountingChunkedInputStream(final SessionInputBuffer in, final MessageConstraints constraints,
                Consumer<Long> chunkConsumer) {
            super();
            this.in = Args.notNull(in, "Session input buffer");
            this.pos = 0L;
            this.buffer = new CharArrayBuffer(16);
            this.constraints = constraints != null ? constraints : MessageConstraints.DEFAULT;
            this.state = CHUNK_LEN;
            this.chunkConsumer = chunkConsumer;
        }

        @Override
        public int available() throws IOException {
            if (this.in instanceof BufferInfo) {
                final int len = ((BufferInfo) this.in).length();
                return (int) Math.min(len, this.chunkSize - this.pos);
            }
            return 0;
        }

        /**
         * <p>
         * Returns all the data in a chunked stream in coalesced form. A chunk
         * is followed by a CRLF. The method returns -1 as soon as a chunksize of 0
         * is detected.
         * </p>
         *
         * <p>
         * Trailer headers are read automatically at the end of the stream and
         * can be obtained with the getResponseFooters() method.
         * </p>
         *
         * @return -1 of the end of the stream has been reached or the next data
         *         byte
         * @throws IOException in case of an I/O error
         */
        @Override
        public int read() throws IOException {
            if (this.closed) {
                throw new IOException("Attempted read from closed stream.");
            }
            if (this.eof) {
                return -1;
            }
            if (state != CHUNK_DATA) {
                nextChunk();
                if (this.eof) {
                    return -1;
                }
            }
            final int b = in.read();
            if (b != -1) {
                pos++;
                if (pos >= chunkSize) {
                    state = CHUNK_CRLF;
                }
            }
            return b;
        }

        /**
         * Read some bytes from the stream.
         *
         * @param b The byte array that will hold the contents from the stream.
         * @param off The offset into the byte array at which bytes will start to be
         *        placed.
         * @param len the maximum number of bytes that can be returned.
         * @return The number of bytes returned or -1 if the end of stream has been
         *         reached.
         * @throws IOException in case of an I/O error
         */
        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {

            if (closed) {
                throw new IOException("Attempted read from closed stream.");
            }

            if (eof) {
                return -1;
            }
            if (state != CHUNK_DATA) {
                nextChunk();
                if (eof) {
                    return -1;
                }
            }
            final int readLen = in.read(b, off, (int) Math.min(len, chunkSize - pos));
            if (readLen != -1) {
                pos += readLen;
                if (pos >= chunkSize) {
                    state = CHUNK_CRLF;
                }
                return readLen;
            }
            eof = true;
            throw new TruncatedChunkException("Truncated chunk (expected size: %,d; actual size: %,d)",
                    chunkSize, pos);
        }

        /**
         * Read some bytes from the stream.
         *
         * @param b The byte array that will hold the contents from the stream.
         * @return The number of bytes returned or -1 if the end of stream has been
         *         reached.
         * @throws IOException in case of an I/O error
         */
        @Override
        public int read(final byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        /**
         * Read the next chunk.
         *
         * @throws IOException in case of an I/O error
         */
        private void nextChunk() throws IOException {
            if (state == CHUNK_INVALID) {
                throw new MalformedChunkCodingException("Corrupt data stream");
            }
            try {
                chunkSize = getChunkSize();
                chunkConsumer.accept(chunkSize);
                if (chunkSize < 0L) {
                    throw new MalformedChunkCodingException("Negative chunk size");
                }
                state = CHUNK_DATA;
                pos = 0L;
                if (chunkSize == 0L) {
                    eof = true;
                    parseTrailerHeaders();
                }
            } catch (final MalformedChunkCodingException ex) {
                state = CHUNK_INVALID;
                throw ex;
            }
        }

        /**
         * Expects the stream to start with a chunksize in hex with optional
         * comments after a semicolon. The line must end with a CRLF: "a3; some
         * comment\r\n" Positions the stream at the start of the next line.
         */
        private long getChunkSize() throws IOException {
            final int st = this.state;
            switch (st) {
                case CHUNK_CRLF:
                    this.buffer.clear();
                    final int bytesRead1 = this.in.readLine(this.buffer);
                    if (bytesRead1 == -1) {
                        throw new MalformedChunkCodingException(
                                "CRLF expected at end of chunk");
                    }
                    if (!this.buffer.isEmpty()) {
                        throw new MalformedChunkCodingException(
                                "Unexpected content at the end of chunk");
                    }
                    state = CHUNK_LEN;
                    //$FALL-THROUGH$
                case CHUNK_LEN:
                    this.buffer.clear();
                    final int bytesRead2 = this.in.readLine(this.buffer);
                    if (bytesRead2 == -1) {
                        throw new ConnectionClosedException(
                                "Premature end of chunk coded message body: closing chunk expected");
                    }
                    int separator = this.buffer.indexOf(';');
                    if (separator < 0) {
                        separator = this.buffer.length();
                    }
                    final String s = this.buffer.substringTrimmed(0, separator);
                    try {
                        return Long.parseLong(s, 16);
                    } catch (final NumberFormatException e) {
                        throw new MalformedChunkCodingException("Bad chunk header: " + s);
                    }
                default:
                    throw new IllegalStateException("Inconsistent codec state");
            }
        }

        /**
         * Reads and stores the Trailer headers.
         *
         * @throws IOException in case of an I/O error
         */
        private void parseTrailerHeaders() throws IOException {
            try {
                this.footers = AbstractMessageParser.parseHeaders(in,
                        constraints.getMaxHeaderCount(),
                        constraints.getMaxLineLength(),
                        null);
            } catch (final HttpException ex) {
                final IOException ioe = new MalformedChunkCodingException("Invalid footer: "
                        + ex.getMessage());
                ioe.initCause(ex);
                throw ioe;
            }
        }

        /**
         * Upon close, this reads the remainder of the chunked message,
         * leaving the underlying socket at a position to start reading the
         * next response without scanning.
         *
         * @throws IOException in case of an I/O error
         */
        @Override
        public void close() throws IOException {
            if (!closed) {
                try {
                    if (!eof && state != CHUNK_INVALID) {
                        // read and discard the remainder of the message
                        final byte buff[] = new byte[BUFFER_SIZE];
                        while (read(buff) >= 0) {
                        }
                    }
                } finally {
                    eof = true;
                    closed = true;
                }
            }
        }

        public Header[] getFooters() {
            return this.footers.clone();
        }

    }
}
