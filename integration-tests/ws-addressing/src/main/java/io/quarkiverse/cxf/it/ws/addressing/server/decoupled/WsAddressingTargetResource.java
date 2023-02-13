package io.quarkiverse.cxf.it.ws.addressing.server.decoupled;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

@Path("/ws-addressing-target")
public class WsAddressingTargetResource {

    private final Map<String, String> replyToMessages = new ConcurrentHashMap<>();

    @GET
    @Path("/replyTo/{messageId}")
    public String get(@PathParam("messageId") String messageId) {
        return replyToMessages.get(messageId);
    }

    @POST
    @Path("/replyTo")
    public Response replyTo(String message) {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        MessageIdExtractor messageIdExtractor;
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            SAXParser saxParser = factory.newSAXParser();
            messageIdExtractor = new MessageIdExtractor();
            saxParser.parse(new InputSource(new StringReader(message)), messageIdExtractor);
            replyToMessages.put(messageIdExtractor.relatesTo, message);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
        return Response.ok().build();
    }

    static class MessageIdExtractor extends DefaultHandler {

        private final StringBuilder chars = new StringBuilder();
        private String relatesTo;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            chars.setLength(0);
        }

        @Override
        public void endElement(String uri, String localName, String qName)
                throws SAXException {
            if ("RelatesTo".equals(localName)) {
                relatesTo = chars.toString();
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            chars.append(ch, start, length);
        }
    }

}
