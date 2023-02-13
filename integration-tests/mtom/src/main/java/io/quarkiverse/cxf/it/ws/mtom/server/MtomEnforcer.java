package io.quarkiverse.cxf.it.ws.mtom.server;

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

/**
 * Throws an exception if any handled message does not have an XOP attachment
 */
public class MtomEnforcer implements SOAPHandler<SOAPMessageContext> {

    static boolean walk(NodeList nodes) {
        boolean found = false;
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if ("Include".equals(n.getLocalName())) {
                found = true;
                break;
            } else {
                found = walk(n.getChildNodes());
            }
        }

        return found;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext msgContext) {
        try {
            SOAPEnvelope envelope = msgContext.getMessage().getSOAPPart().getEnvelope();
            SOAPBody body = envelope.getBody();
            boolean found = walk(body.getChildNodes());
            if (!found) {
                throw new IllegalStateException("The SOAP message should contain an <xop:Include> element");
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
