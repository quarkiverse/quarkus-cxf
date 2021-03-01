package io.quarkiverse.cxf.tracing;

import java.util.List;
import java.util.Map;

import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

import io.opentracing.Tracer;

@NoJSR250Annotations
public class OpenTracingStartInterceptor extends AbstractOpenTracingInterceptor {
    public OpenTracingStartInterceptor(Tracer tracer) {
        super(Phase.PRE_INVOKE, tracer);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        final Map<String, List<String>> headers = CastUtils.cast((Map<?, ?>) message.get(Message.PROTOCOL_HEADERS));

        final TraceScopeHolder<TraceScope> holder = super.startTraceSpan(headers,
                getUri(message), (String) message.get(Message.HTTP_REQUEST_METHOD));

        if (holder != null) {
            message.getExchange().put(TRACE_SPAN, holder);
        }
    }
}