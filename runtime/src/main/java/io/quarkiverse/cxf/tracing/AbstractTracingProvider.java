package io.quarkiverse.cxf.tracing;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;

public abstract class AbstractTracingProvider {
    protected static String getSpanIdHeader() {
        return getHeaderOrDefault("org.apache.cxf.tracing.header.span_id", "X-Span-Id");
    }

    protected static class TraceScopeHolder<T> implements Serializable {
        private static final long serialVersionUID = -5985783659818936359L;

        private final T scope;
        private final boolean detached;

        public TraceScopeHolder(final T scope, final boolean detached) {
            this.scope = scope;
            this.detached = detached;
        }

        public T getScope() {
            return this.scope;
        }

        public boolean isDetached() {
            return this.detached;
        }
    }

    private static String getHeaderOrDefault(final String property, final String fallback) {
        final Message message = PhaseInterceptorChain.getCurrentMessage();

        if (message != null) {
            final Object header = message.getContextualProperty(property);

            if (header instanceof String) {
                final String name = (String) header;
                if (!StringUtils.isEmpty(name)) {
                    return name;
                }
            }
        }

        return fallback;
    }

    protected String buildSpanDescription(final String path, final String method) {
        if (StringUtils.isEmpty(method)) {
            return path;
        }
        return method + " " + path;
    }

    private static String safeGet(Message message, String key) {
        if (!message.containsKey(key)) {
            return null;
        }
        final Object value = message.get(key);
        return (value instanceof String) ? value.toString() : null;
    }

    private static String getUriAsString(Message message) {
        String uri = safeGet(message, Message.REQUEST_URL);

        if (uri == null) {
            String address = safeGet(message, Message.ENDPOINT_ADDRESS);
            uri = safeGet(message, Message.REQUEST_URI);
            if (uri != null && uri.startsWith("/")) {
                if (address != null && !address.startsWith(uri)) {
                    if (address.endsWith("/") && address.length() > 1) {
                        address = address.substring(0, address.length());
                    }
                    uri = address + uri;
                }
            } else {
                uri = address;
            }
        }
        final String query = safeGet(message, Message.QUERY_STRING);
        if (query != null) {
            return uri + "?" + query;
        }

        return uri;
    }

    protected static URI getUri(Message message) {
        try {
            final String uriSt = getUriAsString(message);
            return uriSt != null ? new URI(uriSt) : new URI("");
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}