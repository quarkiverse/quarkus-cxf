package io.quarkiverse.cxf;

import java.util.concurrent.RejectedExecutionException;

public class StacklessRejectedExecutionException extends RejectedExecutionException {

    private static final long serialVersionUID = 1L;

    public static RejectedExecutionException workerDispatchTimeout(long workerDispatchTimeout) {
        return new StacklessRejectedExecutionException("Unable to dispatch SOAP client call within " + workerDispatchTimeout
                + " ms on a worker thread due to worker thread pool exhaustion."
                + " You may want to adjust one or more of the following configuration options:"
                + " quarkus.thread-pool.core-threads, quarkus.thread-pool.max-threads, quarkus.cxf.client.worker-dispatch-timeout");
    }

    private StacklessRejectedExecutionException(String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

}
