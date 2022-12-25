package io.quarkiverse.cxf.deployment.test;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;

public class CxfServiceTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(FruitWebService.class)
                    .addClass(FruitWebServiceImpl.class)
                    .addClass(Fruit.class))
            .withConfigurationResource("application-cxf-server-test.properties");

    @Test
    public void whenCheckingWsdl() throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        Response response = RestAssured.given().when().get("/fruit?wsdl");
        response.then().statusCode(200);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(response.body().asInputStream());
        doc.getDocumentElement().normalize();
        XPath xpath = XPathFactory.newInstance().newXPath();

        String val = xpath.compile("/definitions/binding/operation[@name='count']/output/@name")
                .evaluate(doc);
        Assertions.assertEquals("countResponse", val);

        val = xpath
                .compile(
                        "/definitions/types/schema/complexType[@name='Fruit']/sequence/element[@name='description']/@type")
                .evaluate(doc);
        Assertions.assertEquals("xs:string", val);

        val = xpath
                .compile(
                        "/definitions/service/port/address/@location")
                .evaluate(doc);
        Assertions.assertEquals("https://io.quarkus-cxf.com/fruit", val);
    }

    @Test
    public void whenUsingCountMethod_thenCorrect()
            throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {
        String xml = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tem=\"http://test.deployment.cxf.quarkiverse.io/\">\n"
                +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <tem:count>\n" +
                "      </tem:count>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";
        String val = "";

        Response response = RestAssured.given().header("Content-Type", "text/xml").and().body(xml).when().post("/fruit");
        response.then().statusCode(200);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(response.body().asInputStream());
        doc.getDocumentElement().normalize();
        XPath xpath = XPathFactory.newInstance().newXPath();
        val = xpath.compile("/Envelope/Body/countResponse/countFruitsResponse").evaluate(doc);
        Assertions.assertEquals("2", val);
    }

    @Test
    public void whenUsingAddMethod_thenCorrect()
            throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {
        String xml = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tem=\"http://test.deployment.cxf.quarkiverse.io/\">\n"
                +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <tem:add>\n" +
                "      <fruit>\n" +
                "      <name>Pineapple</name>\n" +
                "      <description>Tropical fruit</description>\n" +
                "      </fruit>\n" +
                "      </tem:add>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";

        Response response = RestAssured.given().header("Content-Type", "text/xml").and().body(xml).when().post("/fruit");
        response.then().statusCode(200);
    }
}
