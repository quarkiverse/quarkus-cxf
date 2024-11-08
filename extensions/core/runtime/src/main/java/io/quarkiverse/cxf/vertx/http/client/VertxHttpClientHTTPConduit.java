/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.quarkiverse.cxf.vertx.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PushbackInputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.ClientCallback;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.http.Address;
import org.apache.cxf.transport.http.Cookies;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPException;
import org.apache.cxf.transport.http.Headers;
import org.apache.cxf.transport.http.MessageTrustDecider;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.version.Version;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import io.quarkiverse.cxf.QuarkusTLSClientParameters;
import io.quarkiverse.cxf.vertx.http.client.HttpClientPool.ClientSpec;
import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit.RequestBodyEvent.RequestBodyEventType;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;

/**
 */
public class VertxHttpClientHTTPConduit extends HTTPConduit {
    private static final Logger LOG = LogUtils.getL7dLogger(VertxHttpClientHTTPConduit.class);

    private final HttpClientPool httpClientPool;
    private final String userAgent;

    public VertxHttpClientHTTPConduit(Bus b, EndpointInfo ei, EndpointReferenceType t, HttpClientPool httpClientPool)
            throws IOException {
        super(b, ei, t);
        this.httpClientPool = httpClientPool;
        this.userAgent = Version.getCompleteVersionString();
    }

    @Override
    protected void setupConnection(Message message, Address address, HTTPClientPolicy csPolicy) throws IOException {
        final RequestOptions requestOptions = new RequestOptions();

        final URI uri = address.getURI();
        final String scheme = uri.getScheme();
        message.put("http.scheme", scheme);

        final HttpMethod method = getMethod(message);

        // message.put("use.async.http.conduit", Boolean.TRUE);

        final HttpVersion version = getVersion(message, csPolicy);
        final boolean isHttps = "https".equals(uri.getScheme());
        final QuarkusTLSClientParameters clientParameters;
        if (isHttps) {
            clientParameters = findTLSClientParameters(message);
            if (clientParameters.getSSLSocketFactory() != null) {
                throw new IllegalStateException(VertxHttpClientHTTPConduit.class.getName()
                        + " does not support SSLSocketFactory set via TLSClientParameters");
            }
            if (clientParameters.getSslContext() != null) {
                throw new IllegalStateException(VertxHttpClientHTTPConduit.class.getName()
                        + " does not support SSLContext set via TLSClientParameters");
            }
            if (clientParameters.isUseHttpsURLConnectionDefaultSslSocketFactory()) {
                throw new IllegalStateException(VertxHttpClientHTTPConduit.class.getName()
                        + " does not support TLSClientParameters.isUseHttpsURLConnectionDefaultSslSocketFactory() returning true");
            }
            final List<MessageTrustDecider> trustDeciders;
            final MessageTrustDecider decider2;
            if ((decider2 = message.get(MessageTrustDecider.class)) != null || this.trustDecider != null) {
                trustDeciders = new ArrayList<>(2);
                if (this.trustDecider != null) {
                    trustDeciders.add(this.trustDecider);
                }
                if (decider2 != null) {
                    trustDeciders.add(decider2);
                }
            } else {
                trustDeciders = Collections.emptyList();
            }
        } else {
            clientParameters = null;
        }

        final Proxy proxy = proxyFactory.createProxy(csPolicy, uri);
        InetSocketAddress adr;
        if (proxy != null && (adr = (InetSocketAddress) proxy.address()) != null) {
            requestOptions.setProxyOptions(
                    new ProxyOptions()
                            .setHost(adr.getHostName())
                            .setPort(adr.getPort())
                            .setType(toProxyType(proxy.type())));
        }

        final String query = uri.getQuery();
        final String pathAndQuery = query != null && !query.isEmpty()
                ? uri.getPath() + "?" + query
                : uri.getPath();
        requestOptions
                .setMethod(method)
                .setHost(uri.getHost())
                .setURI(pathAndQuery)
                .setConnectTimeout(determineConnectionTimeout(message, csPolicy));

        final int port = uri.getPort();
        if (port >= 0) {
            requestOptions
                    .setPort(uri.getPort());
        }

        final RequestContext requestContext = new RequestContext(
                uri,
                requestOptions,
                clientParameters != null
                        ? new ClientSpec(version, clientParameters.getTlsConfigurationName(),
                                clientParameters.getTlsConfiguration())
                        : new ClientSpec(version, null, null),
                determineReceiveTimeout(message, csPolicy));
        message.put(RequestContext.class, requestContext);

    }

    static ProxyType toProxyType(Type type) {
        switch (type) {
            case HTTP:
                return ProxyType.HTTP;
            case SOCKS:
                return ProxyType.SOCKS4;
            default:
                throw new IllegalArgumentException("Unexpected " + Type.class.getName() + " " + type);
        }
    }

    @Override
    protected OutputStream createOutputStream(
            Message message,
            boolean possibleRetransmit,
            boolean isChunking,
            int chunkThreshold) throws IOException {
        final RequestContext requestContext = message.get(RequestContext.class);
        final IOEHandler<ResponseEvent> responseHandler = new ResponseHandler(
                requestContext.uri,
                message,
                cookies,
                incomingObserver);

        final IOEHandler<RequestBodyEvent> requestBodyHandler = new RequestBodyHandler(
                message,
                requestContext.uri,
                userAgent,
                httpClientPool,
                requestContext.requestOptions,
                requestContext.clientSpec,
                requestContext.receiveTimeoutMs,
                responseHandler);
        return new RequestBodyOutputStream(chunkThreshold, requestBodyHandler);
    }

    static HttpVersion getVersion(Message message, HTTPClientPolicy csPolicy) {
        String verc = (String) message.getContextualProperty(FORCE_HTTP_VERSION);
        if (verc == null) {
            verc = csPolicy.getVersion();
        }
        if (verc == null) {
            verc = "1.1";
        }
        final HttpVersion v = switch (verc) {
            case "2": {
                yield HttpVersion.HTTP_2;
            }
            case "auto":
            case "1.1": {
                yield HttpVersion.HTTP_1_1;
            }
            case "1.0": {
                yield HttpVersion.HTTP_1_0;
            }
            default:
                throw new IllegalArgumentException("Unexpected HTTP protocol version " + verc);
        };
        return v;
    }

    static HttpMethod getMethod(Message message) {
        final HttpMethod method;
        String rawRequestMethod = (String) message.get(Message.HTTP_REQUEST_METHOD);
        if (rawRequestMethod == null) {
            method = HttpMethod.POST;
            message.put(Message.HTTP_REQUEST_METHOD, "POST");
        } else {
            method = HttpMethod.valueOf(rawRequestMethod);
        }
        return method;
    }

    QuarkusTLSClientParameters findTLSClientParameters(Message message) {
        TLSClientParameters clientParameters = message.get(TLSClientParameters.class);
        if (clientParameters == null) {
            clientParameters = this.tlsClientParameters;
        }
        if (clientParameters == null) {
            clientParameters = new QuarkusTLSClientParameters(null, null);
        }

        if (clientParameters.getHostnameVerifier() != null) {
            throw new IllegalStateException(
                    getConduitName() + " does not support setting a hostname verifier."
                            + " AllowAllHostnameVerifier can be replaced by using a named TLS configuration"
                            + " with hostname-verification-algorithm set to NONE");
        }

        if (clientParameters instanceof QuarkusTLSClientParameters) {
            return (QuarkusTLSClientParameters) clientParameters;
        }
        throw new IllegalStateException(
                VertxHttpClientHTTPConduit.class.getName() + " accepts only " + QuarkusTLSClientParameters.class.getName());
    }

    @Override
    public void setTlsClientParameters(TLSClientParameters params) {
        if (params != null && !(params instanceof QuarkusTLSClientParameters)) {
            throw new IllegalStateException(
                    VertxHttpClientHTTPConduit.class.getName() + " accepts only " + QuarkusTLSClientParameters.class.getName());
        }
        super.setTlsClientParameters(params);
    }

    static record RequestContext(
            URI uri,
            RequestOptions requestOptions,
            ClientSpec clientSpec,
            long receiveTimeoutMs) {
    }

    static record RequestBodyEvent(Buffer buffer, RequestBodyEventType eventType) {
        public enum RequestBodyEventType {
            NON_FINAL_CHUNK,
            FINAL_CHUNK,
            COMPLETE_BODY
        };
    }

    static class RequestBodyOutputStream extends OutputStream {
        private Buffer buffer;
        private final int chunkSize;
        private final IOEHandler<RequestBodyEvent> bodyHandler;
        private boolean closed = false;
        private boolean firstChunkSent = false;

        /**
         * {@code chunkSize} {@code 0} or less means no chunking - i.e. the buffer will grow
         * endlessly and the {@code bodyHandler} will be notified only once at {@link #close()}.
         *
         * @param chunkSize
         * @param bodyHandler
         */
        public RequestBodyOutputStream(int chunkSize, IOEHandler<RequestBodyEvent> bodyHandler) {
            this.chunkSize = chunkSize;
            this.bodyHandler = bodyHandler;
            this.buffer = Buffer.buffer(chunkSize);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (chunkSize > 0) {
                int remainingCapacity;
                while ((remainingCapacity = chunkSize - buffer.length()) < len) {
                    /* Split the bytes */
                    buffer.appendBytes(b, off, remainingCapacity);
                    off += remainingCapacity;
                    len -= remainingCapacity;
                    final Buffer buf = buffer;
                    bodyHandler.handle(new RequestBodyEvent(buf, RequestBodyEventType.NON_FINAL_CHUNK));
                    firstChunkSent = true;
                    buffer = Buffer.buffer(chunkSize);
                }
            }

            if (len > 0) {
                /* Write the rest */
                buffer.appendBytes(b, off, len);
            }

        }

        @Override
        public void write(int b) throws IOException {
            if (chunkSize > 0 && buffer.length() == chunkSize) {
                final Buffer buf = buffer;
                bodyHandler.handle(new RequestBodyEvent(buf, RequestBodyEventType.NON_FINAL_CHUNK));
                firstChunkSent = true;
                buffer = Buffer.buffer(chunkSize);
            }
            buffer.appendByte((byte) b);
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                super.close();
                RequestBodyEventType eventType = firstChunkSent ? RequestBodyEventType.FINAL_CHUNK
                        : RequestBodyEventType.COMPLETE_BODY;
                final Buffer buf = buffer;
                buffer = null;
                bodyHandler.handle(new RequestBodyEvent(buf, eventType));
            }
        }
    }

    static class RequestBodyHandler implements IOEHandler<RequestBodyEvent> {
        private final Message outMessage;
        private final URI url;
        private final String userAgent;
        private final HttpClientPool clientPool;
        private final RequestOptions requestOptions;
        private final ClientSpec clientSpec;
        /** Time in epoch milliseconds when the response should be fully received */
        private final long receiveTimeoutDeadline;
        private final IOEHandler<ResponseEvent> responseHandler;

        /** Read an written only from the producer thread */
        private boolean firstEvent = true;
        /**
         * Read from the producer thread, written from the event loop. Protected by {@link #lock} {@link #requestReady}
         * {@link Condition}
         */
        private Result<HttpClientRequest> request;
        /**
         * Read from the producer thread, written from the event loop. Protected by {@link #lock} {@link #responseReceived}
         * {@link Condition}
         */
        private Result<ResponseEvent> response;

        /* Locks and conditions */
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition requestReady = lock.newCondition();
        private final Condition requestWriteable = lock.newCondition();
        private final Condition responseReceived = lock.newCondition();

        /* Backpressure control when writing the request body */
        private boolean drainHandlerRegistered;
        private boolean waitingForDrain;

        public RequestBodyHandler(
                Message outMessage,
                URI url,
                String userAgent,
                HttpClientPool clientPool,
                RequestOptions requestOptions,
                ClientSpec clientSpec,
                long receiveTimeoutMs,
                IOEHandler<ResponseEvent> responseHandler) {
            super();
            this.outMessage = outMessage;
            this.url = url;
            this.userAgent = userAgent;
            this.clientPool = clientPool;
            this.requestOptions = requestOptions;
            this.clientSpec = clientSpec;
            this.receiveTimeoutDeadline = System.currentTimeMillis() + receiveTimeoutMs;
            this.responseHandler = responseHandler;
        }

        @Override
        public void handle(RequestBodyEvent event) throws IOException {
            if (firstEvent) {
                firstEvent = false;
                final HttpClient client = clientPool.getClient(clientSpec);

                switch (event.eventType()) {
                    case NON_FINAL_CHUNK:
                    case FINAL_CHUNK: {
                        break;
                    }
                    case COMPLETE_BODY: {
                        requestOptions.putHeader("Content-Length", String.valueOf(event.buffer().length()));
                        break;
                    }
                    default:
                        throw new IllegalArgumentException(
                                "Unexpected " + RequestBodyEventType.class.getName() + ": " + event.eventType());
                }

                setProtocolHeaders(outMessage, requestOptions, userAgent);

                client.request(requestOptions)
                        .onSuccess(req -> {
                            switch (event.eventType()) {
                                case NON_FINAL_CHUNK: {
                                    req
                                            .setChunked(true)
                                            .write(event.buffer())
                                            .onFailure(RequestBodyHandler.this::failResponse);

                                    lock.lock();
                                    try {
                                        this.request = new Result<>(req, null);
                                        requestReady.signal();
                                    } finally {
                                        lock.unlock();
                                    }

                                    break;
                                }
                                case FINAL_CHUNK:
                                case COMPLETE_BODY: {
                                    finishRequest(req, event.buffer());
                                    break;
                                }
                                default:
                                    throw new IllegalArgumentException(
                                            "Unexpected " + RequestBodyEventType.class.getName() + ": " + event.eventType());

                            }
                        })
                        .onFailure(t -> {
                            lock.lock();
                            try {
                                request = Result.failure(t);
                                requestReady.signal();

                                /* Fail also the response so that awaitResponse() fails rather than waiting forever */
                                response = Result.failure(t);
                                responseReceived.signal();
                            } finally {
                                lock.unlock();
                            }
                        });

                switch (event.eventType()) {
                    case NON_FINAL_CHUNK:
                        /* Nothing to do */
                        break;
                    case FINAL_CHUNK:
                    case COMPLETE_BODY: {
                        responseHandler.handle(awaitResponse());
                        break;
                    }
                    default:
                        throw new IllegalArgumentException(
                                "Unexpected " + RequestBodyEventType.class.getName() + ": " + event.eventType());

                }

            } else {
                /* Non-first event */
                final HttpClientRequest req = awaitRequest();
                switch (event.eventType()) {
                    case NON_FINAL_CHUNK: {
                        req
                                .write(event.buffer())
                                .onFailure(RequestBodyHandler.this::failResponse);
                        break;
                    }
                    case FINAL_CHUNK:
                    case COMPLETE_BODY: {
                        finishRequest(req, event.buffer());
                        responseHandler.handle(awaitResponse());
                        break;
                    }
                    default:
                        throw new IllegalArgumentException(
                                "Unexpected " + RequestBodyEventType.class.getName() + ": " + event.eventType());

                }
            }
        }

        void finishRequest(HttpClientRequest req, Buffer buffer) {
            try {
                final PipedOutputStream pipedOutputStream = new PipedOutputStream();
                final ExceptionAwarePipedInputStream pipedInputStream = new ExceptionAwarePipedInputStream(
                        pipedOutputStream);

                req.response()
                        .onComplete(ar -> {
                            if (ar.succeeded()) {
                                pipe(ar.result(), pipedOutputStream, pipedInputStream);
                            } else {
                                if (ar.cause() instanceof IOException) {
                                    pipedInputStream.setException((IOException) ar.cause());
                                } else {
                                    pipedInputStream.setException(new IOException(ar.cause()));
                                }
                            }
                            lock.lock();
                            try {
                                response = new Result<>(new ResponseEvent(ar.result(), pipedInputStream),
                                        ar.cause());
                                responseReceived.signal();
                            } finally {
                                lock.unlock();
                            }
                        });

                req
                        .end(buffer)
                        .onFailure(RequestBodyHandler.this::failResponse);

            } catch (IOException e) {
                throw new VertxHttpException(e);
            }
        }

        void failResponse(Throwable t) {
            lock.lock();
            try {
                response = Result.failure(t);
                responseReceived.signal();
            } finally {
                lock.unlock();
            }
        }

        static void setProtocolHeaders(Message outMessage, RequestOptions requestOptions, String userAgent) throws IOException {
            final Headers h = new Headers(outMessage);
            final MultiMap outHeaders;
            final String contentType;
            if (requestHasBody(requestOptions.getMethod())
                    && (contentType = h.determineContentType()) != null) {
                requestOptions.putHeader(HttpHeaderHelper.CONTENT_TYPE, contentType);
                outHeaders = requestOptions.getHeaders();
            } else {
                outHeaders = HttpHeaders.headers();
                requestOptions.setHeaders(outHeaders);
            }

            boolean addHeaders = MessageUtils.getContextualBoolean(outMessage, Headers.ADD_HEADERS_PROPERTY, false);

            for (Map.Entry<String, List<String>> header : h.headerMap().entrySet()) {
                if (HttpHeaderHelper.CONTENT_TYPE.equalsIgnoreCase(header.getKey())) {
                    continue;
                }
                if (addHeaders || HttpHeaderHelper.COOKIE.equalsIgnoreCase(header.getKey())) {
                    List<String> values = header.getValue();
                    for (String s : values) {
                        outHeaders.add(HttpHeaderHelper.COOKIE, s);
                    }
                } else if (!"Content-Length".equalsIgnoreCase(header.getKey())) {
                    final List<String> values = header.getValue();
                    final int len = values.size();
                    switch (len) {
                        case 0: {
                            outHeaders.set(header.getKey(), "");
                            break;
                        }
                        case 1: {
                            outHeaders.set(header.getKey(), values.get(0));
                            break;
                        }
                        default:
                            final StringBuilder b = new StringBuilder();
                            for (int i = 0; i < len; i++) {
                                b.append(values.get(i));
                                if (i + 1 < len) {
                                    b.append(',');
                                }
                            }
                            outHeaders.set(header.getKey(), b.toString());
                            break;
                    }
                }
                if (!outHeaders.contains("User-Agent")) {
                    outHeaders.set("User-Agent", userAgent);
                }
            }
        }

        static boolean requestHasBody(HttpMethod method) {
            if (HttpMethod.POST == method) {
                /* Fast track for the most likely value */
                return true;
            }
            if (
            /* Fast track for the second most likely value */
            method == HttpMethod.GET
                    || method == HttpMethod.HEAD
                    || method == HttpMethod.OPTIONS
                    || method == HttpMethod.TRACE) {
                return false;
            }
            return true;
        }

        ResponseEvent awaitResponse() throws IOException {
            /* This should be called from the same worker thread as handle() */
            if (response == null) {
                lock.lock();
                try {
                    if (response == null) {
                        if (!responseReceived.await(receiveTimeout(), TimeUnit.MILLISECONDS) || response == null) {
                            throw new SocketTimeoutException("Timeout waiting for HTTP response from " + url);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted waiting for HTTP response from " + url, e);
                } finally {
                    lock.unlock();
                }
            }
            if (response.succeeded()) {
                return response.result();
            } else {
                final Throwable e = response.cause();
                throw new IOException("Unable to receive HTTP response from " + url, e);
            }
        }

        HttpClientRequest awaitRequest() throws IOException {
            /* This should be called from the same worker thread as handle() */
            if (request == null) {
                lock.lock();
                try {
                    if (request == null) {
                        if (!requestReady.await(requestOptions.getConnectTimeout(), TimeUnit.MILLISECONDS) || request == null) {
                            throw new SocketTimeoutException("Timeout waiting for HTTP connect to " + url);
                        }
                    }
                    if (request.succeeded()) {
                        awaitWriteable(request.result());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted waiting for HTTP response from " + url, e);
                } finally {
                    lock.unlock();
                }
            }
            if (request.succeeded()) {
                return request.result();
            } else {
                final Throwable e = request.cause();
                throw new IOException("Unable to connect to " + url, e);
            }
        }

        static void pipe(
                HttpClientResponse response,
                PipedOutputStream pipedOutputStream,
                ExceptionAwarePipedInputStream pipedInputStream

        ) {

            response.handler(buffer -> {
                try {
                    pipedOutputStream.write(buffer.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            response.endHandler(v -> {
                try {
                    pipedOutputStream.close();
                } catch (IOException e) {
                    pipedInputStream.setException(e);
                }
            });

            response.exceptionHandler(e -> {
                final IOException ioe = e instanceof IOException
                        ? (IOException) e
                        : new IOException(e);
                pipedInputStream.setException(ioe);
            });
        }

        void awaitWriteable(HttpClientRequest request) throws IOException, InterruptedException {
            assert lock.isHeldByCurrentThread();
            while (request.writeQueueFull()) {
                if (this.request.cause() != null) {
                    throw new IOException(this.request.cause());
                }
                if (Context.isOnEventLoopThread()) {
                    throw new IllegalStateException("Attempting a blocking write on io thread");
                }
                if (!drainHandlerRegistered) {
                    drainHandlerRegistered = true;
                    final Handler<Void> drainHandler = new Handler<Void>() {
                        @Override
                        public void handle(Void event) {
                            if (waitingForDrain) {
                                lock.lock();
                                try {
                                    requestWriteable.signal();
                                } finally {
                                    lock.unlock();
                                }
                            }
                        }
                    };
                    request.drainHandler(drainHandler);
                }
                try {
                    waitingForDrain = true;
                    requestWriteable.await(receiveTimeout(), TimeUnit.MILLISECONDS);
                } finally {
                    waitingForDrain = false;
                }
            }
        }

        /**
         * Computes the timeout for receive related operations based on {@link #receiveTimeoutDeadline}
         *
         * @return the timeout in milliseconds for response related operations
         * @throws SocketTimeoutException if {@link #receiveTimeoutDeadline} was missed already
         */
        long receiveTimeout() throws SocketTimeoutException {
            final long timeout = receiveTimeoutDeadline - System.currentTimeMillis();
            if (timeout <= 0) {
                /* Too late already */
                throw new SocketTimeoutException("Timeout waiting for HTTP response from " + url);
            }
            return timeout;
        }
    }

    static record ResponseEvent(HttpClientResponse response, InputStream responseBodyInputStream) {
    }

    /**
     * A slimmed-down variant of {@link AsyncResult}
     *
     * @param <T> the type of {@link #result}
     */
    static record Result<T>(T result, Throwable cause) {

        static <T> Result<T> failure(Throwable cause) {
            return new Result<T>(null, cause);
        }

        boolean succeeded() {
            return this.cause == null;
        }
    }

    static class ResponseHandler implements IOEHandler<ResponseEvent> {
        private static final Collection<Integer> DEFAULT_SERVICE_NOT_AVAILABLE_ON_HTTP_STATUS_CODES = Arrays.asList(404, 429,
                503);

        private final URI url;
        private final Message outMessage;
        private final Cookies cookies;
        private final MessageObserver incomingObserver;

        public ResponseHandler(URI url, Message outMessage, Cookies cookies, MessageObserver incomingObserver) {
            super();
            this.url = url;
            this.outMessage = outMessage;
            this.cookies = cookies;
            this.incomingObserver = incomingObserver;
        }

        @Override
        public void handle(ResponseEvent responseEvent) throws IOException {
            final HttpClientResponse response = responseEvent.response;
            final Exchange exchange = outMessage.getExchange();
            final int responseCode = doProcessResponseCode(url, response, exchange, outMessage);

            InputStream in = null;
            // oneway or decoupled twoway calls may expect HTTP 202 with no content

            Message inMessage = new MessageImpl();
            inMessage.setExchange(exchange);
            updateResponseHeaders(response, inMessage, cookies);
            inMessage.put(Message.RESPONSE_CODE, responseCode);
            if (MessageUtils.getContextualBoolean(outMessage, SET_HTTP_RESPONSE_MESSAGE, false)) {
                inMessage.put(HTTP_RESPONSE_MESSAGE, response.statusMessage());
            }
            propagateConduit(exchange, inMessage);

            if ((!doProcessResponse(outMessage, responseCode)
                    || HttpURLConnection.HTTP_ACCEPTED == responseCode)
                    && MessageUtils.getContextualBoolean(outMessage,
                            Message.PROCESS_202_RESPONSE_ONEWAY_OR_PARTIAL, true)) {
                in = getPartialResponse(response, responseEvent.responseBodyInputStream);
                if (in == null
                        || !MessageUtils.getContextualBoolean(outMessage, Message.PROCESS_ONEWAY_RESPONSE, false)) {
                    // oneway operation or decoupled MEP without
                    // partial response
                    if (isOneway(exchange) && responseCode > 300) {
                        final String msg = "HTTP response '" + responseCode + ": "
                                + response.statusMessage() + "' when communicating with " + url.toString();
                        throw new VertxHttpException(msg);
                    }
                    // REVISIT move the decoupled destination property name into api
                    Endpoint ep = exchange.getEndpoint();
                    if (null != ep && null != ep.getEndpointInfo() && null == ep.getEndpointInfo()
                            .getProperty("org.apache.cxf.ws.addressing.MAPAggregator.decoupledDestination")) {
                        // remove callback so that it won't be invoked twice
                        ClientCallback cc = exchange.remove(ClientCallback.class);
                        if (null != cc) {
                            cc.handleResponse(null, null);
                        }
                    }
                    exchange.put("IN_CHAIN_COMPLETE", Boolean.TRUE);

                    exchange.setInMessage(inMessage);
                    if (MessageUtils.getContextualBoolean(outMessage,
                            Message.PROPAGATE_202_RESPONSE_ONEWAY_OR_PARTIAL, false)) {
                        incomingObserver.onMessage(inMessage);
                    }

                    return;
                }
            } else {
                // not going to be resending or anything, clear out the stuff in the out message
                // to free memory
                outMessage.removeContent(OutputStream.class);
                // if (cachingForRetransmission && cachedStream != null) {
                // cachedStream.close();
                // }
                // cachedStream = null;
            }

            final String charset = HttpHeaderHelper.findCharset((String) inMessage.get(Message.CONTENT_TYPE));
            final String normalizedEncoding = HttpHeaderHelper.mapCharset(charset);
            if (normalizedEncoding == null) {
                final String m = new org.apache.cxf.common.i18n.Message("INVALID_ENCODING_MSG",
                        LOG, charset).toString();
                throw new VertxHttpException(m);
            }
            inMessage.put(Message.ENCODING, normalizedEncoding);
            if (in == null) {
                in = responseEvent.responseBodyInputStream;
            }
            inMessage.setContent(InputStream.class, in);

            incomingObserver.onMessage(inMessage);

        }

        static int doProcessResponseCode(URI uri, HttpClientResponse response, Exchange exchange, Message outMessage)
                throws IOException {
            final int rc = response.statusCode();
            if (rc == -1) {
                LOG.warning("HTTP Response code appears to be corrupted");
            }
            if (exchange != null) {
                exchange.put(Message.RESPONSE_CODE, rc);
                final Collection<Integer> serviceNotAvailableOnHttpStatusCodes = MessageUtils
                        .getContextualIntegers(outMessage, SERVICE_NOT_AVAILABLE_ON_HTTP_STATUS_CODES,
                                DEFAULT_SERVICE_NOT_AVAILABLE_ON_HTTP_STATUS_CODES);
                if (serviceNotAvailableOnHttpStatusCodes.contains(rc)) {
                    exchange.put("org.apache.cxf.transport.service_not_available", true);
                }
            }

            // "org.apache.cxf.transport.no_io_exceptions" property should be set in case the exceptions
            // should not be handled here; for example jax rs uses this

            // "org.apache.cxf.transport.process_fault_on_http_400" property should be set in case a
            // soap fault because of a HTTP 400 should be returned back to the client (SOAP 1.2 spec)

            if (rc >= 400 && rc != 500
                    && !MessageUtils.getContextualBoolean(outMessage, NO_IO_EXCEPTIONS)
                    && (rc > 400 || !MessageUtils.getContextualBoolean(outMessage, PROCESS_FAULT_ON_HTTP_400))) {

                throw new HTTPException(rc, response.statusMessage(), uri.toURL());
            }
            return rc;
        }

        static void updateResponseHeaders(HttpClientResponse response, Message inMessage, Cookies cookies) {
            Headers h = new Headers(inMessage);
            inMessage.put(Message.CONTENT_TYPE, readHeaders(response, h));
            cookies.readFromHeaders(h);
        }

        static InputStream getPartialResponse(HttpClientResponse response, InputStream responseBodyInputStream) {
            InputStream in = null;
            int responseCode = response.statusCode();
            if (responseCode == 202 || responseCode == 200) {

                final MultiMap headers = response.headers();
                final String rawContentLength = headers.get(HttpHeaderHelper.CONTENT_LENGTH);
                int contentLength = 0;
                if (rawContentLength != null) {
                    try {
                        contentLength = Integer.parseInt(rawContentLength);
                    } catch (NumberFormatException e) {
                        LOG.fine("Could not parse Content-Length value " + rawContentLength);
                    }
                }
                final String transferEncoding = headers.get(HttpHeaderHelper.TRANSFER_ENCODING);
                boolean isChunked = transferEncoding != null && HttpHeaderHelper.CHUNKED.equalsIgnoreCase(transferEncoding);
                final String connection = headers.get(HttpHeaderHelper.CONNECTION);
                boolean isEofTerminated = connection != null && HttpHeaderHelper.CLOSE.equalsIgnoreCase(connection);
                if (contentLength > 0) {
                    in = responseBodyInputStream;
                } else if (isChunked || isEofTerminated) {
                    // ensure chunked or EOF-terminated response is non-empty
                    try {
                        PushbackInputStream pin = new PushbackInputStream(responseBodyInputStream);
                        int c = pin.read();
                        if (c != -1) {
                            pin.unread((byte) c);
                            in = pin;
                        }
                    } catch (IOException ioe) {
                        // ignore
                    }
                }
            }
            return in;
        }

        static String readHeaders(HttpClientResponse response, Headers h) {

            final Map<String, List<String>> dest = h.headerMap();
            String ct = null;
            for (Entry<String, String> en : response.headers().entries()) {
                final String key = en.getKey();
                dest.computeIfAbsent(key, k -> new ArrayList<>()).add(en.getValue());
                if (Message.CONTENT_TYPE.equalsIgnoreCase(key)) {
                    ct = en.getValue();
                }
            }
            return ct;
        }

        static void propagateConduit(Exchange exchange, Message in) {
            if (exchange != null) {
                Message out = exchange.getOutMessage();
                if (out != null) {
                    in.put(Conduit.class, out.get(Conduit.class));
                }
            }
        }

        static boolean doProcessResponse(Message message, int responseCode) {
            // 1. Not oneWay
            if (!isOneway(message.getExchange())) {
                return true;
            }
            // 2. Robust OneWays could have a fault
            return responseCode == 500 && MessageUtils.getContextualBoolean(message, Message.ROBUST_ONEWAY, false);
        }

        /**
         * This predicate returns true if the exchange indicates
         * a oneway MEP.
         *
         * @param exchange The exchange in question
         */
        static boolean isOneway(Exchange exchange) {
            return exchange != null && exchange.isOneWay();
        }

    }

    static class ExceptionAwarePipedInputStream extends PipedInputStream {
        private IOException exception;
        private final Object lock = new Object();

        public ExceptionAwarePipedInputStream(PipedOutputStream pipedOutputStream) throws IOException {
            super(pipedOutputStream);
        }

        public void setException(IOException exception) {
            synchronized (lock) {
                if (this.exception == null) {
                    /* Ignore subsequent exceptions */
                    this.exception = exception;
                }
            }
        }

        @Override
        public int read() throws IOException {
            synchronized (lock) {
                if (exception != null) {
                    throw exception;
                }
            }
            return super.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            synchronized (lock) {
                if (exception != null) {
                    throw exception;
                }
            }
            return super.read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            synchronized (lock) {
                if (exception != null) {
                    throw exception;
                }
            }
            super.close();
        }

    }

    public interface IOEHandler<E> {

        /**
         * Something has happened, so handle it.
         *
         * @param event the event to handle
         */
        void handle(E event) throws IOException;
    }
}
