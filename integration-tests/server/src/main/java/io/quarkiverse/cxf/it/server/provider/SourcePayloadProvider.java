package io.quarkiverse.cxf.it.server.provider;

import java.io.StringReader;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import jakarta.xml.ws.*;

import org.apache.cxf.staxutils.StaxUtils;

@WebServiceProvider
@ServiceMode(value = Service.Mode.PAYLOAD)
@BindingType(value = "http://cxf.apache.org/bindings/xformat")
public class SourcePayloadProvider implements Provider<DOMSource> {

    public SourcePayloadProvider() {
    }

    @Override
    public DOMSource invoke(DOMSource request) throws WebServiceException {
        try {
            String payload = StaxUtils.toString(request.getNode());
            payload = payload.replace("<text>Hello</text>", "<text>Hello from SourcePayloadProvider</text>");
            return new DOMSource(StaxUtils.read(new StreamSource(new StringReader(payload))));
        } catch (XMLStreamException e) {
            throw new WebServiceException(e);
        }
    }
}
