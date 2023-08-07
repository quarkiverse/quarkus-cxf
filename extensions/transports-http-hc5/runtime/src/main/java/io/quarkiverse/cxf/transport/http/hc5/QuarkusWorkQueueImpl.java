package io.quarkiverse.cxf.transport.http.hc5;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.workqueue.AutomaticWorkQueue;
import org.eclipse.microprofile.context.ManagedExecutor;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

/**
 * Executes the tasks using {@link ManagedExecutor} for the sake of context propagation.
 */
public class QuarkusWorkQueueImpl implements AutomaticWorkQueue {

    private final String name;

    public QuarkusWorkQueueImpl(String name) {
        this.name = name;
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
        InstanceHandle<ManagedExecutor> managedExecutorInst = Arc.container().instance(ManagedExecutor.class);
        if (managedExecutorInst.isAvailable()) {
            ManagedExecutor managedExecutor = managedExecutorInst.get();
            managedExecutor.execute(r);
        } else {
            throw new IllegalStateException(ManagedExecutor.class.getName() + " not available in Arc");
        }
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
