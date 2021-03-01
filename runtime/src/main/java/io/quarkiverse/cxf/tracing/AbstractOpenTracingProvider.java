package io.quarkiverse.cxf.tracing;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format.Builtin;
import io.opentracing.tag.Tags;

public abstract class AbstractOpenTracingProvider extends AbstractTracingProvider {
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractOpenTracingProvider.class);
    protected static final String TRACE_SPAN = "org.apache.cxf.tracing.opentracing.span";

    protected final Tracer tracer;

    protected AbstractOpenTracingProvider(final Tracer tracer) {
        this.tracer = tracer;
    }

    protected TraceScopeHolder<TraceScope> startTraceSpan(final Map<String, List<String>> requestHeaders,
            URI uri, String method) {

        final SpanContext parent = this.tracer.extract(Builtin.HTTP_HEADERS,
                new HeaderExtractAdapter(requestHeaders));

        Span activeSpan = null;
        Scope scope = null;
        if (parent == null) {
            activeSpan = this.tracer.buildSpan(this.buildSpanDescription(uri.getPath(), method)).start();
            scope = this.tracer.scopeManager().activate(activeSpan, false);
        } else {
            activeSpan = this.tracer.buildSpan(this.buildSpanDescription(uri.getPath(), method)).asChildOf(parent).start();
            scope = this.tracer.scopeManager().activate(activeSpan, false);
        }

        // Set additional tags
        activeSpan.setTag(Tags.HTTP_METHOD.getKey(), method);
        activeSpan.setTag(Tags.HTTP_URL.getKey(), uri.toString());

        // If the service resource is using asynchronous processing mode, the trace
        // scope will be closed in another thread and as such should be detached.
        Span span = null;
        if (this.isAsyncResponse()) {
            // Do not modify the current context span
            span = activeSpan;
            this.propagateContinuationSpan(span);
            scope.close();
        }

        return new TraceScopeHolder<>(new TraceScope(activeSpan, scope), span != null);
    }

    protected void stopTraceSpan(final Map<String, List<String>> requestHeaders,
            final Map<String, List<Object>> responseHeaders,
            final int responseStatus,
            final TraceScopeHolder<TraceScope> holder) {

        if (holder == null) {
            return;
        }

        final TraceScope traceScope = holder.getScope();
        if (traceScope != null) {
            final Span span = traceScope.getSpan();
            Scope scope = traceScope.getScope();

            // If the service resource is using asynchronous processing mode, the trace
            // scope has been created in another thread and should be re-attached to the current
            // one.
            if (holder.isDetached()) {
                scope = this.tracer.scopeManager().activate(span, false);
            }

            span.setTag(Tags.HTTP_STATUS.getKey(), responseStatus);
            span.setTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER);
            span.finish();

            scope.close();
        }
    }

    protected boolean isAsyncResponse() {
        return !PhaseInterceptorChain.getCurrentMessage().getExchange().isSynchronous();
    }

    private void propagateContinuationSpan(final Span continuation) {
        PhaseInterceptorChain.getCurrentMessage().put(Span.class, continuation);
    }
}