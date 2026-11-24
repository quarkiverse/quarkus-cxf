package io.quarkiverse.cxf.it.http3;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import io.vertx.core.http.HttpConnection;

/**
 * A registry of the identifiers of the HTTP connections through which {@link ProtocolEcho#connectionId(long)} was
 * called.
 * <p>
 * Shared by {@link Http3Peer} and {@link ProtocolEchoImpl}, so that the identifiers are unique within this application.
 *
 * @since 4.0.0
 */
@ApplicationScoped
class ConnectionIds {

    private final AtomicInteger counter = new AtomicInteger();
    /** The identifiers of the open connections for which {@link #id(HttpConnection)} was called */
    private final Map<HttpConnection, String> open = new ConcurrentHashMap<>();
    /** The identifiers of the connections from {@link #open} that were closed in the meantime */
    private final Set<String> closed = ConcurrentHashMap.newKeySet();

    /**
     * Registers the given {@code connection} if it was not registered yet.
     *
     * @param connection the connection to identify
     * @return an identifier of the given {@code connection} that is unique within this application and stays the same
     *         as long as the given {@code connection} is open
     * @since 4.0.0
     */
    String id(HttpConnection connection) {
        return open.computeIfAbsent(connection, cn -> {
            final String id = String.valueOf(counter.incrementAndGet());
            cn.closeHandler(v -> {
                open.remove(cn);
                closed.add(id);
            });
            return id;
        });
    }

    /**
     * @return an immutable snapshot of the identifiers of the connections that were registered via
     *         {@link #id(HttpConnection)} and closed since then
     * @since 4.0.0
     */
    Set<String> closed() {
        return Set.copyOf(closed);
    }
}
