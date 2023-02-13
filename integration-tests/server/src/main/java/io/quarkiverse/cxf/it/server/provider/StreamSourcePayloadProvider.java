package io.quarkiverse.cxf.it.server.provider;

import java.io.StringReader;

import javax.xml.transform.stream.StreamSource;

import jakarta.xml.ws.*;

import org.apache.cxf.staxutils.StaxUtils;

@WebServiceProvider
@ServiceMode(value = Service.Mode.PAYLOAD)
@BindingType(value = "http://cxf.apache.org/bindings/xformat")
public class StreamSourcePayloadProvider implements Provider<StreamSource> {

    public StreamSourcePayloadProvider() {
    }

    @Override
    public StreamSource invoke(StreamSource request) {
        String payload = StaxUtils.toString(request);
        payload = payload.replace("<text>Hello</text>", "<text>Hello from StreamSourcePayloadProvider</text>");
        return new StreamSource(new StringReader(payload));
    }
}
