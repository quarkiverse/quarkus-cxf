package io.quarkiverse.cxf.it.server.provider;

import java.io.StringReader;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.*;

import org.apache.cxf.staxutils.StaxUtils;

@WebServiceProvider
@ServiceMode(value = Service.Mode.MESSAGE)
public class SOAPMessageProvider implements Provider<SOAPMessage> {

    public SOAPMessageProvider() {
    }

    @Override
    public SOAPMessage invoke(SOAPMessage request) throws WebServiceException {
        try {
            String payload = StaxUtils.toString(request.getSOAPBody().extractContentAsDocument());
            payload = payload.replace("<text>Hello</text>", "<text>Hello from SOAPMessageProvider</text>");

            MessageFactory mf = MessageFactory.newInstance();
            SOAPMessage response = mf.createMessage();
            response.getSOAPBody().addDocument(StaxUtils.read(new StreamSource(new StringReader(payload))));
            response.saveChanges();
            return response;

        } catch (XMLStreamException | SOAPException e) {
            throw new WebServiceException(e);
        }
    }
}
