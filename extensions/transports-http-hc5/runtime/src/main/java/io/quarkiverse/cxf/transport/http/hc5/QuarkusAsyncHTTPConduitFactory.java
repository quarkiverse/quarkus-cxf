package io.quarkiverse.cxf.transport.http.hc5;

import java.io.IOException;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.asyncclient.hc5.AsyncHTTPConduitFactory;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

/**
 * Returns {@link QuarkusAsyncHTTPConduit}s from {@link #createConduit(Bus, EndpointInfo, EndpointReferenceType)}.
 */
public class QuarkusAsyncHTTPConduitFactory extends AsyncHTTPConduitFactory {

    public QuarkusAsyncHTTPConduitFactory(Bus b) {
        super(b);
    }

    public QuarkusAsyncHTTPConduitFactory(Map<String, Object> conf) {
        super(conf);
    }

    @Override
    public HTTPConduit createConduit(Bus bus, EndpointInfo localInfo,
            EndpointReferenceType target) throws IOException {
        return new QuarkusAsyncHTTPConduit(bus, localInfo, target, this);
    }

}
