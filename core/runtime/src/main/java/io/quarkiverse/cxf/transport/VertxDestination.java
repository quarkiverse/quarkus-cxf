package io.quarkiverse.cxf.transport;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http_jaxws_spi.JAXWSHttpSpiDestination;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

public class VertxDestination extends JAXWSHttpSpiDestination {

    static final Logger LOG = LogUtils.getL7dLogger(VertxDestination.class);

    public VertxDestination(EndpointInfo endpointInfo, Bus bus, DestinationRegistry destinationRegistry) throws IOException {
        super(bus, destinationRegistry, endpointInfo);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public EndpointReferenceType getAddress() {
        return super.getAddress();
    }
}
