package io.quarkiverse.cxf.it.server;

import java.util.*;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

public class GreetingSOAPHandler implements SOAPHandler<SOAPMessageContext> {

    @Override
    public Set<QName> getHeaders() {
        return Collections.emptySet();
    }

    @Override
    public boolean handleMessage(SOAPMessageContext messageContext) {
        Map<String, List<String>> responseHeaders = new HashMap<>();
        responseHeaders.put("TEST-HEADER-KEY", Collections.singletonList("From SOAP Handler"));
        messageContext.put(MessageContext.HTTP_RESPONSE_HEADERS, responseHeaders);
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {

    }

}
