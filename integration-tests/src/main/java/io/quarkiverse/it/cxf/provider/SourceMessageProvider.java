package io.quarkiverse.it.cxf.provider;

import java.io.StringReader;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;

import org.apache.cxf.staxutils.StaxUtils;

@WebServiceProvider
@ServiceMode(value = Service.Mode.MESSAGE)
public class SourceMessageProvider implements Provider<Source> {

    public SourceMessageProvider() {
    }

    public Source invoke(Source request) {
        String payload = StaxUtils.toString(request);
        payload = payload.replace("<text>Hello</text>", "<text>Hello from SourceMessageProvider</text>");
        Source response = new StreamSource(new StringReader(payload));
        return response;
    }
}
