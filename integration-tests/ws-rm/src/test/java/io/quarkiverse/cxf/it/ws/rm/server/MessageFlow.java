package io.quarkiverse.cxf.it.ws.rm.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.rm.RMConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class MessageFlow {

    private final String addressingNamespace;
    private final String rmNamespace;
    private final List<Document> outboundMessages = new ArrayList<>();
    private final List<Document> inboundMessages = new ArrayList<>();

    public MessageFlow(List<byte[]> out, List<byte[]> in, String addrns, String rmns) throws Exception {
        addressingNamespace = addrns;
        rmNamespace = rmns;
        for (byte[] bytes : in) {
            inboundMessages.add(StaxUtils.read(new ByteArrayInputStream(bytes)));
        }
        for (byte[] bytes : out) {
            outboundMessages.add(StaxUtils.read(new ByteArrayInputStream(bytes)));
        }
    }

    public Document getMessage(int i, boolean outbound) {
        return outbound ? outboundMessages.get(i) : inboundMessages.get(i);
    }

    public void verifyActions(String[] expectedActions, boolean outbound) throws Exception {

        assertEquals(expectedActions.length, outbound ? outboundMessages.size() : inboundMessages.size());

        for (int i = 0; i < expectedActions.length; i++) {
            Document doc = outbound ? outboundMessages.get(i) : inboundMessages.get(i);
            String action = getAction(doc);
            if (null == expectedActions[i]) {
                assertNull(action,
                        (outbound ? "Outbound " : "Inbound") + " message " + i + " has unexpected action: " + action);
            } else {
                assertEquals(expectedActions[i], action,
                        (outbound ? "Outbound " : "Inbound") + " message " + i
                                + " does not contain expected action header");
            }
        }
    }

    public void verifyActionsIgnoringPartialResponses(String[] expectedActions) throws Exception {
        int j = 0;
        for (int i = 0; i < inboundMessages.size() && j < expectedActions.length; i++) {
            String action = getAction(inboundMessages.get(i));
            if (null == action && emptyBody(inboundMessages.get(i))) {
                continue;
            }
            if (null == expectedActions[j]) {
                assertNull(action, "Inbound message " + i + " has unexpected action: " + action);
            } else {
                assertEquals(expectedActions[j], action, "Inbound message " + i + " has unexpected action: " + action);
            }
            j++;
        }
        if (j < expectedActions.length) {
            fail("Inbound messages do not contain all expected actions.");
        }
    }

    public boolean checkActions(String[] expectedActions, boolean outbound) throws Exception {

        if (expectedActions.length != (outbound ? outboundMessages.size() : inboundMessages.size())) {
            return false;
        }

        for (int i = 0; i < expectedActions.length; i++) {
            String action = outbound ? getAction(outboundMessages.get(i)) : getAction(inboundMessages.get(i));
            if (null == expectedActions[i]) {
                if (action != null) {
                    return false;
                }
            } else {
                if (!expectedActions[i].equals(action)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void verifyAction(String expectedAction,
            int expectedCount,
            boolean outbound,
            boolean exact) throws Exception {
        int messageCount = outbound ? outboundMessages.size() : inboundMessages.size();
        int count = 0;
        for (int i = 0; i < messageCount; i++) {
            String action = outbound ? getAction(outboundMessages.get(i)) : getAction(inboundMessages.get(i));
            if (null == expectedAction) {
                if (action == null) {
                    count++;
                }
            } else {
                if (expectedAction.equals(action)) {
                    count++;
                }
            }
        }
        if (exact) {
            assertEquals(expectedCount,
                    count,
                    "unexpected count for action: " + expectedAction);
        } else {
            assertTrue(
                    expectedCount <= count,
                    "unexpected count for action: " + expectedAction + ": " + count);
        }

    }

    public void verifyMessageNumbers(String[] expectedMessageNumbers, boolean outbound) throws Exception {
        verifyMessageNumbers(expectedMessageNumbers, outbound, true);
    }

    public void verifyMessageNumbers(String[] expectedMessageNumbers,
            boolean outbound,
            boolean exact) throws Exception {

        int actualMessageCount = outbound ? outboundMessages.size() : inboundMessages.size();
        if (exact) {
            assertEquals(expectedMessageNumbers.length, actualMessageCount);
        } else {
            assertTrue(expectedMessageNumbers.length <= actualMessageCount);
        }

        if (exact) {
            for (int i = 0; i < expectedMessageNumbers.length; i++) {
                Document doc = outbound ? outboundMessages.get(i) : inboundMessages.get(i);
                Element e = getSequence(doc);
                if (null == expectedMessageNumbers[i]) {
                    assertNull(e, (outbound ? "Outbound" : "Inbound") + " message " + i
                            + " contains unexpected message number ");
                } else {
                    assertEquals(expectedMessageNumbers[i],
                            getMessageNumber(e), (outbound ? "Outbound" : "Inbound") + " message " + i
                                    + " does not contain expected message number "
                                    + expectedMessageNumbers[i]);
                }
            }
        } else {
            boolean[] matches = new boolean[expectedMessageNumbers.length];
            for (int i = 0; i < actualMessageCount; i++) {
                String messageNumber = null;
                Element e = outbound ? getSequence(outboundMessages.get(i))
                        : getSequence(inboundMessages.get(i));
                messageNumber = null == e ? null : getMessageNumber(e);
                for (int j = 0; j < expectedMessageNumbers.length; j++) {
                    if (messageNumber == null) {
                        if (expectedMessageNumbers[j] == null && !matches[j]) {
                            matches[j] = true;
                            break;
                        }
                    } else {
                        if (messageNumber.equals(expectedMessageNumbers[j]) && !matches[j]) {
                            matches[j] = true;
                            break;
                        }
                    }
                }
            }
            for (int k = 0; k < expectedMessageNumbers.length; k++) {
                assertTrue(matches[k], "no match for message number: " + expectedMessageNumbers[k]);
            }
        }
    }

    public void verifyLastMessage(boolean[] expectedLastMessages,
            boolean outbound) throws Exception {
        verifyLastMessage(expectedLastMessages, outbound, true);
    }

    public void verifyLastMessage(boolean[] expectedLastMessages,
            boolean outbound,
            boolean exact) throws Exception {

        int actualMessageCount = outbound ? outboundMessages.size() : inboundMessages.size();
        if (exact) {
            assertEquals(expectedLastMessages.length, actualMessageCount);
        } else {
            assertTrue(expectedLastMessages.length <= actualMessageCount);
        }

        for (int i = 0; i < expectedLastMessages.length; i++) {
            boolean lastMessage;
            Element e = outbound ? getSequence(outboundMessages.get(i))
                    : getSequence(inboundMessages.get(i));
            lastMessage = null != e && getLastMessage(e);
            assertEquals(expectedLastMessages[i], lastMessage,
                    "Outbound message " + i
                            + (expectedLastMessages[i] ? " does not contain expected last message element."
                                    : " contains last message element."));

        }
    }

    public void verifyAcknowledgements(boolean[] expectedAcks, boolean outbound) throws Exception {
        assertEquals(expectedAcks.length, outbound ? outboundMessages.size()
                : inboundMessages.size());

        for (int i = 0; i < expectedAcks.length; i++) {
            boolean ack = outbound ? (null != getAcknowledgment(outboundMessages.get(i)))
                    : (null != getAcknowledgment(inboundMessages.get(i)));

            if (expectedAcks[i]) {
                assertTrue(ack, (outbound ? "Outbound" : "Inbound") + " message " + i
                        + " does not contain expected acknowledgement");
            } else {
                assertFalse(ack, (outbound ? "Outbound" : "Inbound") + " message " + i
                        + " contains unexpected acknowledgement");
            }
        }
    }

    public void verifyAcknowledgements(int expectedAcks,
            boolean outbound,
            boolean exact) throws Exception {

        int actualMessageCount = outbound ? outboundMessages.size() : inboundMessages.size();
        int ackCount = 0;
        for (int i = 0; i < actualMessageCount; i++) {
            boolean ack = outbound ? (null != getAcknowledgment(outboundMessages.get(i)))
                    : (null != getAcknowledgment(inboundMessages.get(i)));
            if (ack) {
                ackCount++;
            }
        }
        if (exact) {
            assertEquals(expectedAcks, ackCount, "unexpected number of acks");
        } else {
            assertTrue(expectedAcks <= ackCount, "unexpected number of acks: " + ackCount);
        }
    }

    public void verifyAckRequestedOutbound(boolean outbound) throws Exception {
        boolean found = false;
        List<Document> messages = outbound ? outboundMessages : inboundMessages;
        for (Document d : messages) {
            Element se = getAckRequested(d);
            if (se != null) {
                found = true;
                break;
            }
        }
        assertTrue(found, "expected AckRequested");
    }

    public void verifySequenceFault(QName code, boolean outbound, int index) throws Exception {
        Document d = outbound ? outboundMessages.get(index) : inboundMessages.get(index);
        assertNotNull(getRMHeaderElement(d, RMConstants.SEQUENCE_FAULT_NAME));
    }

    public void verifyHeader(QName name, boolean outbound, int index) throws Exception {
        Document d = outbound ? outboundMessages.get(index) : inboundMessages.get(index);
        assertNotNull(getHeaderElement(d, name.getNamespaceURI(), name.getLocalPart()),
                (outbound ? "Outbound" : "Inbound")
                        + " message " + index + " does not have " + name + "header.");
    }

    public void verifyNoHeader(QName name, boolean outbound, int index) throws Exception {
        Document d = outbound ? outboundMessages.get(index) : inboundMessages.get(index);
        assertNull(getHeaderElement(d, name.getNamespaceURI(), name.getLocalPart()),
                (outbound ? "Outbound" : "Inbound")
                        + " message " + index + " has " + name + "header.");
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

    private Element getAckRequested(Document document) throws Exception {
        return getRMHeaderElement(document, RMConstants.ACK_REQUESTED_NAME);
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

    public void verifyAcknowledgementRange(long lower, long upper) throws Exception {
        long currentLower = 0;
        long currentUpper = 0;
        // get the final ack range
        for (Document doc : inboundMessages) {
            Element e = getRMHeaderElement(doc, RMConstants.SEQUENCE_ACK_NAME);
            // let the newer messages take precedence over the older messages in getting the final range
            if (null != e) {
                e = getNamedElement(e, "AcknowledgementRange");
                if (null != e) {
                    currentLower = Long.parseLong(e.getAttribute("Lower"));
                    currentUpper = Long.parseLong(e.getAttribute("Upper"));
                }
            }
        }
        assertEquals(lower, currentLower, "Unexpected acknowledgement lower range");
        assertEquals(upper, currentUpper, "Unexpected acknowledgement upper range");
    }

    // note that this method picks the first match and returns
    public static Element getNamedElement(Element element, String lcname) throws Exception {
        for (Node nd = element.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.ELEMENT_NODE == nd.getNodeType() && lcname.equals(nd.getLocalName())) {
                return (Element) nd;
            }
        }
        return null;
    }

    public void purgePartialResponses() throws Exception {
        for (int i = inboundMessages.size() - 1; i >= 0; i--) {
            if (isPartialResponse(inboundMessages.get(i))) {
                inboundMessages.remove(i);
            }
        }
    }

    public void verifyPartialResponses(int nExpected) throws Exception {
        verifyPartialResponses(nExpected, null);
    }

    public void verifyPartialResponses(int nExpected, boolean[] piggybackedAcks) throws Exception {
        int npr = 0;
        for (int i = 0; i < inboundMessages.size(); i++) {
            if (isPartialResponse(inboundMessages.get(i))) {
                if (piggybackedAcks != null) {
                    Element ack = getAcknowledgment(inboundMessages.get(i));
                    if (piggybackedAcks[npr]) {
                        assertNotNull(ack, "Partial response " + npr + " does not include acknowledgement.");
                    } else {
                        assertNull(ack, "Partial response " + npr + " has unexpected acknowledgement.");
                    }
                }
                npr++;
            }
        }
        assertEquals(nExpected, npr, "Inbound messages did not contain expected number of partial responses.");
    }

    public boolean isPartialResponse(Document d) throws Exception {
        return null == getAction(d) && emptyBody(d);
    }

    public boolean emptyBody(Document d) throws Exception {
        Element envelopeElement = d.getDocumentElement();
        Element bodyElement = null;
        for (Node nd = envelopeElement.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.ELEMENT_NODE == nd.getNodeType() && "Body".equals(nd.getLocalName())) {
                bodyElement = (Element) nd;
                break;
            }
        }
        return !(null != bodyElement && bodyElement.hasChildNodes());
    }

    static String dump(List<byte[]> streams) {
        StringBuilder buf = new StringBuilder();
        try {
            for (int i = 0; i < streams.size(); i++) {
                buf.append(System.getProperty("line.separator"));
                buf.append('[').append(i).append("] : ").append(new String(streams.get(i)));
            }
        } catch (Exception ex) {
            return ex.getMessage();
        }

        return buf.toString();
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
