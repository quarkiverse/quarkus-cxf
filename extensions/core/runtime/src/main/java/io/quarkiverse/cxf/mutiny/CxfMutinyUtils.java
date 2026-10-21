package io.quarkiverse.cxf.mutiny;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import jakarta.xml.ws.AsyncHandler;

import org.jboss.logging.Logger;

import io.quarkiverse.cxf.CxfConfig;
import io.quarkiverse.cxf.StacklessRejectedExecutionException;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.runtime.BlockingOperationControl;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.vertx.core.Vertx;

/**
 * Methods to map JAX-WS 2.0 asynchronous SOAP client calls to Mutiny types.
 *
 * @since 4.19.0
 */
public class CxfMutinyUtils {
    private static final Logger log = Logger.getLogger(CxfMutinyUtils.class);

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
        return new WsAsyncHandlerUni<>(subscriptionConsumer, (payload, context) -> payload);
    }

    /**
     * Use this rather than {@link #toUni(Consumer)} when you need to access the response context map in the subsequent
     * steps.
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
        return new WsAsyncHandlerUni<>(subscriptionConsumer, (payload, context) -> new SucceededResponse<T>(payload, context));
    }

    static class WsAsyncHandlerUni<T, P> extends AbstractUni<T> implements Uni<T> {
        private final Consumer<AsyncHandler<P>> subscriptionConsumer;
        private final BiFunction<P, Map<String, Object>, T> mapper;

        public WsAsyncHandlerUni(Consumer<AsyncHandler<P>> subscriptionConsumer, BiFunction<P, Map<String, Object>, T> mapper) {
            this.subscriptionConsumer = Infrastructure.decorate(subscriptionConsumer);
            this.mapper = mapper;
        }

        @Override
        public void subscribe(UniSubscriber<? super T> downstream) {
            final AtomicBoolean terminated = new AtomicBoolean();
            downstream.onSubscribe(() -> terminated.set(true));
            if (!terminated.get()) {
                if (!BlockingOperationControl.isBlockingAllowed()) {
                    /*
                     * We are on Vert.x event loop.
                     * Because subscriptionConsumer.accept() can perform blocking operations,
                     * we dispatch the task to a worker thread.
                     */
                    final ArcContainer container = Arc.container();
                    final long workerDispatchTimeout = container.instance(CxfConfig.class).get().client()
                            .workerDispatchTimeout();
                    final Vertx vertx = container.instance(Vertx.class).get();

                    final AtomicBoolean dispatchCompleted;
                    final long timerId;
                    if (workerDispatchTimeout > 0) {
                        dispatchCompleted = new AtomicBoolean();
                        timerId = vertx.setTimer(workerDispatchTimeout, id -> {
                            boolean shouldTimeout = dispatchCompleted.compareAndSet(false, true)
                                    && !terminated.getAndSet(true);
                            log.debugf("Timer %d will timeout: %s", (Object) id, shouldTimeout);
                            if (shouldTimeout) {
                                downstream.onFailure(
                                        StacklessRejectedExecutionException.workerDispatchTimeout(workerDispatchTimeout));
                            }
                        });
                        log.debugf("Created timer %d with timeout %d", timerId, workerDispatchTimeout);
                    } else {
                        dispatchCompleted = null;
                        timerId = -1;
                    }

                    vertx.executeBlocking(() -> {
                        if (dispatchCompleted != null) {
                            /* The timeout covers waiting for a worker, not waiting for the SOAP response. */
                            vertx.cancelTimer(timerId);
                            final boolean alive = dispatchCompleted.compareAndSet(false, true);
                            if (log.isDebugEnabled()) {
                                log.debugf("Scheduled on a worker thread for timer %d: %s", timerId, alive ? "alive" : "dead");
                            }
                            if (!alive) {
                                return null;
                            }
                        }
                        if (!terminated.get()) {
                            subscribeInternal(downstream, terminated);
                        }
                        return null;
                    }).onFailure(e -> {
                        if (!terminated.getAndSet(true)) {
                            downstream.onFailure(e);
                        }
                    });
                } else {
                    /*
                     * We are not on Vert.x event loop so we may call subscriptionConsumer.accept() as is.
                     */
                    subscribeInternal(downstream, terminated);
                }
            }
        }

        private void subscribeInternal(UniSubscriber<? super T> downstream, AtomicBoolean terminated) {
            try {
                subscriptionConsumer.accept(response -> {
                    boolean alive = !terminated.getAndSet(true);
                    if (alive) {
                        try {
                            downstream.onItem(mapper.apply(response.get(), response.getContext()));
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
