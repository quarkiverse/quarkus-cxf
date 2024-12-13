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

import static io.vertx.core.http.HttpHeaders.CONTENT_LENGTH;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.*;
import java.net.Proxy.Type;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.ClientCallback;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.http.*;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.version.Version;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

import io.quarkiverse.cxf.QuarkusTLSClientParameters;
import io.quarkiverse.cxf.vertx.http.client.HttpClientPool.ClientSpec;
import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit.RequestBodyEvent.RequestBodyEventType;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.logging.Log;
import io.quarkus.runtime.BlockingOperationControl;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.http.impl.HttpUtils;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import io.vertx.core.streams.WriteStream;

/**
 */
public class VertxHttpClientHTTPConduit extends HTTPConduit {
    private static final Logger log = Logger.getLogger(VertxHttpClientHTTPConduit.class);
    public static final String USE_ASYNC = "use.async.http.conduit";
    public static final String ENABLE_HTTP2 = "org.apache.cxf.transports.http2.enabled";
    public static final String AUTO_REDIRECT_MAX_SAME_URI_COUNT = "http.redirect.max.same.uri.count";
    private static final String AUTO_REDIRECT_SAME_HOST_ONLY = "http.redirect.same.host.only";
    private static final String AUTO_REDIRECT_ALLOWED_URI = "http.redirect.allowed.uri";
    public static final String AUTO_REDIRECT_ALLOW_REL_URI = "http.redirect.relative.uri";

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

        final UseAsyncPolicy useAsync = UseAsyncPolicy.of(message.getContextualProperty(USE_ASYNC));
        final boolean isAsync = useAsync.isAsync(message);
        message.put(USE_ASYNC, isAsync);

        if (!isAsync && !BlockingOperationControl.isBlockingAllowed()) {
            throw new IllegalStateException("You have attempted to perform a blocking operation on an IO thread."
                    + " This is not allowed, as blocking the IO thread will cause major performance issues with your application."
                    + " You need to offload the blocking CXF client call to a worker thread,"
                    + " e.g. by using the @io.smallrye.common.annotation.Blocking annotation on a caller method"
                    + " where it is supported by the underlying Quarkus extension, such as quarkus-rest, quarkus-vertx,"
                    + " quarkus-reactive-routes, quarkus-grpc, quarkus-messaging-* and possibly others.");
        }

        final HttpVersion version = getVersion(message, csPolicy);
        final boolean isHttps = "https".equals(uri.getScheme());
        final QuarkusTLSClientParameters clientParameters;
        if (isHttps) {
            clientParameters = findTLSClientParameters(message);
            validateClientParameters(clientParameters);
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
            /* Port was set explicitly */
            requestOptions.setPort(uri.getPort());
        } else if (isHttps) {
            requestOptions.setPort(443);
        } else {
            requestOptions.setPort(80);
        }

        final RequestContext requestContext = new RequestContext(
                uri,
                requestOptions,
                clientParameters != null
                        ? new ClientSpec(version, clientParameters.getTlsConfigurationName(),
                                clientParameters.getTlsConfiguration())
                        : new ClientSpec(version, null, null),
                determineReceiveTimeout(message, csPolicy),
                isAsync,
                csPolicy.getMaxRetransmits(),
                csPolicy.isAutoRedirect());
        message.put(RequestContext.class, requestContext);

    }

    private static void validateClientParameters(QuarkusTLSClientParameters clientParameters) {
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
                getConduitName(),
                message,
                requestContext.uri,
                cookies,
                userAgent,
                httpClientPool,
                requestContext.requestOptions,
                requestContext.clientSpec,
                requestContext.receiveTimeoutMs,
                responseHandler,
                requestContext.async,
                requestContext.autoRedirect, // we do not support authorizationRetransmit yet possibleRetransmit,
                requestContext.maxRetransmits,
                0);
        return new RequestBodyOutputStream(chunkThreshold, requestBodyHandler);
    }

    static HttpVersion getVersion(Message message, HTTPClientPolicy csPolicy) {
        String verc = (String) message.getContextualProperty(FORCE_HTTP_VERSION);
        final Object enableHttp2 = message.getContextualProperty(ENABLE_HTTP2);
        if (verc == null && enableHttp2 != null) {
            csPolicy.setVersion("2");
        }
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
            long receiveTimeoutMs,
            boolean async,
            int maxRetransmits,
            boolean autoRedirect) {
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
        private final Cookies cookies;
        private final String userAgent;
        private final HttpClientPool clientPool;
        private final RequestOptions requestOptions;
        private final ClientSpec clientSpec;

        /** Read an written only from the producer thread */
        private boolean firstEvent = true;
        /**
         * Read from the producer thread, written from the event loop. Protected by {@link #lock} {@link #requestReady}
         * {@link Condition}
         */
        private Result<HttpClientRequest> request;

        /* Retransmit settings, read/written from the event loop */
        private final boolean possibleRetransmit;
        private List<Buffer> bodyRecorder;
        private List<URI> redirects;
        private final int maxRetransmits;
        private int performedRetransmits;
        private final String conduitName;

        /* Locks and conditions */
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition requestReady = lock.newCondition();
        private final Condition requestWriteable = lock.newCondition();

        /* Backpressure control when writing the request body */
        private boolean drainHandlerRegistered;
        private boolean waitingForDrain;
        private Mode mode;

        public RequestBodyHandler(
                String conduitName,
                Message outMessage,
                URI url,
                Cookies cookies,
                String userAgent,
                HttpClientPool clientPool,
                RequestOptions requestOptions,
                ClientSpec clientSpec,
                long receiveTimeoutMs,
                IOEHandler<ResponseEvent> responseHandler,
                boolean isAsync,
                boolean possibleRetransmit,
                int maxRetransmits,
                int performedRetransmits) {
            super();
            this.conduitName = conduitName;
            this.outMessage = outMessage;
            this.url = url;
            this.cookies = cookies;
            this.userAgent = userAgent;
            this.clientPool = clientPool;
            this.requestOptions = requestOptions;
            this.clientSpec = clientSpec;

            final long deadline = System.currentTimeMillis() + receiveTimeoutMs;
            this.mode = isAsync
                    ? new Mode.Async(url, deadline, responseHandler, outMessage)
                    : new Mode.Sync(url, deadline, responseHandler, lock);

            this.possibleRetransmit = possibleRetransmit;
            this.maxRetransmits = maxRetransmits;
            this.performedRetransmits = performedRetransmits;
        }

        @Override
        public void handle(RequestBodyEvent event) throws IOException {

            final Buffer buffer = event.buffer();
            if (firstEvent) {
                firstEvent = false;
                if (possibleRetransmit) {
                    final List<Buffer> recorder = bodyRecorder = new ArrayList<>();
                    recorder.add(buffer.slice());
                    final List<URI> redirs = redirects = new ArrayList<>();
                    redirs.add(url);
                }

                final HttpClient client = clientPool.getClient(clientSpec);
                if (event.eventType() == RequestBodyEventType.COMPLETE_BODY && requestHasBody(requestOptions.getMethod())) {
                    requestOptions.putHeader(CONTENT_LENGTH, String.valueOf(buffer.length()));
                }

                setProtocolHeaders(outMessage, requestOptions, userAgent);

                client.request(requestOptions)
                        .onSuccess(req -> {
                            switch (event.eventType()) {
                                case NON_FINAL_CHUNK: {
                                    req
                                            .setChunked(true)
                                            .write(buffer)
                                            .onFailure(t -> mode.responseFailed(t, true));

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
                                    finishRequest(req, buffer);
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
                                mode.responseFailed(t, false);
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
                        mode.awaitResponse();
                        break;
                    }
                    default:
                        throw new IllegalArgumentException(
                                "Unexpected " + RequestBodyEventType.class.getName() + ": " + event.eventType());

                }

            } else {
                /* Non-first event */
                if (bodyRecorder != null) {
                    bodyRecorder.add(buffer.slice());
                }
                final HttpClientRequest req = awaitRequest();
                switch (event.eventType()) {
                    case NON_FINAL_CHUNK: {
                        req
                                .write(buffer)
                                .onFailure(RequestBodyHandler.this::failResponse);
                        break;
                    }
                    case FINAL_CHUNK:
                    case COMPLETE_BODY: {
                        finishRequest(req, buffer);
                        mode.awaitResponse();
                        break;
                    }
                    default:
                        throw new IllegalArgumentException(
                                "Unexpected " + RequestBodyEventType.class.getName() + ": " + event.eventType());

                }
            }
        }

        @SuppressWarnings("resource")
        void finishRequest(HttpClientRequest req, Buffer buffer) {

            req.response()
                    .onComplete(ar -> {
                        final InputStreamWriteStream sink = new InputStreamWriteStream(2);
                        final HttpClientResponse response = ar.result();
                        if (ar.succeeded()) {

                            /* need to retransmit? */
                            final boolean isRedirect = isRedirect(response.statusCode());
                            if (possibleRetransmit
                                    && (maxRetransmits < 0 || performedRetransmits < maxRetransmits)
                                    && isRedirect) {
                                performedRetransmits++;
                                ResponseHandler.updateResponseHeaders(response, outMessage, cookies);
                                final String loc = response.getHeader("Location");
                                try {

                                    if (loc != null && !loc.startsWith("http")
                                            && !MessageUtils.getContextualBoolean(outMessage, AUTO_REDIRECT_ALLOW_REL_URI)) {
                                        throw new IOException(
                                                "Relative Redirect detected on Conduit '" + conduitName + "' on '" + loc + "'."
                                                        + " You may want to set quarkus.cxf.client.\"client-name\".redirect-relative-uri = true,"
                                                        + " where \"client-name\" is the name of your client in application.properties");
                                    }
                                    final URI previousUri = redirects.get(redirects.size() - 1);
                                    final URI newUri = HttpUtils.resolveURIReference(previousUri, loc);
                                    detectRedirectLoop(conduitName, redirects, newUri, outMessage);
                                    redirects.add(newUri);
                                    checkAllowedRedirectUri(conduitName, previousUri, newUri, outMessage);
                                    redirectRetransmit(newUri);
                                } catch (IOException e) {
                                    sink.setException((IOException) e);
                                    mode.responseReady(new Result<>(new ResponseEvent(response, sink), ar.cause()));
                                } catch (URISyntaxException e) {
                                    sink.setException(new IOException(
                                            "Could not resolve redirect Location " + loc + " relative to " + url, e));
                                    mode.responseReady(new Result<>(new ResponseEvent(response, sink), ar.cause()));
                                } catch (Exception e) {
                                    sink.setException(new IOException(e));
                                    mode.responseReady(new Result<>(new ResponseEvent(response, sink), ar.cause()));
                                }
                                return;
                            } else {
                                if (!possibleRetransmit && isRedirect) {
                                    Log.warnf(
                                            "Received redirection status %d from %s, but following redirects is not"
                                                    + " enabled for this CXF client. You may want to set"
                                                    + " quarkus.cxf.client.\"client-name\".auto-redirect = true,"
                                                    + " where \"client-name\" is the name of your client in application.properties",
                                            response.statusCode(),
                                            url);
                                }
                                if (possibleRetransmit && isRedirect && maxRetransmits >= 0
                                        && maxRetransmits <= performedRetransmits) {
                                    Log.warnf(
                                            "Received redirection status %d from %s, but already performed maximum"
                                                    + " number %d of allowed retransmits for this exchange; you may want to"
                                                    + " increase quarkus.cxf.client.\"client-name\".max-retransmits in application.properties",
                                            response.statusCode(),
                                            redirects.get(redirects.size() - 1),
                                            maxRetransmits);
                                }
                                /* No retransmit */
                                /* Pass the body back to CXF */
                                response.pipeTo(sink);
                            }
                        } else {
                            if (ar.cause() instanceof IOException) {
                                sink.setException((IOException) ar.cause());
                            } else {
                                sink.setException(new IOException(ar.cause()));
                            }
                        }
                        mode.responseReady(new Result<>(new ResponseEvent(response, sink), ar.cause()));
                    });
            req
                    .end(buffer)
                    .onFailure(t -> mode.responseFailed(t, true));

        }

        void redirectRetransmit(URI newURL) throws IOException {
            if (Log.isDebugEnabled()) {
                final int i = redirects.size() - 2;
                final String previousUrl = i >= 0 ? redirects.get(i).toString() : "null";
                Log.infof("Redirect retransmit from %s to %s", previousUrl, newURL);
            }
            boolean ssl;
            int port = newURL.getPort();
            String protocol = newURL.getScheme();
            char chend = protocol.charAt(protocol.length() - 1);
            if (chend == 'p') {
                ssl = false;
                if (port == -1) {
                    port = 80;
                }
            } else if (chend == 's') {
                ssl = true;
                if (port == -1) {
                    port = 443;
                }
            } else {
                throw new IllegalStateException("Unexpected URI scheme " + protocol + "; expected 'http' or 'https'");
            }
            String requestURI = newURL.getPath();
            if (requestURI == null || requestURI.isEmpty()) {
                requestURI = "/";
            }
            String query = newURL.getQuery();
            if (query != null) {
                requestURI += "?" + query;
            }
            RequestOptions options = new RequestOptions(requestOptions);
            options.setHost(newURL.getHost());
            options.setPort(port);
            options.setSsl(ssl);
            options.setURI(requestURI);

            final List<Buffer> body = bodyRecorder;
            final int last = body.size() - 1;
            if (last == 0 && requestHasBody(options.getMethod())) {
                /* Only one buffer recorded */
                requestOptions.putHeader(CONTENT_LENGTH, String.valueOf(body.get(0).length()));
            } else if (last == -1 && requestHasBody(options.getMethod())) {
                /* No buffer recorded */
                requestOptions.putHeader(CONTENT_LENGTH, "0");
            } else {
                options.removeHeader(CONTENT_LENGTH);
            }

            final HttpClient client = clientPool.getClient(clientSpec);

            // Should not be necessary, because we copy from the original requestOptions
            // setProtocolHeaders(outMessage, options, userAgent);

            client.request(options)
                    .onSuccess(req -> {
                        if (last == 0) {
                            /* Single buffer recorded */
                            finishRequest(req, body.get(0).slice());
                        } else if (last == -1) {
                            /* Empty body */
                            finishRequest(req, Buffer.buffer());
                        } else {
                            /* Multiple buffers recorded */
                            req.setChunked(true);
                            for (int i = 0; i <= last; i++) {
                                if (i == last) {
                                    finishRequest(req, body.get(i).slice());
                                } else {
                                    req
                                            .write(body.get(i).slice())
                                            .onFailure(t -> mode.responseFailed(t, true));
                                }
                            }
                        }
                    })
                    .onFailure(t -> {
                        lock.lock();
                        try {
                            request = Result.failure(t);
                            requestReady.signal();

                            /* Fail also the response so that awaitResponse() fails rather than waiting forever */
                            mode.responseFailed(t, false);
                        } finally {
                            lock.unlock();
                        }
                    });
        }

        private static boolean isRedirect(int statusCode) {
            return statusCode >= 301 // fast return for statusCode == 200 that we'll see mostly
                    && (statusCode == 302 || statusCode == 301 || statusCode == 303 || statusCode == 307);
        }

        private static void detectRedirectLoop(String conduitName,
                List<URI> redirects,
                URI newURL,
                Message message) throws IOException {
            if (redirects.contains(newURL)) {
                final Integer maxSameURICount = PropertyUtils.getInteger(message, AUTO_REDIRECT_MAX_SAME_URI_COUNT);
                if (maxSameURICount != null
                        && redirects.stream().filter(newURL::equals).count() > maxSameURICount.longValue()) {
                    final String msg = "Redirect loop detected on Conduit '"
                            + conduitName + "' (with http.redirect.max.same.uri.count = " + maxSameURICount + "): "
                            + redirects.stream().map(URI::toString).collect(Collectors.joining(" -> ")) + " -> " + newURL
                            + ". You may want to increase quarkus.cxf.client.\"client-name\".max-retransmits in application.properties"
                            + " where \"client-name\" is the name of your client in application.properties";
                    throw new IOException(msg);
                } else if (maxSameURICount == null) {
                    final String msg = "Redirect loop detected on Conduit '" + conduitName
                            + ". You may want to set quarkus.cxf.client.\"client-name\".max-retransmits in application.properties"
                            + " where \"client-name\" is the name of your client in application.properties";
                    throw new IOException(msg);
                }
            }
        }

        private static void checkAllowedRedirectUri(String conduitName,
                URI lastUri,
                URI newUri,
                Message message) throws IOException {
            if (MessageUtils.getContextualBoolean(message, AUTO_REDIRECT_SAME_HOST_ONLY)) {

                // This can be further restricted to make sure newURL completely contains lastURL
                // though making sure the same HTTP scheme and host are preserved should be enough

                if (!newUri.getScheme().equals(lastUri.getScheme())
                        || !newUri.getHost().equals(lastUri.getHost())) {
                    String msg = "Different HTTP Scheme or Host Redirect detected on Conduit '"
                            + conduitName + "' on '" + newUri + "'";
                    LOG.log(Level.INFO, msg);
                    throw new IOException(msg);
                }
            }

            String allowedRedirectURI = (String) message.getContextualProperty(AUTO_REDIRECT_ALLOWED_URI);
            if (allowedRedirectURI != null && !newUri.toString().startsWith(allowedRedirectURI)) {
                String msg = "Forbidden Redirect URI " + newUri + "detected on Conduit '" + conduitName;
                LOG.log(Level.INFO, msg);
                throw new IOException(msg);
            }
        }

        void failResponse(Throwable t) {
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

        void awaitWriteable(HttpClientRequest request) throws IOException, InterruptedException {
            // assert lock.isHeldByCurrentThread();
            while (request.writeQueueFull()) {
                if (this.request.cause() != null) {
                    throw new IOException(this.request.cause());
                }
                if (!BlockingOperationControl.isBlockingAllowed()) {
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
                    if (!requestWriteable.await(mode.receiveTimeout(), TimeUnit.MILLISECONDS)) {
                        throw new SocketTimeoutException("Timeout waiting for sending HTTP headers to " + url);
                    }
                } finally {
                    waitingForDrain = false;
                }
            }
        }

        static abstract class Mode {
            /** Time in epoch milliseconds when the response should be fully received */
            private final long receiveTimeoutDeadline;
            protected final URI url;
            protected final IOEHandler<ResponseEvent> responseHandler;

            Mode(URI url, long receiveTimeoutDeadline, IOEHandler<ResponseEvent> responseHandler) {
                this.url = url;
                this.receiveTimeoutDeadline = receiveTimeoutDeadline;
                this.responseHandler = responseHandler;
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

            protected abstract void responseFailed(Throwable t, boolean lockIfNeeded);

            protected abstract void responseReady(Result<ResponseEvent> response);

            protected abstract void awaitResponse() throws IOException;

            static class Sync extends Mode {
                private final ReentrantLock lock;
                private final Condition responseReceived;
                /**
                 * Read from the producer thread, written from the event loop. Protected by {@link #lock}
                 * {@link #responseReceived}
                 * {@link Condition}
                 */
                private Result<ResponseEvent> response;

                Sync(URI url, long receiveTimeoutDeadline, IOEHandler<ResponseEvent> responseHandler, ReentrantLock lock) {
                    super(url, receiveTimeoutDeadline, responseHandler);
                    this.lock = lock;
                    this.responseReceived = lock.newCondition();
                }

                @Override
                protected void responseFailed(Throwable t, boolean lockIfNeeded) {
                    if (lockIfNeeded) {
                        lock.lock();
                        try {
                            response = Result.failure(t);
                            responseReceived.signal();
                        } finally {
                            lock.unlock();
                        }
                    } else {
                        // assert lock.isHeldByCurrentThread();
                        response = Result.failure(t);
                        responseReceived.signal();
                    }
                }

                @Override
                protected void responseReady(Result<ResponseEvent> response) {
                    lock.lock();
                    try {
                        this.response = response;
                        responseReceived.signal();
                    } finally {
                        lock.unlock();
                    }
                }

                @Override
                protected void awaitResponse() throws IOException {
                    responseHandler.handle(awaitResponseInternal());
                }

                ResponseEvent awaitResponseInternal() throws IOException {
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

            }

            static class Async extends Mode {
                private final Message outMessage;

                Async(URI url, long receiveTimeoutDeadline, IOEHandler<ResponseEvent> responseHandler, Message outMessage) {
                    super(url, receiveTimeoutDeadline, responseHandler);
                    this.outMessage = outMessage;
                }

                @Override
                protected void responseFailed(Throwable t, boolean lockIfNeeded) {
                    // dispatch on worker thread
                    responseReady(Result.failure(t));
                }

                protected void responseFailedOnWorkerThread(Throwable t) {
                    // on worker thread already
                    ((PhaseInterceptorChain) outMessage.getInterceptorChain()).abort();
                    outMessage.setContent(Exception.class, t);
                    if (t instanceof Exception) {
                        outMessage.put(Exception.class, (Exception) t);
                    } else {
                        // FIXME: log this special case
                    }
                    ((PhaseInterceptorChain) outMessage.getInterceptorChain()).unwind(outMessage);
                    MessageObserver mo = outMessage.getInterceptorChain().getFaultObserver();
                    if (mo == null) {
                        mo = outMessage.getExchange().get(MessageObserver.class);
                    }
                    mo.onMessage(outMessage);
                }

                @Override
                protected void responseReady(Result<ResponseEvent> response) {
                    // dispatch on worker thread
                    final InstanceHandle<ManagedExecutor> managedExecutorInst = Arc.container().instance(ManagedExecutor.class);
                    if (!managedExecutorInst.isAvailable()) {
                        throw new IllegalStateException(ManagedExecutor.class.getName() + " not available in Arc");
                    }
                    managedExecutorInst.get().execute(() -> {
                        if (response.succeeded()) {
                            try {
                                responseHandler.handle(response.result());
                            } catch (Throwable e) {
                                responseFailedOnWorkerThread(e);
                            }
                        } else {
                            responseFailedOnWorkerThread(response.cause());
                        }
                    });
                }

                @Override
                protected void awaitResponse() throws IOException {
                    /* Nothing to do in async mode because we dispatch the response via responseReady */
                }

            }
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
            final URI uri = URI.create(response.request().absoluteURI());
            final int responseCode = doProcessResponseCode(uri, response, exchange, outMessage);

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
                throw new VertxHttpException("Invalid character set " + charset + " in request");
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
                        log.debug("Could not parse Content-Length value " + rawContentLength);
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

    static class InputStreamWriteStream extends InputStream implements WriteStream<Buffer> {

        private static final Buffer END = new DummyBuffer();

        private final Queue<Buffer> queue;
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition queueChange = lock.newCondition();

        /** Written from event loop, read from the consumer worker thread */
        private volatile Handler<Void> drainHandler;
        private volatile IOException exception;
        private int maxQueueSize; // volatile not needed as we assume the value stays stable after being set on init

        /** Read and written from the from the consumer worker thread */
        private Buffer readBuffer;
        private int readPosition = 0;

        public InputStreamWriteStream(int queueSize) {
            setWriteQueueMaxSize(queueSize);
            this.queue = new ArrayDeque<>(queueSize);
        }

        @Override
        public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Future<Void> write(Buffer data) {
            final Promise<Void> promise = Promise.promise();
            write(data, promise);
            return promise.future();
        }

        @Override
        public void write(Buffer data, Handler<AsyncResult<Void>> handler) {
            try {
                final ReentrantLock lock = this.lock;
                lock.lock();
                try {
                    queue.offer(data);
                    // Log.infof("Adding buffer %d with size %d bytes; queue size after %d",
                    // System.identityHashCode(data),
                    // data.length(), queue.size());
                    queueChange.signal();
                } finally {
                    lock.unlock();
                }
                handler.handle(Future.succeededFuture());
            } catch (Throwable e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                if (this.exception == null) {
                    this.exception = e instanceof IOException ? (IOException) e : new IOException(e);
                }
                handler.handle(Future.failedFuture(e));
            }
        }

        @Override
        public void end(Handler<AsyncResult<Void>> handler) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                queue.offer(END);
                // Log.info("Adding final buffer");
                queueChange.signal();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public WriteStream<Buffer> setWriteQueueMaxSize(int maxSize) {
            if (maxSize < 1) {
                throw new IllegalArgumentException("maxSize must be >= 1");
            }
            this.maxQueueSize = maxSize;
            return this;
        }

        @Override
        public boolean writeQueueFull() {
            // int size = queue.size();
            // Log.infof("Queue %s: %d", (size >= maxQueueSize ? "full" : "not full"), size);
            return queue.size() >= maxQueueSize;
        }

        @Override
        public WriteStream<Buffer> drainHandler(Handler<Void> handler) {
            this.drainHandler = handler;
            return this;
        }

        @Override
        public int read() throws IOException {
            // Log.infof("> reading 1 byte");
            final IOException e;
            if ((e = exception) != null) {
                throw e;
            }

            final Buffer rb = takeBuffer(true);
            return rb != null ? (rb.getByte(readPosition++) & 0xFF) : -1;
        }

        @Override
        public int read(byte b[], final int off, int len) throws IOException {
            final IOException e;
            if ((e = exception) != null) {
                throw e;
            }
            // Log.infof("Ready to read up to %d bytes", len);

            Buffer rb = takeBuffer(true);
            if (rb == null) {
                return -1;
            }
            int rbLen = rb.length();
            int readable = rbLen - readPosition;
            // Log.infof("Readable %d bytes", readable);

            int result;
            if (readable >= len) {
                readable = len;
                // Log.infof("Downsized readable to %d bytes", readable);
                rb.getBytes(readPosition, readPosition + readable, b, off);
                // Log.infof("After read 1: %s", new String(b, off, readable, StandardCharsets.UTF_8));
                readPosition += readable;
                // Log.infof("readPosition now at %d", readPosition);
                if (readPosition >= rbLen) {
                    /* Nothing more to read from this buffer, so dereference it so that it can be GC's earlier; */
                    // Log.infof("Buffer read out completely");
                    if (readBuffer != END) {
                        readBuffer = null; // deref. so that it can be GC's earlier;
                    }
                }
                result = readable;
            } else {
                /*
                 * readable < len so we read out the current buffer completely and we try the subsequent ones if
                 * available
                 */
                rb.getBytes(readPosition, readPosition + readable, b, off);
                // Log.infof("Read out current buffer %d completely: %s", System.identityHashCode(rb),
                // new String(b, off, readable, StandardCharsets.UTF_8));
                readPosition += readable;
                // assert readPosition == rbLen;
                len -= readable;
                int off2 = off + readable;

                result = readable;
                /* check whether we get more buffers */
                while (len > 0 && (rb = takeBuffer(false)) != null) {
                    rbLen = rb.length();
                    readable = rbLen - readPosition;
                    if (readable > len) {
                        readable = len;
                        // Log.infof("Downsized readable to %d bytes", readable);
                    }
                    rb.getBytes(readPosition, readPosition + readable, b, off2);
                    // Log.infof("Read 2 from buffer %d %d to %d: %s", System.identityHashCode(rb), readPosition,
                    // readPosition + readable,
                    // new String(b, off2, readable, StandardCharsets.UTF_8));
                    readPosition += readable;
                    len -= readable;
                    off2 += readable;
                    // Log.infof("readPosition now at %d", readPosition);
                    result += readable;
                }
                if (readPosition == rbLen) {
                    // Log.infof("Buffer read out completely");
                    if (readBuffer != END) {
                        readBuffer = null; // deref. so that it can be GC's earlier;
                    }
                }
                // assert readPosition <= rbLen;
            }
            // Log.infof("> read %d bytes: %s", result, new String(b, off, result, StandardCharsets.UTF_8));
            return result;
        }

        @Override
        public void close() {
            readBuffer = null;
            // assert queueEmpty() : "Queue still has " + queue.size() + " items";
        }

        @Override
        public int available() throws IOException {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                Buffer rb = takeBuffer(false);
                if (rb != null) {
                    int result = rb.length() - readPosition;
                    for (Buffer b : queue) {
                        if (rb != b) {
                            /* Skip the buffer returned by takeBuffer() above */
                            result += b.length();
                        }
                    }
                    return result;
                }
            } finally {
                lock.unlock();
            }
            return 0;
        }

        private Buffer takeBuffer(boolean blockingAwaitBuffer) throws IOException {
            // Log.infof("About to take buffer at queue size %d %s", queue.size(),
            // blockingAwaitBuffer ? "with blocking" : "without blocking");
            Buffer rb = readBuffer;
            if (rb == END) {
                return null;
            }
            // Log.infof("Buffer is null: %s; %d >= %d: %s", rb == null, readPosition, (rb == null ? -1 : rb.length()),
            // rb != null && readPosition >= rb.length());
            if (rb == null || readPosition >= rb.length()) {
                // Log.info("Buffer is null or empty");

                final ReentrantLock lock = this.lock;
                try {
                    lock.lockInterruptibly();
                    if (blockingAwaitBuffer) {
                        while ((readBuffer = rb = queue.poll()) == null) {
                            // Log.infof("Awaiting a buffer at queue size %d", queue.size());
                            queueChange.await();
                        }
                    } else {
                        readBuffer = rb = queue.poll();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException(e);
                } finally {
                    lock.unlock();
                }
                if (rb == END) {
                    return null;
                }
                readPosition = 0;

                final Handler<Void> dh;
                if (!writeQueueFull() && (dh = drainHandler) != null) {
                    dh.handle(null);
                }
            }
            // Log.infof("Taken a %s buffer %d, will read from %d to %d; queue size after: %d",
            // (rb != null ? "valid" : "null"),
            // System.identityHashCode(rb),
            // readPosition, (rb != null ? rb.length() : -1), queue.size());
            return rb;
        }

        public void setException(IOException exception) {
            if (this.exception == null) {
                /* Ignore subsequent exceptions */
                this.exception = exception;
            }
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

    public enum UseAsyncPolicy {
        ALWAYS(true),
        NEVER(false),
        ASYNC_ONLY(false) {
            @Override
            public boolean isAsync(Message message) {
                return !message.getExchange().isSynchronous();
            }
        };

        private final boolean async;

        private UseAsyncPolicy(Boolean async) {
            this.async = async;
        }

        static final Map<Object, UseAsyncPolicy> values = Map.of(
                "ALWAYS", ALWAYS,
                "always", ALWAYS,
                "ASYNC_ONLY", ASYNC_ONLY,
                "async_only", ASYNC_ONLY,
                "NEVER", NEVER,
                "never", NEVER,
                Boolean.TRUE, ALWAYS,
                Boolean.FALSE, NEVER);

        public static UseAsyncPolicy of(Object st) {
            if (st == null) {
                return UseAsyncPolicy.ASYNC_ONLY;
            }
            if (st instanceof UseAsyncPolicy) {
                return (UseAsyncPolicy) st;
            }
            final UseAsyncPolicy result = values.get(st);
            return result != null ? result : ASYNC_ONLY;
        }

        public boolean isAsync(Message message) {
            return async;
        }
    };

}
