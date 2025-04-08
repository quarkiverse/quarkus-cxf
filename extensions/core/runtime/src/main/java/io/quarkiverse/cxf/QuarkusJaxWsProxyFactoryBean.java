package io.quarkiverse.cxf;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.xml.ws.AsyncHandler;
import jakarta.xml.ws.Binding;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.EndpointReference;
import jakarta.xml.ws.Response;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientFactoryBean;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.jboss.logging.Logger;

import io.quarkus.runtime.BlockingOperationControl;
import io.vertx.core.Vertx;

public class QuarkusJaxWsProxyFactoryBean extends JaxWsProxyFactoryBean {

    private static final Logger log = Logger.getLogger(QuarkusJaxWsProxyFactoryBean.class);

    private final Class<?>[] additionalImplementingClasses;
    private final Vertx vertx;
    private final long workerDispatchTimeout;

    public QuarkusJaxWsProxyFactoryBean(
            ClientFactoryBean fact,
            Vertx vertx,
            long workerDispatchTimeout,
            Class<?>... additionalImplementingClasses) {
        super(fact);
        this.vertx = vertx;
        this.workerDispatchTimeout = workerDispatchTimeout;
        this.additionalImplementingClasses = additionalImplementingClasses;
    }

    @Override
    protected Class<?>[] getImplementingClasses() {
        Class<?> cls = getClientFactoryBean().getServiceClass();
        Class<?>[] result = new Class<?>[additionalImplementingClasses.length + 1];
        result[0] = cls;
        System.arraycopy(additionalImplementingClasses, 0, result, 1, additionalImplementingClasses.length);
        return result;
    }

    @Override
    protected ClientProxy clientClientProxy(Client c) {
        return new QuarkusJaxWsClientProxy(vertx, (JaxWsClientProxy) super.clientClientProxy(c), workerDispatchTimeout);
    }

    public static class QuarkusJaxWsClientProxy extends ClientProxy implements BindingProvider {

        private final JaxWsClientProxy delegate;
        private final Vertx vertx;
        private final long workerDispatchTimeout;

        public QuarkusJaxWsClientProxy(Vertx vertx, JaxWsClientProxy delegate, long workerDispatchTimeout) {
            super(delegate.getClient());
            this.vertx = vertx;
            this.delegate = delegate;
            this.workerDispatchTimeout = workerDispatchTimeout;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            final boolean isAsync = isAsync(method);
            if (isAsync && !BlockingOperationControl.isBlockingAllowed()) {
                /* We are returning a Future and we are on Vert.x event loop thread */

                final CompletableFuture<Response<Object>> result = new CompletableFuture<>();

                /*
                 * We complete the result Future using AsyncHandler because that one gets a completed Response
                 * whose get() method does not block - see org.apache.cxf.jaxws.JaxwsClientCallback for how
                 * the AsyncHandler is called
                 */
                final Object[] newArgs;
                final AsyncHandler<Object> newAsyncHandler;
                final int len = args.length;
                if (len > 0 && args[len - 1] instanceof AsyncHandler) {
                    final AsyncHandler<Object> jaxWsHandler = (AsyncHandler<Object>) args[len - 1];
                    newArgs = new Object[len];
                    System.arraycopy(args, 0, newArgs, 0, len);
                    newArgs[len - 1] = newAsyncHandler = new AsyncHandler<Object>() {
                        @Override
                        public void handleResponse(Response<Object> res) {
                            try {
                                jaxWsHandler.handleResponse(res);
                            } finally {
                                result.complete(res);
                            }
                        }
                    };
                } else {
                    newArgs = new Object[len + 1];
                    System.arraycopy(args, 0, newArgs, 0, len);
                    newArgs[len] = newAsyncHandler = new AsyncHandler<Object>() {
                        @Override
                        public void handleResponse(Response<Object> res) {
                            result.complete(res);
                        }
                    };
                }

                /*
                 * Because even the async mode of VertxHttpClientConduit may block,
                 * we better dispatch the invocation to a worker thread
                 */
                final AtomicBoolean completed;
                final long timerId;
                if (workerDispatchTimeout > 0) {
                    completed = new AtomicBoolean(false);
                    timerId = vertx.setTimer(workerDispatchTimeout, id -> {
                        boolean shouldTimeout = completed.compareAndSet(false, true);
                        log.debugf("Timer %d will timeout: %s", (Object) id, shouldTimeout);
                        if (shouldTimeout) {
                            newAsyncHandler.handleResponse(new QuarkusJaxWsFailedResponse<>(
                                    StacklessRejectedExecutionException.workerDispatchTimeout(workerDispatchTimeout)));
                        }
                    });
                    log.debugf("Created timer %d with timeout %d", timerId, workerDispatchTimeout);
                } else {
                    completed = null;
                    timerId = -1;
                }

                vertx.executeBlocking(new Callable<>() {
                    @Override
                    public Void call() throws Exception {
                        if (completed != null) {
                            vertx.cancelTimer(timerId);
                            final boolean alive = completed.compareAndSet(false, true);
                            if (log.isDebugEnabled()) {
                                log.debugf("Scheduled on a worker thread for timer %d: %s", timerId, alive ? "alive" : "dead");
                            }
                            if (!alive) {
                                /* The timer handler has already thrown a RejectedExecutionException */
                                return null;
                            }
                        }
                        try {
                            delegate.invoke(proxy, method, newArgs);
                            return null;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw e;
                        } catch (Exception e) {
                            throw e;
                        } catch (Throwable e) {
                            throw new Exception(e);
                        }
                    }
                }).onFailure(e -> {
                    newAsyncHandler.handleResponse(new QuarkusJaxWsFailedResponse<>(e));
                });
                return new QuarkusJaxWsResponse<Object>(result);
            }
            return delegate.invoke(proxy, method, args);

        }

        boolean isAsync(Method m) {
            return m.getName().endsWith("Async")
                    && (Future.class.equals(m.getReturnType())
                            || Response.class.equals(m.getReturnType()));
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
        public Object invokeSync(Method method, BindingOperationInfo oi, Object[] params) throws Exception {
            return delegate.invokeSync(method, oi, params);
        }

        @Override
        public Map<String, Object> getRequestContext() {
            return delegate.getRequestContext();
        }

        @Override
        public Map<String, Object> getResponseContext() {
            return delegate.getResponseContext();
        }

        @Override
        public Client getClient() {
            return delegate.getClient();
        }

        @Override
        public boolean equals(Object obj) {
            return delegate.equals(obj);
        }

        @Override
        public String toString() {
            return delegate.toString();
        }

        @Override
        public Binding getBinding() {
            return delegate.getBinding();
        }

        @Override
        public EndpointReference getEndpointReference() {
            return delegate.getEndpointReference();
        }

        @Override
        public <T extends EndpointReference> T getEndpointReference(Class<T> clazz) {
            return delegate.getEndpointReference(clazz);
        }

        static class QuarkusJaxWsFailedResponse<T> implements Response<T> {
            private final CompletableFuture<T> delegate;

            public QuarkusJaxWsFailedResponse(Throwable e) {
                super();
                this.delegate = CompletableFuture.failedFuture(e);
            }

            @Override
            public Map<String, Object> getContext() {
                /* Not available yet - return null */
                return null;
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return delegate.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return delegate.isCancelled();
            }

            @Override
            public boolean isDone() {
                return delegate.isDone();
            }

            @Override
            public T get() throws InterruptedException, ExecutionException {
                return delegate.get();
            }

            @Override
            public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return delegate.get(timeout, unit);
            }

        }

        static class QuarkusJaxWsResponse<T> implements Response<T> {

            final CompletableFuture<Response<T>> delegate;

            public QuarkusJaxWsResponse(CompletableFuture<Response<T>> delegate) {
                this.delegate = delegate;
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return delegate.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return delegate.isCancelled();
            }

            @Override
            public boolean isDone() {
                return delegate.isDone();
            }

            @Override
            public T get() throws InterruptedException, ExecutionException {
                return delegate.get().get();
            }

            @Override
            public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return delegate.get(timeout, unit).get();
            }

            @Override
            public Map<String, Object> getContext() {
                try {
                    return delegate.get().getContext();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }

        }

    }

}
