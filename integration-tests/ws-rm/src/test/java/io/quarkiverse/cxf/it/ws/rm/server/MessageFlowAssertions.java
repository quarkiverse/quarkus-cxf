package io.quarkiverse.cxf.it.ws.rm.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.rm.RMConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class MessageFlowAssertions {

    private final String addressingNamespace;
    private final String rmNamespace;
    private final List<Document> outboundMessages = new ArrayList<>();
    private final String direction;

    public MessageFlowAssertions(List<byte[]> out, String addrns, String rmns, String direction) throws Exception {
        addressingNamespace = addrns;
        rmNamespace = rmns;
        this.direction = direction;
        for (byte[] bytes : out) {
            outboundMessages.add(StaxUtils.read(new ByteArrayInputStream(bytes)));
        }
    }

    public void verifyActions(String... expectedActions) throws Exception {

        assertEquals(expectedActions.length, outboundMessages.size());

        for (int i = 0; i < expectedActions.length; i++) {
            Document doc = outboundMessages.get(i);
            String action = getAction(doc);
            if (null == expectedActions[i]) {
                assertNull(action, direction + " message " + i + " has unexpected action: " + action);
            } else {
                assertEquals(expectedActions[i], action,
                        direction + " message " + i
                                + " does not contain expected action header");
            }
        }
    }

    public void verifyMessageNumbers(String... expectedMessageNumbers) throws Exception {

        assertEquals(expectedMessageNumbers.length, outboundMessages.size());

        for (int i = 0; i < expectedMessageNumbers.length; i++) {
            Document doc = outboundMessages.get(i);
            Element e = getSequence(doc);
            if (null == expectedMessageNumbers[i]) {
                assertNull(e, direction + " message " + i
                        + " contains unexpected message number ");
            } else {
                assertEquals(expectedMessageNumbers[i],
                        getMessageNumber(e), direction + " message " + i
                                + " does not contain expected message number "
                                + expectedMessageNumbers[i]);
            }
        }

    }

    public void verifyLastMessage(boolean... expectedLastMessages) throws Exception {

        int actualMessageCount = outboundMessages.size();
        assertEquals(expectedLastMessages.length, actualMessageCount);

        for (int i = 0; i < expectedLastMessages.length; i++) {
            boolean lastMessage;
            Element e = getSequence(outboundMessages.get(i));
            lastMessage = null != e && getLastMessage(e);
            assertEquals(expectedLastMessages[i], lastMessage,
                    "Outbound message " + i
                            + (expectedLastMessages[i] ? " does not contain expected last message element."
                                    : " contains last message element."));

        }
    }

    public void verifyAcknowledgements(boolean[] expectedAcks) throws Exception {
        assertEquals(expectedAcks.length, outboundMessages.size());

        for (int i = 0; i < expectedAcks.length; i++) {
            boolean ack = (null != getAcknowledgment(outboundMessages.get(i)));

            if (expectedAcks[i]) {
                assertTrue(ack, direction + " message " + i
                        + " does not contain expected acknowledgement");
            } else {
                assertFalse(ack, direction + " message " + i
                        + " contains unexpected acknowledgement");
            }
        }
    }

    public void verifyAcknowledgements(int expectedAcks) throws Exception {

        int actualMessageCount = outboundMessages.size();
        int ackCount = 0;
        for (int i = 0; i < actualMessageCount; i++) {
            boolean ack = (null != getAcknowledgment(outboundMessages.get(i)));
            if (ack) {
                ackCount++;
            }
        }
        assertEquals(expectedAcks, ackCount, "unexpected number of acks");
    }

    private String getAction(Document document) throws Exception {
        Element e = getHeaderElement(document, addressingNamespace, "Action");
        if (null != e) {
            return getText(e);
        }
        return null;
    }

    protected Element getSequence(Document document) throws Exception {
        return getRMHeaderElement(document, RMConstants.SEQUENCE_NAME);
    }

    public String getMessageNumber(Element elem) throws Exception {
        for (Node nd = elem.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.ELEMENT_NODE == nd.getNodeType() && "MessageNumber".equals(nd.getLocalName())) {
                return getText(nd);
            }
        }
        return null;
    }

    private boolean getLastMessage(Element element) throws Exception {
        for (Node nd = element.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.ELEMENT_NODE == nd.getNodeType() && "LastMessage".equals(nd.getLocalName())) {
                return true;
            }
        }
        return false;
    }

    protected Element getAcknowledgment(Document document) throws Exception {
        return getRMHeaderElement(document, RMConstants.SEQUENCE_ACK_NAME);
    }

    private Element getRMHeaderElement(Document document, String name) throws Exception {
        return getHeaderElement(document, rmNamespace, name);
    }

    private static Element getHeaderElement(Document document, String namespace, String localName)
            throws Exception {
        Element envelopeElement = document.getDocumentElement();
        Element headerElement = null;
        for (Node nd = envelopeElement.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.ELEMENT_NODE == nd.getNodeType() && "Header".equals(nd.getLocalName())) {
                headerElement = (Element) nd;
                break;
            }
        }
        if (null == headerElement) {
            return null;
        }
        for (Node nd = headerElement.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.ELEMENT_NODE != nd.getNodeType()) {
                continue;
            }
            Element element = (Element) nd;
            String ns = element.getNamespaceURI();
            String ln = element.getLocalName();
            if (namespace.equals(ns)
                    && localName.equals(ln)) {
                return element;
            }
        }
        return null;
    }

    public static String getText(Node node) {
        for (Node nd = node.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.TEXT_NODE == nd.getNodeType()) {
                return nd.getNodeValue();
            }
        }
        return null;
    }

}
