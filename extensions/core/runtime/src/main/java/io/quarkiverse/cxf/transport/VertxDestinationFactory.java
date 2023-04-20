package io.quarkiverse.cxf.transport;

import static java.lang.String.format;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapTransportFactory;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.DestinationRegistryImpl;
import org.jboss.logging.Logger;

public class VertxDestinationFactory extends SoapTransportFactory implements DestinationFactory {
    private static final Logger LOGGER = Logger.getLogger(VertxDestinationFactory.class);

    private static final DestinationRegistry registry = new DestinationRegistryImpl();

    private static final Set<String> URI_PREFIXES = new HashSet<>();
    static {
        URI_PREFIXES.add("http://");
        URI_PREFIXES.add("https://");
    }

    /*
     * This is to make Camel Quarkus happy. It would be nice to come up with a prettier solution
     */
    public static void resetRegistry() {
        synchronized (registry) {
            for (String path : new ArrayList<>(registry.getDestinationsPaths())) {
                registry.removeDestination(path);
            }
        }
    }

    public VertxDestinationFactory() {
        super();
    }

    @Override
    public Destination getDestination(EndpointInfo endpointInfo, Bus bus) throws IOException {
        if (endpointInfo == null) {
            throw new IllegalArgumentException("EndpointInfo cannot be null");
        }
        synchronized (registry) {
            String endpointAddress = endpointInfo.getAddress();
            LOGGER.debug(format("Looking for destination for address %s...", endpointAddress));
            AbstractHTTPDestination d = registry.getDestinationForPath(endpointInfo.getAddress());
            if (d == null) {
                LOGGER.debug(format("Creating VertxDestination for address %s...", endpointAddress));
                d = new VertxDestination(endpointInfo, bus, registry);
                registry.addDestination(d);
                d.finalizeConfig();
            }
            LOGGER.debug(format("Destination for address %s is %s", endpointAddress, d));
            return d;
        }
    }

    public Set<String> getUriPrefixes() {
        return URI_PREFIXES;
    }

    public DestinationRegistry getDestinationRegistry() {
        return registry;
    }
}
