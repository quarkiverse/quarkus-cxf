package io.quarkiverse.cxf.it.large.slow;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.jws.WebService;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.ws.AsyncHandler;
import jakarta.xml.ws.Response;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;

import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.Message;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.quarkiverse.cxf.annotation.CXFEndpoint;
import io.quarkiverse.cxf.it.HelloService;
import io.quarkiverse.cxf.it.large.slow.generated.LargeSlowOutput;
import io.quarkiverse.cxf.it.large.slow.generated.LargeSlowResponse;
import io.quarkiverse.cxf.it.large.slow.generated.LargeSlowService;

@WebService(serviceName = "HelloService", targetNamespace = HelloService.NS)
@CXFEndpoint("/largeSlowSOAPHandler")
public class SOAPHandlerLargeSlowServiceImpl implements LargeSlowService {
    static final QName HEADER_NAME = new QName(HelloService.NS,
            "MyTestHeader");
    static final NamespaceContext NS_CONTEXT = new NSContext("hello", HelloService.NS);

    @Resource
    WebServiceContext wsContext;

    @Override
    public LargeSlowOutput largeSlow(
            int sizeBytes,
            int clientDeserializationDelayMs,
            int serviceExecutionDelayMs) {

        try {
            WrappedMessageContext msgContext = (WrappedMessageContext) wsContext.getMessageContext();
            Message msg = msgContext.getWrappedMessage();
            Node node = msg.getContent(org.w3c.dom.Node.class);
            XPath xPath = XPathFactory.newInstance().newXPath();
            xPath.setNamespaceContext(NS_CONTEXT);
            NodeList nodes = (NodeList) xPath.evaluate("//hello:MyTestHeader", node, XPathConstants.NODESET);
            return new LargeSlowOutput(clientDeserializationDelayMs, String.valueOf(nodes.getLength()));
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Response<LargeSlowResponse> largeSlowAsync(int sizeBytes,
            int clientDeserializationDelayMs,
            int serviceExecutionDelayMs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<?> largeSlowAsync(int sizeBytes,
            int clientDeserializationDelayMs,
            int serviceExecutionDelayMs, AsyncHandler<LargeSlowResponse> asyncHandler) {
        throw new UnsupportedOperationException();
    }

    @ApplicationScoped
    @Named("SOAPHeaderAppender")
    public static class SOAPHeaderAppender implements SOAPHandler<SOAPMessageContext> {

        @Override
        public boolean handleMessage(SOAPMessageContext msgContext) {
            try {
                SOAPHeader header = msgContext.getMessage().getSOAPHeader();
                if (header == null) {
                    header = msgContext.getMessage().getSOAPPart().getEnvelope().addHeader();
                }
                header.addHeaderElement(HEADER_NAME).setTextContent("val");
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

    static class NSContext implements NamespaceContext {

        private final Map<String, String> namespaceMap;

        public NSContext(String prefix, String uri) {
            namespaceMap = Collections.singletonMap(prefix, uri);
        }

        public String getNamespaceURI(String arg0) {
            return namespaceMap.get(arg0);
        }

        public String getPrefix(String arg0) {
            for (String key : namespaceMap.keySet()) {
                String value = namespaceMap.get(key);
                if (value.equals(arg0)) {
                    return key;
                }
            }
            return null;
        }

        public Iterator<String> getPrefixes(String arg0) {
            return namespaceMap.keySet().iterator();
        }
    }
}
