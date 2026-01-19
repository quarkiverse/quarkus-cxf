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
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
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
import org.apache.cxf.transport.http.Address;
import org.apache.cxf.transport.http.Cookies;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPException;
import org.apache.cxf.transport.http.Headers;
import org.apache.cxf.transport.http.MessageTrustDecider;
import org.apache.cxf.transport.http.auth.HttpAuthHeader;
import org.apache.cxf.transport.http.auth.HttpAuthSupplier;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.version.Version;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.QuarkusCxfUtils;
import io.quarkiverse.cxf.QuarkusTLSClientParameters;
import io.quarkiverse.cxf.vertx.http.client.BodyRecorder.BodyWriter;
import io.quarkiverse.cxf.vertx.http.client.BodyRecorder.StoredBody;
import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit.RequestBodyEvent.RequestBodyEventType;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.proxy.ProxyConfiguration;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.tls.TlsConfiguration;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.HttpUtils;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.NoStackTraceTimeoutException;
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
    private final CXFClientInfo clientInfo;
    private final ProxyConfiguration proxyConfiguration;

    public VertxHttpClientHTTPConduit(
            CXFClientInfo clientInfo,
            Bus b,
            EndpointInfo ei,
            EndpointReferenceType t,
            HttpClientPool httpClientPool,
            ProxyConfiguration proxyConfiguration)
            throws IOException {
        super(b, ei, t);
        this.clientInfo = clientInfo;
        this.httpClientPool = httpClientPool;
        this.proxyConfiguration = proxyConfiguration;
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

        final boolean blockingAllowed = BlockingOperationControl.isBlockingAllowed();
        if (!isAsync && !blockingAllowed) {
            throw new IllegalStateException(
                    "You have attempted to perform a blocking service method call on Vert.x event loop thread with CXF client "
                            + clientInfo.getConfigKey() + "."
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
                clientInfo,
                uri,
                requestOptions,
                version,
                clientParameters != null ? clientParameters.getTlsConfiguration() : null,
                proxyConfiguration,
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

        final HttpAuthSupplier authSupp = authSupplier;
        final IOEHandler<RequestBodyEvent> requestBodyHandler = new RequestBodyHandler(
                (ContextInternal) httpClientPool.getVertx().getOrCreateContext(),
                requestContext.clientInfo,
                message,
                requestContext.uri,
                cookies,
                userAgent,
                httpClientPool,
                requestContext.requestOptions,
                requestContext.version,
                requestContext.tlsConfiguration,
                requestContext.proxyConfiguration,
                requestContext.receiveTimeoutMs,
                responseHandler,
                requestContext.async,
                requestContext.autoRedirect || (authSupp != null && authSupp.requiresRequestCaching()),
                requestContext.maxRetransmits,
                getAuthorization(),
                authSupp);
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
            CXFClientInfo clientInfo,
            URI uri,
            RequestOptions requestOptions,
            HttpVersion version,
            TlsConfiguration tlsConfiguration,
            ProxyConfiguration proxyConfiguration,
            long receiveTimeoutMs,
            boolean async,
            int maxRetransmits,
            boolean autoRedirect) {
    }

    static record RequestBodyEvent(Buffer buffer, RequestBodyEventType eventType) {
        public enum RequestBodyEventType {
            NON_FINAL_CHUNK(false),
            FINAL_CHUNK(true),
            COMPLETE_BODY(true);

            private final boolean finalChunk;

            private RequestBodyEventType(boolean finalChunk) {
                this.finalChunk = finalChunk;
            }

            public boolean isFinalChunk() {
                return finalChunk;
            }
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
        private final HttpVersion version;
        private final TlsConfiguration tlsConfiguration;
        private final ProxyConfiguration proxyConfiguration;
        private final ContextInternal context;
        private final AuthorizationPolicy authorizationPolicy;
        private final HttpAuthSupplier authSupplier;

        /* Read an written only from the producer thread */
        private boolean firstEvent = true;
        private Future<BodyWriter> bodyWriter;
        private Future<StoredBody> body;

        /*
         * Read from the producer thread, written from the event loop. Protected by {@link #lock} {@link #requestReady}
         * {@link Condition}
         */
        private Result<HttpClientRequest> request;

        /* Retransmit settings, read/written from the event loop */
        private final boolean possibleRetransmit;
        private List<URI> redirects;
        private Set<String> authUris;
        private final int maxRetransmits;
        private final CXFClientInfo clientInfo;

        /* Locks and conditions */
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition requestReady = lock.newCondition();
        private final Condition requestWriteable = lock.newCondition();

        /* Backpressure control when writing the request body */
        private boolean drainHandlerRegistered;
        private boolean waitingForDrain;
        private Mode mode;

        public RequestBodyHandler(
                ContextInternal context,
                CXFClientInfo clientInfo,
                Message outMessage,
                URI url,
                Cookies cookies,
                String userAgent,
                HttpClientPool clientPool,
                RequestOptions requestOptions,
                HttpVersion version,
                TlsConfiguration tlsConfiguration,
                ProxyConfiguration proxyConfiguration,
                long receiveTimeoutMs,
                IOEHandler<ResponseEvent> responseHandler,
                boolean isAsync,
                boolean possibleRetransmit,
                int maxRetransmits,
                AuthorizationPolicy authorizationPolicy,
                HttpAuthSupplier authSupplier) {
            super();
            this.context = context;
            this.clientInfo = clientInfo;
            this.outMessage = outMessage;
            this.url = url;
            this.cookies = cookies;
            this.userAgent = userAgent;
            this.clientPool = clientPool;
            this.requestOptions = requestOptions;
            this.version = version;
            this.tlsConfiguration = tlsConfiguration;
            this.proxyConfiguration = proxyConfiguration;

            this.mode = isAsync
                    ? new Mode.Async(TimeoutSpec.create(receiveTimeoutMs, url), responseHandler, outMessage)
                    : new Mode.Sync(TimeoutSpec.create(receiveTimeoutMs, url), responseHandler, lock);

            this.possibleRetransmit = possibleRetransmit;
            this.maxRetransmits = maxRetransmits;

            this.authorizationPolicy = authorizationPolicy;
            this.authSupplier = authSupplier;
        }

        @Override
        public void handle(RequestBodyEvent event) throws IOException {

            final Buffer buffer = event.buffer();
            final boolean finalChunk = event.eventType().isFinalChunk();
            if (firstEvent) {
                firstEvent = false;
                if (possibleRetransmit) {
                    Future<BodyWriter> bw = BodyRecorder.openWriter(
                            (ContextInternal) clientPool.getVertx().getOrCreateContext(),
                            clientInfo.getRetransmitCache());
                    bw = bw.compose(w -> w.write(buffer.slice()));
                    if (finalChunk) {
                        body = bw.compose(w -> w.close());
                    } else {
                        bodyWriter = bw;
                    }
                    final List<URI> redirs = redirects = new ArrayList<>();
                    redirs.add(url);
                }

                final HttpClient client = clientPool.getClient(clientInfo, version, tlsConfiguration, proxyConfiguration);
                if (event.eventType() == RequestBodyEventType.COMPLETE_BODY && requestHasBody(requestOptions.getMethod())) {
                    requestOptions.putHeader(CONTENT_LENGTH, String.valueOf(buffer.length()));
                }

                setProtocolHeaders(outMessage, requestOptions, userAgent, version);

                client.request(requestOptions)
                        .timeout(mode.timeoutSpec.remainingTimeout(), TimeUnit.MILLISECONDS)
                        .recover(e -> mode.timeoutSpec.mapTimeoutException(e, "Timeout %d ms sending request headers to %s"))
                        .onSuccess(req -> {
                            if (!finalChunk) {
                                req
                                        .setChunked(true)
                                        .write(buffer)
                                        .timeout(mode.timeoutSpec.remainingTimeout(), TimeUnit.MILLISECONDS)
                                        .recover(e -> mode.timeoutSpec.mapTimeoutException(e,
                                                "Timeout %d ms sending request body to %s"))
                                        .onFailure(t -> mode.responseFailed(t, true));

                                lock.lock();
                                try {
                                    this.request = new Result<>(req, null);
                                    requestReady.signal();
                                } finally {
                                    lock.unlock();
                                }
                            } else {
                                finishRequest(req, buffer);
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

                if (finalChunk) {
                    mode.awaitResponse();
                }
            } else {
                /* Non-first event */
                Future<BodyWriter> bw = bodyWriter;
                if (bw != null) {
                    bw = bw.compose(w -> w.write(buffer.slice()));
                    if (finalChunk) {
                        body = bw.compose(w -> w.close());
                        bodyWriter = null;
                    } else {
                        bodyWriter = bw;
                    }
                }
                final HttpClientRequest req = awaitRequest();
                if (!finalChunk) {
                    req
                            .write(buffer)
                            .timeout(mode.timeoutSpec.remainingTimeout(), TimeUnit.MILLISECONDS)
                            .recover(e -> mode.timeoutSpec.mapTimeoutException(e, "Timeout %d ms sending request body to %s"))
                            .onFailure(RequestBodyHandler.this::failResponse);
                } else {
                    finishRequest(req, buffer);
                    mode.awaitResponse();
                }
            }
        }

        @SuppressWarnings("resource")
        void finishRequest(HttpClientRequest req, Buffer buffer) {
            prepareResponse(req);
            req
                    .end(buffer)
                    .timeout(mode.timeoutSpec.remainingTimeout(), TimeUnit.MILLISECONDS)
                    .recover(e -> mode.timeoutSpec.mapTimeoutException(e, "Timeout %d ms sending request body to %s"))
                    .onFailure(t -> mode.responseFailed(t, true));

        }

        private void prepareResponse(HttpClientRequest req) {
            req.response()
                    .timeout(mode.timeoutSpec.remainingTimeout(), TimeUnit.MILLISECONDS)
                    .recover(e -> mode.timeoutSpec.mapTimeoutException(e,
                            "Timeout waiting %d ms to receive response headers from %s"))
                    .onComplete(ar -> {
                        final InputStreamWriteStream sink = new InputStreamWriteStream(context, mode.timeoutSpec, 2);
                        final HttpClientResponse response = ar.result();
                        if (ar.succeeded()) {

                            /* need to retransmit? */
                            final int statusCode = response.statusCode();
                            final boolean isRedirect = isRedirect(statusCode);
                            final boolean isAuthRetransmit = statusCode == 401 || statusCode == 407;
                            if (possibleRetransmit
                                    && (isRedirect || isAuthRetransmit)
                                    && (maxRetransmits < 0 || performedRetransmits(redirects) < maxRetransmits)) {
                                ResponseHandler.updateResponseHeaders(response, outMessage, cookies);

                                try {
                                    if (isAuthRetransmit) {
                                        authorize(clientInfo.getConfigKey(), response);
                                    } else if (isRedirect) {
                                        redirect(response);
                                    } else {
                                        throw new IllegalStateException("Either authorize or retransmit should be true");
                                    }
                                } catch (IOException e) {
                                    sink.setException((IOException) e);
                                    mode.responseReady(new Result<>(ResponseEvent.prepare(body, response, sink), e));
                                } catch (Exception e) {
                                    final IOException ioe = new IOException(e);
                                    sink.setException(ioe);
                                    mode.responseReady(new Result<>(ResponseEvent.prepare(body, response, sink), ioe));
                                }
                                return;
                            } else {
                                if (!possibleRetransmit && isRedirect) {
                                    final String qKey = QuarkusCxfUtils.quoteCongurationKeyIfNeeded(clientInfo.getConfigKey());
                                    final IOException ioe = new IOException(
                                            "Received redirection status " + statusCode
                                                    + " from " + url + " by client " + qKey
                                                    + " but following redirects is not enabled for this client."
                                                    + " You may want to set quarkus.cxf.client." + qKey
                                                    + ".auto-redirect = true");
                                    sink.setException(ioe);
                                    mode.responseReady(new Result<>(ResponseEvent.prepare(body, response, sink), ioe));
                                    return;
                                }
                                if (possibleRetransmit && isRedirect && maxRetransmits >= 0
                                        && maxRetransmits <= performedRetransmits(redirects)) {
                                    final String qKey = QuarkusCxfUtils.quoteCongurationKeyIfNeeded(clientInfo.getConfigKey());
                                    final IOException ioe = new IOException("Received redirection status " +
                                            statusCode + " from " + redirects.get(redirects.size() - 1)
                                            + " by client " + qKey + ", but already performed maximum"
                                            + " number " + maxRetransmits
                                            + " of allowed retransmits; you may want to"
                                            + " increase quarkus.cxf.client." + qKey + ".max-retransmits. Visited URIs: "
                                            + redirects.stream().map(URI::toString).collect(Collectors.joining(" -> ")));
                                    sink.setException(ioe);
                                    mode.responseReady(new Result<>(ResponseEvent.prepare(body, response, sink), ioe));
                                    return;
                                }
                                /* No retransmit */
                                /* Pass the body back to CXF */
                                // log.trace("Staring pipe");
                                response.pipeTo(sink)
                                        .timeout(mode.timeoutSpec.remainingTimeout(), TimeUnit.MILLISECONDS)
                                        .recover(e -> mode.timeoutSpec.mapTimeoutException(e,
                                                "Timeout waiting %d ms to receive response body from %s"))
                                        .onFailure(e -> {
                                            sink.setException(e);
                                            //log.trace("Pipe failed", e);
                                        });
                                // .onSuccess(v -> log.trace("Pipe finished"));
                            }
                        } else {
                            sink.setException(ar.cause());
                        }
                        mode.responseReady(new Result<>(ResponseEvent.prepare(body, response, sink), ar.cause()));
                    });
        }

        private void redirect(final HttpClientResponse response) throws IOException {
            final URI newUri;
            final String loc = response.getHeader("Location");

            if (loc != null && !loc.startsWith("http")
                    && !MessageUtils.getContextualBoolean(outMessage, AUTO_REDIRECT_ALLOW_REL_URI)) {
                final String qKey = QuarkusCxfUtils
                        .quoteCongurationKeyIfNeeded(clientInfo.getConfigKey());
                throw new IOException(
                        "Illegal relative redirect " + loc + " detected by client " + qKey
                                + "; you may want to set quarkus.cxf.client."
                                + qKey + ".redirect-relative-uri = true");
            }
            final URI previousUri = redirects.get(redirects.size() - 1);
            try {
                newUri = HttpUtils.resolveURIReference(previousUri, loc);
                final String configKey = clientInfo.getConfigKey();
                detectRedirectLoop(configKey, redirects, newUri, outMessage);
                redirects.add(newUri);
                checkAllowedRedirectUri(configKey, previousUri, newUri, outMessage);
                retransmit(newUri, Function.identity());
            } catch (URISyntaxException e) {
                throw new IOException(
                        "Could not resolve redirect Location " + loc + " relative to " + url, e);
            }
        }

        private static int performedRetransmits(List<URI> retransmits) {
            /* The first element in the retransmits list is the original URI that we do not count as a retransmit */
            return retransmits.size() - 1;
        }

        void retransmit(URI newURL, Function<RequestOptions, RequestOptions> requestOptionsCustomizer) throws IOException {
            if (log.isDebugEnabled()) {
                log.debugf("Redirect retransmit: %s",
                        redirects.stream().map(URI::toString).collect(Collectors.joining(" -> ")));
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

            this.body.compose(storedBody -> {
                final long contentLength = storedBody.length();
                if (contentLength >= 0 && requestHasBody(options.getMethod())) {
                    /* Only one buffer recorded */
                    options.putHeader(CONTENT_LENGTH, String.valueOf(contentLength));
                } else {
                    options.removeHeader(CONTENT_LENGTH);
                }

                final HttpClient client = clientPool.getClient(clientInfo, version, tlsConfiguration, proxyConfiguration);

                // Should not be necessary, because we copy from the original requestOptions
                // setProtocolHeaders(outMessage, options, userAgent);

                return client.request(requestOptionsCustomizer.apply(options))
                        .compose(req -> {
                            prepareResponse(req);
                            return storedBody.pipeTo(req).compose(v -> Future.succeededFuture(req));
                        });
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

        private static void detectRedirectLoop(
                String configKey,
                List<URI> redirects,
                URI newURL,
                Message message) throws IOException {
            if (redirects.contains(newURL)) {
                final Integer maxSameURICount = PropertyUtils.getInteger(message, AUTO_REDIRECT_MAX_SAME_URI_COUNT);
                final String qKey = QuarkusCxfUtils.quoteCongurationKeyIfNeeded(configKey);
                if (maxSameURICount != null) {
                    final long sameUriRetransmitsToBePerformed = redirects.stream()
                            .skip(1) // the first element is not a retransmit
                            .filter(newURL::equals)
                            .count()
                            + 1 // +1 because newURL was not added to redirects yet
                    ;
                    if (sameUriRetransmitsToBePerformed > maxSameURICount.longValue()) {
                        final String msg = "Redirect chain with too many same URIs " + newURL
                                + " (found " + sameUriRetransmitsToBePerformed + ", allowed <= " + maxSameURICount.longValue()
                                + ")"
                                + " detected by client " + qKey + ": "
                                + redirects.stream().map(URI::toString).collect(Collectors.joining(" -> "))
                                + " -> " + newURL
                                + ". You may want to increase quarkus.cxf.client." + qKey
                                + ".max-same-uri";
                        throw new IOException(msg);
                    }
                    /* Allowed number of same URI */
                    return;
                }
                final String msg = "Redirect loop detected by client " + qKey + ": "
                        + redirects.stream().map(URI::toString).collect(Collectors.joining(" -> ")) + " -> " + newURL
                        + ". You may want to increase quarkus.cxf.client." + qKey
                        + ".max-same-uri";
                throw new IOException(msg);
            }
        }

        private static void checkAllowedRedirectUri(String configKey,
                URI lastUri,
                URI newUri,
                Message message) throws IOException {
            if (MessageUtils.getContextualBoolean(message, AUTO_REDIRECT_SAME_HOST_ONLY)) {

                // This can be further restricted to make sure newURL completely contains lastURL
                // though making sure the same HTTP scheme and host are preserved should be enough

                if (!newUri.getScheme().equals(lastUri.getScheme())
                        || !newUri.getHost().equals(lastUri.getHost())) {
                    final String qKey = QuarkusCxfUtils.quoteCongurationKeyIfNeeded(configKey);
                    String msg = "Different HTTP scheme or different host detected in redirect URI " + newUri
                            + " compared to original URI " + lastUri + " by client " + qKey;
                    throw new IOException(msg);
                }
            }

            String allowedRedirectURI = (String) message.getContextualProperty(AUTO_REDIRECT_ALLOWED_URI);
            if (allowedRedirectURI != null && !newUri.toString().startsWith(allowedRedirectURI)) {
                final String qKey = QuarkusCxfUtils.quoteCongurationKeyIfNeeded(configKey);
                String msg = "Illegal redirect URI " + newUri + " detected by client " + qKey
                        + "; expected to start with " + allowedRedirectURI;
                throw new IOException(msg);
            }
        }

        private void authorize(
                String configKey,
                HttpClientResponse response) throws IOException {
            final URI currentURI = url;
            final String authHeaderVal = response.getHeader("WWW-Authenticate");
            if (authHeaderVal == null) {
                final String qKey = QuarkusCxfUtils.quoteCongurationKeyIfNeeded(configKey);
                final String logMessage = "WWW-Authenticate response header is not set on a response from "
                        + currentURI
                        + " for client " + qKey;
                throw new IOException(logMessage);
            }
            final HttpAuthHeader authHeader = new HttpAuthHeader(authHeaderVal);
            final String realm = authHeader.getRealm();
            detectAuthorizationLoop(configKey, outMessage, currentURI, realm);
            AuthorizationPolicy effectiveAthPolicy = getEffectiveAuthPolicy(outMessage, authorizationPolicy);
            String authorizationToken = authSupplier.getAuthorization(
                    effectiveAthPolicy, currentURI, outMessage, authHeader.getFullHeader());
            if (authorizationToken == null) {
                // authentication not possible => we give up
                final String qKey = QuarkusCxfUtils.quoteCongurationKeyIfNeeded(configKey);
                final String logMessage = "No authorization token supplied for client " + qKey
                        + " remote URI " + currentURI
                        + " and WWW-Authenticate: " + authHeader.getFullHeader();
                throw new IOException(logMessage);
            }
            retransmit(currentURI, requestOptions -> requestOptions.addHeader("Authorization", authorizationToken));
        }

        /**
         * Determines effective auth policy from message, conduit and empty default
         * with priority from first to last
         *
         * @param message
         * @return effective AthorizationPolicy
         */
        static AuthorizationPolicy getEffectiveAuthPolicy(Message message, AuthorizationPolicy authPolicy) {
            final AuthorizationPolicy newPolicy = message.get(AuthorizationPolicy.class);
            if (newPolicy != null) {
                return newPolicy;
            }
            if (authPolicy != null) {
                return authPolicy;
            }
            return new AuthorizationPolicy();
        }

        private void detectAuthorizationLoop(
                String configKey,
                Message message,
                URI currentURL,
                String realm) throws IOException {
            Set<String> authURLs = authUris;
            if (authURLs == null) {
                authURLs = authUris = new LinkedHashSet<>();
            }
            // If we have been here (URL & Realm) before for this particular message
            // retransmit, it means we have already supplied information
            // which must have been wrong, or we wouldn't be here again.
            // Otherwise, the server may be 401 looping us around the realms.
            if (!authURLs.add(currentURL.toString() + realm)) {
                final String qKey = QuarkusCxfUtils.quoteCongurationKeyIfNeeded(configKey);

                final String logMessage = "Authorization loop detected by client "
                        + qKey + " on URL \""
                        + currentURL
                        + "\" with realm \""
                        + realm
                        + "\"";
                throw new IOException(logMessage);
            }
        }

        void failResponse(Throwable t) {
        }

        static void setProtocolHeaders(Message outMessage, RequestOptions requestOptions, String userAgent, HttpVersion version)
                throws IOException {
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
                if (HttpHeaderHelper.CONNECTION.equalsIgnoreCase(header.getKey()) && version != HttpVersion.HTTP_1_0
                        && version != HttpVersion.HTTP_1_1) {
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
                            throw new TimeoutIOException(
                                    "Timeout waiting " + requestOptions.getConnectTimeout() + " ms for HTTP connect to " + url);
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
                    if (!requestWriteable.await(mode.timeoutSpec.remainingTimeout(), TimeUnit.MILLISECONDS)) {
                        throw new TimeoutIOException("Timeout waiting " + mode.timeoutSpec.totalReceiveTimeout
                                + " ms for sending HTTP headers to " + url);
                    }
                } finally {
                    waitingForDrain = false;
                }
            }
        }

        static abstract class Mode {
            /** Time in epoch milliseconds when the response should be fully received */
            protected final TimeoutSpec timeoutSpec;
            protected final IOEHandler<ResponseEvent> responseHandler;

            Mode(TimeoutSpec timeoutSpec, IOEHandler<ResponseEvent> responseHandler) {
                this.timeoutSpec = timeoutSpec;
                this.responseHandler = responseHandler;
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

                Sync(TimeoutSpec timeoutSpec, IOEHandler<ResponseEvent> responseHandler, ReentrantLock lock) {
                    super(timeoutSpec, responseHandler);
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
                                if (!responseReceived.await(timeoutSpec.remainingTimeout(), TimeUnit.MILLISECONDS)
                                        || response == null) {
                                    timeoutSpec.throwTimeoutException(null,
                                            "Timeout waiting %d ms to receive response headers from %s");
                                }
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IOException("Interrupted waiting for HTTP response from " + timeoutSpec.url, e);
                        } finally {
                            lock.unlock();
                        }
                    }
                    if (response.succeeded()) {
                        return response.result();
                    } else {
                        final Throwable e = response.cause();
                        throw new IOException("Unable to receive HTTP response from " + timeoutSpec.url, e);
                    }
                }

            }

            static class Async extends Mode {
                private final Message outMessage;

                Async(TimeoutSpec timeoutSpec, IOEHandler<ResponseEvent> responseHandler, Message outMessage) {
                    super(timeoutSpec, responseHandler);
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

    static record TimeoutSpec(long receiveTimeoutDeadline, long totalReceiveTimeout, URI url) {

        static TimeoutSpec create(long receiveTimeoutMs, URI url) {
            return new TimeoutSpec(System.currentTimeMillis() + receiveTimeoutMs, receiveTimeoutMs, url);
        }

        /**
         * Computes the timeout for receive related operations based on {@link #receiveTimeoutDeadline} and current time
         *
         * @return the timeout in milliseconds for response related operations
         */
        long remainingTimeout() {
            return receiveTimeoutDeadline - System.currentTimeMillis();
        }

        /**
         * Throws a {@link TimeoutIOException} optionally passing it to the given {@code exceptionConsumer}.
         *
         * @param exceptionConsumer can be {@code null}
         * @param messageTemplate
         * @throws TimeoutIOException
         */
        void throwTimeoutException(Consumer<TimeoutIOException> exceptionConsumer, String messageTemplate)
                throws TimeoutIOException {
            TimeoutIOException result = createTimeoutException(messageTemplate);
            if (exceptionConsumer != null) {
                exceptionConsumer.accept(result);
            }
            throw result;
        }

        /**
         * @return a new {@link TimeoutIOException} without throwing it
         */
        TimeoutIOException createTimeoutException(String messageTemplate) {
            return new TimeoutIOException(messageTemplate.formatted(totalReceiveTimeout, url));
        }

        <T> Future<T> mapTimeoutException(Throwable originalException, String messageTemplate) {
            return Future.failedFuture(
                    originalException instanceof NoStackTraceTimeoutException ? createTimeoutException(messageTemplate)
                            : originalException);
        }

    }

    /**
     * A custom timeout exception inheriting from {@link IOException} and having no stack trace for performance reasons
     */
    @SuppressWarnings("serial")
    public static class TimeoutIOException extends IOException {
        public TimeoutIOException(String msg) {
            super(msg);
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    static record ResponseEvent(HttpClientResponse response, InputStream responseBodyInputStream) {
        public static ResponseEvent prepare(Future<StoredBody> body, HttpClientResponse response,
                InputStream responseBodyInputStream) {
            if (body != null) {
                body.compose(b -> b.discard());
            }
            return new ResponseEvent(response, responseBodyInputStream);
        }
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

        private static final Buffer END = new ErrorBuffer();

        private final TimeoutSpec timeoutSpec;
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition queueChange = lock.newCondition();
        private final ContextInternal context;

        /*
         * Written from the producer thread, read from the consumer thread
         * All read or write operations must be protected by lock
         */
        private final Queue<Buffer> queue;
        private int maxQueueSize;

        /*
         * Written from the consumer thread, read from the producer thread
         * All read or write operations must be protected by lock
         */
        private TimeoutIOException timeoutException;

        /* Read and written from the consumer thread */
        private Buffer readBuffer;
        private int readPosition = 0;
        // private int bytesRead = 0;
        // private int readCounter = 0;

        /* Read and written from the producer thread */
        // private int bytesWritten = 0;
        // private int writeCounter = 0;
        private Handler<Void> drainHandler;

        public InputStreamWriteStream(ContextInternal context, TimeoutSpec timeoutSpec, int queueSize) {
            this.context = context;
            this.timeoutSpec = timeoutSpec;
            setWriteQueueMaxSize(queueSize);
            this.queue = new ArrayDeque<>(queueSize);
        }

        /* WriteStream methods */
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
            Throwable cause = null;
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                if ((cause = timeoutException) != null) {
                    handler.handle(Future.failedFuture(cause));
                    return;
                }
                // bytesWritten += data.length();
                // writeCounter++;
                // log.tracef("Write #%d: %d bytes; %d total; queue size before %d", writeCounter, data.length(), bytesWritten,
                // queue.size());
                queue.offer(data);
                queueChange.signal();
            } catch (Throwable e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                queue.offer(new ErrorBuffer(e));
                queueChange.signal();
                cause = e;
            } finally {
                lock.unlock();
            }
            if (cause != null) {
                handler.handle(Future.failedFuture(cause));
            } else {
                handler.handle(Future.succeededFuture());
            }
        }

        @Override
        public void end(Handler<AsyncResult<Void>> handler) {
            // log.trace("Ending writes");
            drainHandler = null;
            Throwable cause = null;
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                if ((cause = timeoutException) != null) {
                    handler.handle(Future.failedFuture(cause));
                    return;
                }
                // log.tracef("Ending writes, got %d bytes in %d writes; queue size before %d", bytesWritten, writeCounter,
                // queue.size());
                queue.offer(END);
                queueChange.signal();
            } catch (Throwable e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }

                queue.offer(new ErrorBuffer(e));
                queueChange.signal();
                cause = e;
            } finally {
                lock.unlock();
            }
            if (cause != null) {
                handler.handle(Future.failedFuture(cause));
            } else {
                handler.handle(Future.succeededFuture());
            }
        }

        @Override
        public WriteStream<Buffer> setWriteQueueMaxSize(int maxSize) {
            if (maxSize < 1) {
                throw new IllegalArgumentException("maxSize must be >= 1");
            }
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                this.maxQueueSize = maxSize;
            } finally {
                lock.unlock();
            }
            return this;
        }

        @Override
        public boolean writeQueueFull() {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                return writeQueueFullInternal();
            } finally {
                lock.unlock();
            }
        }

        /**
         * Must be called under {@link #lock}.
         *
         * @return
         */
        private boolean writeQueueFullInternal() {
            assert lock.isHeldByCurrentThread();
            return queue.size() >= maxQueueSize;
        }

        @Override
        public WriteStream<Buffer> drainHandler(Handler<Void> handler) {
            // log.tracef("drainHandler %s", drainHandler);
            this.drainHandler = handler;
            return this;
        }

        /* InputStream methods */

        @Override
        public int read() throws IOException {
            final Buffer rb = takeBuffer(true);
            int result = rb != null ? (rb.getByte(readPosition++) & 0xFF) : -1;
            // log.tracef("Read single byte %d", result);
            return result;
        }

        @Override
        public int read(byte b[], final int off, int len) throws IOException {
            // log.tracef("Ready to read up to %d bytes", len);
            Buffer rb = takeBuffer(true);
            if (rb == null) {
                // log.trace("Nothing more to read");
                return -1;
            }
            int rbLen = rb.length();
            int readable = rbLen - readPosition;
            // log.infof("Readable %d bytes", readable);

            int result;
            if (readable >= len) {
                readable = len;
                // log.infof("Downsized readable to %d bytes", readable);
                rb.getBytes(readPosition, readPosition + readable, b, off);
                // log.infof("After read 1: %s", new String(b, off, readable, StandardCharsets.UTF_8));
                readPosition += readable;
                // log.infof("readPosition now at %d", readPosition);
                if (readPosition >= rbLen) {
                    freeReadBufferIfNeeded();
                }
                result = readable;
            } else {
                /*
                 * readable < len so we read out the current buffer completely and we try the subsequent ones if
                 * available
                 */
                rb.getBytes(readPosition, readPosition + readable, b, off);
                // log.infof("Read out current buffer %d completely: %s", System.identityHashCode(rb),
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
                        // log.infof("Downsized readable to %d bytes", readable);
                    }
                    rb.getBytes(readPosition, readPosition + readable, b, off2);
                    // log.infof("Read 2 from buffer %d %d to %d: %s", System.identityHashCode(rb), readPosition,
                    // readPosition + readable,
                    // new String(b, off2, readable, StandardCharsets.UTF_8));
                    readPosition += readable;
                    len -= readable;
                    off2 += readable;
                    // log.infof("readPosition now at %d", readPosition);
                    result += readable;
                }
                if (readPosition == rbLen) {
                    // log.infof("Buffer read out completely");
                    freeReadBufferIfNeeded();
                }
                // assert readPosition <= rbLen;
            }
            // bytesRead += result;
            // log.tracef("Read #%d: %d bytes, %d total", readCounter++, result, bytesRead);
            return result;
        }

        private void freeReadBufferIfNeeded() {
            if (!(readBuffer instanceof ErrorBuffer)) {
                /* readBuffer is either == END or there is an exception set on it */
                readBuffer = null; // deref. so that it can be GC's earlier;
            }
        }

        @Override
        public void close() {
            // log.trace("Closing reader");
            // log.tracef("Closing reader: got %d bytes in %d reads", bytesRead, readCounter);
            freeReadBufferIfNeeded();
            // assert queueEmpty() : "Queue still has " + queue.size() + " items";
        }

        @Override
        public int available() throws IOException {
            Buffer rb = takeBuffer(false);
            if (rb != null) {
                int result = rb.length() - readPosition;

                final List<Buffer> queueCopy;
                final ReentrantLock lock = this.lock;
                lock.lock();
                try {
                    queueCopy = new ArrayList<>(queue);
                } finally {
                    lock.unlock();
                }
                final int len = queueCopy.size();
                for (int i = 0; i < len; i++) {
                    final Buffer b = queueCopy.get(i);
                    if (b instanceof ErrorBuffer) {
                        /*
                         * Either at END or there is some exception passed via an ErrorBuffer
                         * There is no point in iterating further
                         */
                        break;
                    }
                    if (rb != b) {
                        /* Skip the buffer returned by takeBuffer() above */
                        result += b.length();
                    }
                }
                return result;
            }
            return 0;
        }

        private Buffer takeBuffer(boolean blockingAwaitBuffer) throws IOException {
            // log.infof("About to take buffer at queue size %d %s", queue.size(),
            // blockingAwaitBuffer ? "with blocking" : "without blocking");
            Buffer rb = readBuffer;
            if (checkEndOrException(rb)) {
                return null;
            }
            // log.infof("Buffer is null: %s; %d >= %d: %s", rb == null, readPosition, (rb == null ? -1 : rb.length()),
            // rb != null && readPosition >= rb.length());
            if (rb == null || readPosition >= rb.length()) {
                // log.info("Buffer is null or empty");

                final ReentrantLock lock = this.lock;
                final boolean writeQueueFull;
                // final int qSize;
                try {
                    lock.lockInterruptibly();
                    if (blockingAwaitBuffer) {
                        while ((readBuffer = rb = queue.poll()) == null) {
                            // log.infof("Awaiting a buffer at queue size %d", queue.size());
                            if (!queueChange.await(timeoutSpec.remainingTimeout(),
                                    TimeUnit.MILLISECONDS)) {
                                timeoutSpec.throwTimeoutException(e -> timeoutException = e,
                                        "Timeout waiting %d ms to receive response body from %s");
                            }
                        }
                    } else {
                        readBuffer = rb = queue.poll();
                    }
                    writeQueueFull = writeQueueFullInternal();
                    // qSize = queue.size();
                    // log.tracef("%s unblock the producer at queue size %d", (writeQueueFull ? "May not" : "May"), queue.size());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException(e);
                } finally {
                    lock.unlock();
                }
                if (checkEndOrException(rb)) {
                    return null;
                }
                readPosition = 0;

                // log.tracef("Dispatching to drain handler");
                if (!writeQueueFull) {
                    context.runOnContext(v -> {
                        final Handler<Void> dh;
                        // log.tracef("Testing drain handler");
                        if ((dh = drainHandler) != null) {
                            // log.tracef("Calling drain handler");
                            dh.handle(null);
                        }
                    });
                }
            }
            // log.infof("Taken a %s buffer %d, will read from %d to %d; queue size after: %d",
            // (rb != null ? "valid" : "null"),
            // System.identityHashCode(rb),
            // readPosition, (rb != null ? rb.length() : -1), queue.size());
            return rb;
        }

        private static boolean checkEndOrException(Buffer rb) throws IOException {
            if (rb == END) {
                return true;
            }
            if (rb instanceof ErrorBuffer) {
                ((ErrorBuffer) rb).throwIOExceptionIfNeeded();
            }
            return false;
        }

        public void setException(Throwable exception) {
            // log.trace("Passing an exception", exception);
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                queue.offer(new ErrorBuffer(exception));
                queueChange.signal();
            } finally {
                lock.unlock();
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
