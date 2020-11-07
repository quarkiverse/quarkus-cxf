package io.quarkiverse.cxf.transport;

import java.io.IOException;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transport.http.HttpDestinationFactory;

public class VertxDestinationFactory extends HTTPTransportFactory implements HttpDestinationFactory {

    protected VertxDestinationFactory(List<String> transportIds, DestinationRegistry registry) {
        super(transportIds, registry);
    }

    @Override
    public AbstractHTTPDestination createDestination(EndpointInfo endpointInfo, Bus bus,
            DestinationRegistry destinationRegistry) throws IOException {
        return new VertxDestination(endpointInfo, bus, destinationRegistry);

    }

    @Override
    public Destination getDestination(EndpointInfo endpointInfo, Bus bus) throws IOException {
        if (endpointInfo == null) {
            throw new IllegalArgumentException("EndpointInfo cannot be null");
        }
        synchronized (registry) {
            AbstractHTTPDestination d = registry.getDestinationForPath(endpointInfo.getAddress());
            if (d == null) {
                d = createDestination(endpointInfo, bus, registry);
                registry.addDestination(d);
                configure(bus, d);
                d.finalizeConfig();
            }
            return d;
        }
    }

}
