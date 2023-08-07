package io.quarkiverse.cxf.transport.http.hc5;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.asyncclient.hc5.AsyncHTTPConduit;
import org.apache.cxf.transport.http.asyncclient.hc5.AsyncHTTPConduitFactory;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.function.Supplier;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.AsyncPushConsumer;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.CapacityChannel;
import org.apache.hc.core5.http.nio.HandlerFactory;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.IOReactorStatus;
import org.apache.hc.core5.util.TimeValue;
import org.eclipse.microprofile.context.ThreadContext;

import io.quarkus.arc.Arc;

/**
 * An {@link AsyncHTTPConduit} with custom {@link #getHttpAsyncClient(TlsStrategy)}.
 */
public class QuarkusAsyncHTTPConduit extends AsyncHTTPConduit {

    public QuarkusAsyncHTTPConduit(Bus b, EndpointInfo ei, EndpointReferenceType t, AsyncHTTPConduitFactory factory)
            throws IOException {
        super(b, ei, t, factory);
    }

    /**
     * @param tlsStrategy
     * @return a Custom {@link CloseableHttpAsyncClient} whose {@code execute(*)} methods contextualize (see
     *         {@link ThreadContext}) the passed {@link AsyncResponseConsumer}
     * @throws IOException
     */
    @Override
    public synchronized CloseableHttpAsyncClient getHttpAsyncClient(TlsStrategy tlsStrategy) throws IOException {
        return new QuarkusAsyncClient(super.getHttpAsyncClient(tlsStrategy));
    }

    static class QuarkusAsyncClient extends CloseableHttpAsyncClient {
        private final CloseableHttpAsyncClient delegate;

        public QuarkusAsyncClient(CloseableHttpAsyncClient delegate) {
            super();
            this.delegate = delegate;
        }

        @Override
        public void close(CloseMode closeMode) {
            delegate.close(closeMode);
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public void start() {
            delegate.start();
        }

        @Override
        public IOReactorStatus getStatus() {
            return delegate.getStatus();
        }

        @Override
        public void awaitShutdown(TimeValue waitTime) throws InterruptedException {
            delegate.awaitShutdown(waitTime);
        }

        @Override
        public void initiateShutdown() {
            delegate.initiateShutdown();
        }

        @Override
        public boolean equals(Object obj) {
            return delegate.equals(obj);
        }

        @Override
        public void register(String hostname, String uriPattern, Supplier<AsyncPushConsumer> supplier) {
            delegate.register(hostname, uriPattern, supplier);
        }

        @Override
        public String toString() {
            return delegate.toString();
        }

        @Override
        protected <T> Future<T> doExecute(
                final HttpHost target,
                final AsyncRequestProducer requestProducer,
                final AsyncResponseConsumer<T> responseConsumer,
                final HandlerFactory<AsyncPushConsumer> pushHandlerFactory,
                final HttpContext context,
                final FutureCallback<T> callback) {
            return delegate.execute(
                    target,
                    requestProducer,
                    new ContextualizedResponseConsumer<T>(responseConsumer),
                    pushHandlerFactory,
                    context,
                    callback);
        }
    }

    /**
     * Wraps the delegate in {@link ThreadContext#contextualConsumer(Consumer)} so that context propagation works for
     * async clients
     *
     * @param <T>
     */
    static class ContextualizedResponseConsumer<T> implements AsyncResponseConsumer<T> {
        private final AsyncResponseConsumer<T> delegate;
        private Consumer<ConsumeResponseArgs<T>> contextualConsumer;

        public ContextualizedResponseConsumer(AsyncResponseConsumer<T> delegate) {
            super();
            this.delegate = delegate;
            final ThreadContext threadContext = Arc.container().select(ThreadContext.class).get();
            /*
             * We need to call this threadContext.contextualConsumer() here in the constructor to store the context
             * because consumeResponse() is called from another thread where the context is not available anymore
             */
            this.contextualConsumer = threadContext.contextualConsumer(args -> {
                try {
                    delegate.consumeResponse(
                            args.response,
                            args.entityDetails,
                            args.context,
                            args.resultCallback);
                } catch (HttpException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        @Override
        public void updateCapacity(CapacityChannel capacityChannel) throws IOException {
            delegate.updateCapacity(capacityChannel);
        }

        @Override
        public void consumeResponse(HttpResponse response, EntityDetails entityDetails, HttpContext context,
                FutureCallback<T> resultCallback) throws HttpException, IOException {
            contextualConsumer.accept(new ConsumeResponseArgs<>(response, entityDetails, context, resultCallback));
        }

        @Override
        public void releaseResources() {
            delegate.releaseResources();
        }

        @Override
        public void consume(ByteBuffer src) throws IOException {
            delegate.consume(src);
        }

        @Override
        public void informationResponse(HttpResponse response, HttpContext context) throws HttpException, IOException {
            delegate.informationResponse(response, context);
        }

        @Override
        public void streamEnd(List<? extends Header> trailers) throws HttpException, IOException {
            delegate.streamEnd(trailers);
        }

        @Override
        public void failed(Exception cause) {
            delegate.failed(cause);
        }

        static class ConsumeResponseArgs<T> {
            public ConsumeResponseArgs(HttpResponse response, EntityDetails entityDetails, HttpContext context,
                    FutureCallback<T> resultCallback) {
                super();
                this.response = response;
                this.entityDetails = entityDetails;
                this.context = context;
                this.resultCallback = resultCallback;
            }

            private final HttpResponse response;
            private final EntityDetails entityDetails;
            private final HttpContext context;
            private final FutureCallback<T> resultCallback;
        }
    }

}
