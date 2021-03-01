package io.quarkiverse.cxf.tracing;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.Provider;
import org.apache.cxf.annotations.Provider.Scope;
import org.apache.cxf.annotations.Provider.Type;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.feature.DelegatingFeature;
import org.apache.cxf.interceptor.InterceptorProvider;

import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

@NoJSR250Annotations
@Provider(value = Type.Feature, scope = Scope.Server)
public class OpenTracingFeature extends DelegatingFeature<OpenTracingFeature.Portable> {
    public OpenTracingFeature() {
        super(new Portable());
    }

    public OpenTracingFeature(final Tracer tracer) {
        super(new Portable(tracer));
    }

    public static class Portable implements AbstractPortableFeature {
        private final OpenTracingStartInterceptor in;
        private final OpenTracingStopInterceptor out;

        public Portable() {
            this(GlobalTracer.get());
        }

        public Portable(final Tracer tracer) {
            this.in = new OpenTracingStartInterceptor(tracer);
            this.out = new OpenTracingStopInterceptor(tracer);
        }

        @Override
        public void doInitializeProvider(InterceptorProvider provider, Bus bus) {
            provider.getInInterceptors().add(this.in);
            provider.getInFaultInterceptors().add(this.in);

            provider.getOutInterceptors().add(this.out);
            provider.getOutFaultInterceptors().add(this.out);
        }
    }
}