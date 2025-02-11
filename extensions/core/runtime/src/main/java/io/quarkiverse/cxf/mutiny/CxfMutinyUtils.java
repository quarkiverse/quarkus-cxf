package io.quarkiverse.cxf.mutiny;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import jakarta.xml.ws.AsyncHandler;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;

/**
 * Methods to map JAX-WS 2.0 asynchronous SOAP client calls to Mutiny types.
 *
 * @since 4.19.0
 */
public class CxfMutinyUtils {

    /**
     * See <a href=
     * "https://docs.quarkiverse.io/quarkus-cxf/dev/user-guide/advanced-client-topics/asynchronous-client.html#callback-based-asynchronous-method">Quarkus
     * CXF docs</a>
     * for more information and an example.
     *
     * @param <T> the type of the SOAP response.
     * @param subscriptionConsumer
     * @return an {@link Uni} encapsulating the result of an asynchronous SOAP clien call
     * @since 3.19.0
     */
    public static <T> Uni<T> toUni(Consumer<AsyncHandler<T>> subscriptionConsumer) {
        return new WsAsyncHandlerUni<>(subscriptionConsumer);
    }

    /**
     * Use this rather than {@link #toUni(Consumer)} when you need to access the response context map in the subsequent steps.
     *
     * See <a href=
     * "https://docs.quarkiverse.io/quarkus-cxf/dev/user-guide/advanced-client-topics/asynchronous-client.html#callback-based-asynchronous-method">Quarkus
     * CXF docs</a>
     * for more information.
     *
     * @param <T> the type of the SOAP response.
     * @param subscriptionConsumer
     * @return an {@link Uni} encapsulating the result of an asynchronous SOAP client call
     * @since 3.19.0
     */
    public static <T> Uni<SucceededResponse<T>> toResponseUni(Consumer<AsyncHandler<T>> subscriptionConsumer) {
        return new WsAsyncHandlerResponseUni<>(subscriptionConsumer);
    }

    static class WsAsyncHandlerUni<T> extends AbstractUni<T> implements Uni<T> {
        private final Consumer<AsyncHandler<T>> subscriptionConsumer;

        public WsAsyncHandlerUni(Consumer<AsyncHandler<T>> subscriptionConsumer) {
            this.subscriptionConsumer = Infrastructure.decorate(subscriptionConsumer);
        }

        @Override
        public void subscribe(UniSubscriber<? super T> downstream) {
            AtomicBoolean terminated = new AtomicBoolean();
            downstream.onSubscribe(() -> terminated.set(true));

            if (!terminated.get()) {
                try {
                    subscriptionConsumer.accept(response -> {
                        if (!terminated.getAndSet(true)) {
                            try {
                                downstream.onItem(response.get());
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                downstream.onFailure(e);
                            } catch (ExecutionException e) {
                                downstream.onFailure(e.getCause());
                            } catch (Exception e) {
                                downstream.onFailure(e);
                            }
                        }
                    });
                } catch (Exception e) {
                    if (!terminated.getAndSet(true)) {
                        downstream.onFailure(e);
                    }
                }
            }
        }
    }

    static class WsAsyncHandlerResponseUni<T> extends AbstractUni<SucceededResponse<T>> implements Uni<SucceededResponse<T>> {
        private final Consumer<AsyncHandler<T>> subscriptionConsumer;

        public WsAsyncHandlerResponseUni(Consumer<AsyncHandler<T>> subscriptionConsumer) {
            this.subscriptionConsumer = Infrastructure.decorate(subscriptionConsumer);
        }

        @Override
        public void subscribe(UniSubscriber<? super SucceededResponse<T>> downstream) {
            AtomicBoolean terminated = new AtomicBoolean();
            downstream.onSubscribe(() -> terminated.set(true));

            if (!terminated.get()) {
                try {
                    subscriptionConsumer.accept(response -> {
                        if (!terminated.getAndSet(true)) {
                            try {
                                downstream.onItem(new SucceededResponse<T>(response.get(), response.getContext()));
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                downstream.onFailure(new FailedResponse(e, response.getContext()));
                            } catch (ExecutionException e) {
                                downstream.onFailure(new FailedResponse(e.getCause(), response.getContext()));
                            } catch (Exception e) {
                                downstream.onFailure(new FailedResponse(e, response.getContext()));
                            }
                        }
                    });
                } catch (Exception e) {
                    if (!terminated.getAndSet(true)) {
                        downstream.onFailure(e);
                    }
                }
            }
        }
    }

}
