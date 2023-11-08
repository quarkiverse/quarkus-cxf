package io.quarkiverse.cxf.transport;

import static java.lang.String.format;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapTransportFactory;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.DestinationRegistryImpl;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.wsdl11.WSDLEndpointFactory;
import org.jboss.logging.Logger;

public class VertxDestinationFactory extends HTTPTransportFactory implements WSDLEndpointFactory {
    private static final Logger LOGGER = Logger.getLogger(VertxDestinationFactory.class);

    private static final DestinationRegistry registry = new DestinationRegistryImpl();

    private final SoapTransportFactory soapTransportFactory;

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
        this.soapTransportFactory = new SoapTransportFactory();
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

    public DestinationRegistry getDestinationRegistry() {
        return registry;
    }

    @Override
    public void createPortExtensors(Bus b, EndpointInfo ei, Service service) {
        soapTransportFactory.createPortExtensors(b, ei, service);
    }

    @Override
    public EndpointInfo createEndpointInfo(Bus bus,
            ServiceInfo serviceInfo,
            BindingInfo b,
            List<?> ees) {
        return soapTransportFactory.createEndpointInfo(bus, serviceInfo, b, ees);
    }

}
