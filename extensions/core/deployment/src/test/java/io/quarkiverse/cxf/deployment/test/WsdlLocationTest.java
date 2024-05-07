package io.quarkiverse.cxf.deployment.test;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.Provider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.ServiceMode;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.WebServiceProvider;

import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class WsdlLocationTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloService.class, HelloServiceImpl.class, HelloProvider.class)
                    .addAsResource("wsdllocation/HelloService.wsdl"))
            .overrideConfigKey("quarkus.cxf.path", "/soap")
            .overrideConfigKey("quarkus.cxf.endpoint.\"/wsdllocation\".implementor",
                    HelloServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.endpoint.\"/wsdllocation-provider\".implementor",
                    HelloProvider.class.getName());

    @Test
    public void serviceServingStaticWsdl()
            throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {

        RestAssured.given()
                .header("Content-Type", "text/xml")
                .body("<s11:Envelope xmlns:s11=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                        + "  <s11:Body>\n"
                        + "    <ns1:hello xmlns:ns1=\"http://test.deployment.cxf.quarkiverse.io/\">\n"
                        + "      <helloName>Joe</helloName>\n"
                        + "    </ns1:hello>\n"
                        + "  </s11:Body>\n"
                        + "</s11:Envelope>")
                .post("/soap/wsdllocation/HelloService")
                .then()
                .statusCode(200)
                .body(CoreMatchers.containsString("<return>Hello Joe</return>"));

        RestAssured.get("/soap/wsdllocation/HelloService/?wsdl")
                .then()
                .statusCode(200)
                .body(CoreMatchers.containsString("<!-- my custom comment -->"));
    }

    @Test
    public void providerServingStaticWsdl()
            throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {

        RestAssured.given()
                .header("Content-Type", "text/xml")
                .body("<s11:Envelope xmlns:s11=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                        + "  <s11:Body>\n"
                        + "    <ns1:hello xmlns:ns1=\"http://test.deployment.cxf.quarkiverse.io/\">\n"
                        + "      <helloName>Joe</helloName>\n"
                        + "    </ns1:hello>\n"
                        + "  </s11:Body>\n"
                        + "</s11:Envelope>")
                .post("/soap/wsdllocation-provider")
                .then()
                .statusCode(200)
                .body(CoreMatchers.containsString(
                        "<return>Hello Joe</return>"));

        RestAssured.get("/soap/wsdllocation-provider/?wsdl")
                .then()
                .statusCode(200)
                .body(CoreMatchers.containsString("<!-- my custom comment -->"));

    }

    @WebService(targetNamespace = "http://test.deployment.cxf.quarkiverse.io/")
    public interface HelloService {
        @WebMethod
        String hello(String name);
    }

    @WebService(serviceName = "HelloService", endpointInterface = "io.quarkiverse.cxf.deployment.test.WsdlLocationTest$HelloService", wsdlLocation = "classpath:wsdllocation/HelloService.wsdl", targetNamespace = "http://test.deployment.cxf.quarkiverse.io/")
    public static class HelloServiceImpl implements HelloService {
        @WebMethod
        @Override
        public String hello(String name) {
            return "Hello " + name;
        }
    }

    @WebServiceProvider(wsdlLocation = "classpath:wsdllocation/HelloService.wsdl", serviceName = "HelloService")
    @ServiceMode(value = Service.Mode.MESSAGE)
    public static class HelloProvider implements Provider<SOAPMessage> {

        public HelloProvider() {
        }

        @Override
        public SOAPMessage invoke(SOAPMessage request) throws WebServiceException {
            try {
                final Document doc = request.getSOAPBody().extractContentAsDocument();
                final Element helloElement = doc.getDocumentElement();
                final Element nameElement = (Element) helloElement.getElementsByTagName("helloName").item(0);
                final String name = nameElement.getTextContent();
                doc.renameNode(helloElement, helloElement.getNamespaceURI(), "helloResponse");
                doc.renameNode(nameElement, nameElement.getNamespaceURI(), "return");
                nameElement.setTextContent("Hello " + name);

                MessageFactory mf = MessageFactory.newInstance();
                SOAPMessage response = mf.createMessage();
                response.getSOAPBody().addDocument(doc);
                response.saveChanges();
                return response;

            } catch (SOAPException e) {
                throw new WebServiceException(e);
            }
        }
    }

}
