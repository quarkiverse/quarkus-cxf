package io.quarkiverse.cxf.transport.http.hc5;

import java.io.IOException;
import java.util.function.Consumer;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitFactory;
import org.apache.cxf.transport.http.asyncclient.hc5.AsyncHTTPConduitFactory;
import org.apache.cxf.transport.http.asyncclient.hc5.AsyncHttpResponseWrapperFactory;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.eclipse.microprofile.context.ManagedExecutor;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.HTTPConduitSpec;
import io.quarkiverse.cxf.vertx.http.client.HttpClientPool;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class Hc5Recorder {

    /**
     * Customize the runtime {@link Bus} by adding a custom work queue for {@code http-conduit} and a custom
     * {@link HTTPConduitFactory}. Both are there to enable context propagation for async clients.
     *
     * @return a new {@link RuntimeValue} holding a {@link Consumer} to customize the runtime {@link Bus}
     */
    public RuntimeValue<Consumer<Bus>> customizeBus() {

        return new RuntimeValue<>(bus -> {
            bus.setExtension(new Hc5HTTPConduitImpl(), HTTPConduitSpec.class);
            final WorkQueueManager wqm = bus.getExtension(WorkQueueManager.class);
            InstanceHandle<ManagedExecutor> managedExecutorInst = Arc.container().instance(ManagedExecutor.class);
            if (managedExecutorInst.isAvailable()) {
                wqm.addNamedWorkQueue("http-conduit", new QuarkusWorkQueueImpl("http-conduit", managedExecutorInst.get()));
            } else {
                throw new IllegalStateException(ManagedExecutor.class.getName() + " not available in Arc");
            }
            bus.setExtension(new QuarkusAsyncHttpResponseWrapperFactory(), AsyncHttpResponseWrapperFactory.class);
        });
    }

    public static class Hc5HTTPConduitImpl implements HTTPConduitSpec {
        private AsyncHTTPConduitFactory asyncHTTPConduitFactory;

        @Override
        public HTTPConduit createConduit(CXFClientInfo cxfClientInfo, HttpClientPool httpClientPool, Bus b,
                EndpointInfo localInfo,
                EndpointReferenceType target)
                throws IOException {
            AsyncHTTPConduitFactory factory;
            if ((factory = asyncHTTPConduitFactory) == null) {
                factory = asyncHTTPConduitFactory = new AsyncHTTPConduitFactory(b);
            }
            return factory.createConduit(b, localInfo, target);
        }

        @Override
        public String getConduitDescription() {
            return "io.quarkiverse.cxf:quarkus-cxf-rt-transports-http-hc5";
        }

    }
}
