package io.quarkiverse.cxf.deployment.test;

import java.util.Collections;
import java.util.Set;

import javax.xml.namespace.QName;

import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class FruitDescriptionAppender implements SOAPHandler<SOAPMessageContext> {

    private final String appendText;

    public FruitDescriptionAppender(String appendText) {
        super();
        this.appendText = appendText;
    }

    static Node find(String localName, NodeList nodes) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (localName.equals(n.getLocalName())) {
                return n;
            } else {
                Node result = find(localName, n.getChildNodes());
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext msgContext) {
        try {
            SOAPEnvelope envelope = msgContext.getMessage().getSOAPPart().getEnvelope();
            SOAPBody body = envelope.getBody();
            Node found = find("description", body.getChildNodes());
            if (found != null) {
                found.setTextContent(found.getTextContent() + appendText);
            }
        } catch (SOAPException ex) {
            throw new WebServiceException(ex);
        }

        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {
    }

    @Override
    public Set<QName> getHeaders() {
        return Collections.emptySet();
    }
}
