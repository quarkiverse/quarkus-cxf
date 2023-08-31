package io.quarkiverse.cxf;

import java.io.IOException;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitFactory;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transport.http.URLConnectionHTTPConduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

public class URLConnectionHTTPConduitFactory implements HTTPConduitFactory {
    @Override
    public HTTPConduit createConduit(HTTPTransportFactory f, Bus bus, EndpointInfo endpointInfo, EndpointReferenceType target)
            throws IOException {
        return new URLConnectionHTTPConduit(bus, endpointInfo, target);
    }
}
