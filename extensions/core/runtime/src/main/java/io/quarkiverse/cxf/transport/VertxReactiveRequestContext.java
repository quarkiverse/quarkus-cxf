package io.quarkiverse.cxf.transport;

import io.vertx.ext.web.RoutingContext;

public class VertxReactiveRequestContext {
    private final RoutingContext context;
    private final Deployment deployment;

    public VertxReactiveRequestContext(
            RoutingContext context,
            int minChunkSize,
            int outputBufferSize) {
        super();
        this.context = context;
        this.deployment = new Deployment(minChunkSize, outputBufferSize);
    }

    public Deployment getDeployment() {
        return deployment;
    }

    public RoutingContext getContext() {
        return context;
    }

    public static class Deployment {

        private final ResteasyReactiveConfig resteasyReactiveConfig;

        public Deployment(int minChunkSize, int outputBufferSize) {
            super();
            this.resteasyReactiveConfig = new ResteasyReactiveConfig(minChunkSize, outputBufferSize);
        }

        public ResteasyReactiveConfig getResteasyReactiveConfig() {
            return resteasyReactiveConfig;
        }

    }

    public static class ResteasyReactiveConfig {
        private final int minChunkSize;
        private final int outputBufferSize;

        public ResteasyReactiveConfig(int minChunkSize, int outputBufferSize) {
            super();
            this.minChunkSize = minChunkSize;
            this.outputBufferSize = outputBufferSize;
        }

        public int getOutputBufferSize() {
            return outputBufferSize;
        }

        public int getMinChunkSize() {
            return minChunkSize;
        }
    }
}
