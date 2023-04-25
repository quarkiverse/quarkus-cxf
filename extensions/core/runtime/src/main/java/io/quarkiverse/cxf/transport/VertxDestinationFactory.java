package io.quarkiverse.cxf.transport;

import static java.lang.String.format;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionRegistry;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SOAPBindingUtil;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.jms.interceptor.SoapJMSConstants;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.binding.soap.wsdl.extensions.SoapAddress;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.DestinationRegistryImpl;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.WSDLEndpointFactory;
import org.jboss.logging.Logger;

public class VertxDestinationFactory extends HTTPTransportFactory implements WSDLEndpointFactory {
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

    @Override
    public Set<String> getUriPrefixes() {
        return Collections.unmodifiableSet(URI_PREFIXES);
    }

    public DestinationRegistry getDestinationRegistry() {
        return registry;
    }

    @Override
    public void createPortExtensors(Bus b, EndpointInfo ei, Service service) {
        if (ei.getBinding() instanceof SoapBindingInfo) {
            SoapBindingInfo bi = (SoapBindingInfo) ei.getBinding();
            createSoapExtensors(b, ei, bi.getSoapVersion() instanceof Soap12);
        }
    }

    @Override
    public EndpointInfo createEndpointInfo(Bus bus,
            ServiceInfo serviceInfo,
            BindingInfo b,
            List<?> ees) {
        String transportURI = "http://schemas.xmlsoap.org/wsdl/soap/";
        if (b instanceof SoapBindingInfo) {
            SoapBindingInfo sbi = (SoapBindingInfo) b;
            transportURI = sbi.getTransportURI();
        }
        EndpointInfo info = new SoapEndpointInfo(serviceInfo, transportURI);

        if (ees != null) {
            for (Iterator<?> itr = ees.iterator(); itr.hasNext();) {
                Object extensor = itr.next();

                if (SOAPBindingUtil.isSOAPAddress(extensor)) {
                    final SoapAddress sa = SOAPBindingUtil.getSoapAddress(extensor);

                    info.addExtensor(sa);
                    info.setAddress(sa.getLocationURI());
                    if (isJMSSpecAddress(sa.getLocationURI())) {
                        info.setTransportId(SoapJMSConstants.SOAP_JMS_SPECIFICIATION_TRANSPORTID);
                    }
                } else {
                    info.addExtensor(extensor);
                }
            }
        }

        return info;
    }

    private void createSoapExtensors(Bus bus, EndpointInfo ei, boolean isSoap12) {
        try {

            String address = ei.getAddress();
            if (address == null) {
                address = "http://localhost:9090";
            }

            ExtensionRegistry registry = bus.getExtension(WSDLManager.class).getExtensionRegistry();
            SoapAddress soapAddress = SOAPBindingUtil.createSoapAddress(registry, isSoap12);
            soapAddress.setLocationURI(address);

            ei.addExtensor(soapAddress);

        } catch (WSDLException e) {
            e.printStackTrace();
        }
    }

    private boolean isJMSSpecAddress(String address) {
        return address != null && address.startsWith("jms:") && !"jms://".equals(address);
    }

    private static class SoapEndpointInfo extends EndpointInfo {
        SoapAddress saddress;

        SoapEndpointInfo(ServiceInfo serv, String trans) {
            super(serv, trans);
        }

        @Override
        public void setAddress(String s) {
            super.setAddress(s);
            if (saddress != null) {
                saddress.setLocationURI(s);
            }
        }

        @Override
        public void addExtensor(Object el) {
            super.addExtensor(el);
            if (el instanceof SoapAddress) {
                saddress = (SoapAddress) el;
            }
        }
    }
}
