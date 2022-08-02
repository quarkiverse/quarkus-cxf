package io.quarkiverse.cxf.transport;

import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.DestinationRegistryImpl;

public class VertxDestinationRegistryFactory {
    public static DestinationRegistry INSTANCE = new DestinationRegistryImpl();

    public static DestinationRegistry getDestinationRegistry() {
        return INSTANCE;
    }
}
