package io.quarkiverse.cxf.tracing;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptor;

import io.opentracing.Tracer;

public abstract class AbstractOpenTracingInterceptor extends AbstractOpenTracingProvider
        implements PhaseInterceptor<Message> {

    private final String phase;

    protected AbstractOpenTracingInterceptor(String phase, Tracer tracer) {
        super(tracer);
        this.phase = phase;
    }

    @Override
    public Set<String> getAfter() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getBefore() {
        return Collections.emptySet();
    }

    @Override
    public String getId() {
        return this.getClass().getName();
    }

    @Override
    public String getPhase() {
        return this.phase;
    }

    @Override
    public Collection<PhaseInterceptor<? extends Message>> getAdditionalInterceptors() {
        return null;
    }

    @Override
    public void handleFault(Message message) {
    }
}
