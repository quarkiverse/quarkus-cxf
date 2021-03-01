package io.quarkiverse.cxf.tracing;

import io.opentracing.Scope;
import io.opentracing.Span;

public class TraceScope {
    private final Span span;
    private final Scope scope;

    TraceScope(final Span span, final Scope scope) {
        this.span = span;
        this.scope = scope;
    }

    public Span getSpan() {
        return this.span;
    }

    public Scope getScope() {
        return this.scope;
    }
}
