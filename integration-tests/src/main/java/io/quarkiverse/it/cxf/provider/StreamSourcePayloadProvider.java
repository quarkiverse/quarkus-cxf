package io.quarkiverse.it.cxf.provider;

import java.io.StringReader;

import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingType;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;

import org.apache.cxf.staxutils.StaxUtils;

@WebServiceProvider
@ServiceMode(value = Service.Mode.PAYLOAD)
@BindingType(value = "http://cxf.apache.org/bindings/xformat")
public class StreamSourcePayloadProvider implements Provider<StreamSource> {

    public StreamSourcePayloadProvider() {
    }

    public StreamSource invoke(StreamSource request) {
        String payload = StaxUtils.toString(request);
        payload = payload.replace("<text>Hello</text>", "<text>Hello from StreamSourcePayloadProvider</text>");
        StreamSource response = new StreamSource(new StringReader(payload));
        return response;
    }
}
