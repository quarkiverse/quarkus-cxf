package io.quarkiverse.cxf.transport.http.hc5;

import java.util.concurrent.Executor;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.workqueue.AutomaticWorkQueue;
import org.eclipse.microprofile.context.ManagedExecutor;

/**
 * Executes the tasks using {@link ManagedExecutor} for the sake of context propagation.
 */
public class QuarkusWorkQueueImpl implements AutomaticWorkQueue {

    private final String name;
    private final Executor executor;

    public QuarkusWorkQueueImpl(String name, Executor executor) {
        this.name = name;
        this.executor = executor;
    }

    @Override
    public void execute(final Runnable command) {
        //Grab the context classloader of this thread.   We'll make sure we use that
        //on the thread the runnable actually runs on.

        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Runnable r = new Runnable() {
            public void run() {
                ClassLoaderHolder orig = ClassLoaderUtils.setThreadContextClassloader(loader);
                try {
                    command.run();
                } finally {
                    if (orig != null) {
                        orig.reset();
                    }
                }
            }
        };
        executor.execute(r);
    }

    @Override
    public void execute(Runnable work, long timeout) {
        execute(work);
    }

    @Override
    public synchronized void schedule(final Runnable work, final long delay) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void shutdown(boolean processRemainingWorkItems) {
        // we do not shot down the managed executor instance
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

}
