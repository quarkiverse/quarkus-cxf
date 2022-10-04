package io.quarkiverse.cxf.transport;

import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.DestinationRegistryImpl;

public class VertxDestinationRegistryFactory {
    public static volatile DestinationRegistry INSTANCE = null;

    public static DestinationRegistry getDestinationRegistry() {
        if (INSTANCE == null) {
            INSTANCE = new DestinationRegistryImpl();
        }
        return INSTANCE;
    }

    public static void reset() {
        INSTANCE = null;
    }
}
