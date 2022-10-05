package io.quarkiverse.cxf.transport;

import static java.lang.String.format;

import java.io.IOException;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapTransportFactory;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.jboss.logging.Logger;

public class VertxDestinationFactory extends SoapTransportFactory implements DestinationFactory {
    private static final Logger LOGGER = Logger.getLogger(VertxDestinationFactory.class);

    protected final DestinationRegistry registry;

    protected VertxDestinationFactory(DestinationRegistry registry) {
        super();
        this.registry = registry;
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
}
