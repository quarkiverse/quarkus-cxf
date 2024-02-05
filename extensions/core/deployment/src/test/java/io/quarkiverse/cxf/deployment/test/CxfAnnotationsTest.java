package io.quarkiverse.cxf.deployment.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.WSDLDocumentation;
import org.apache.cxf.annotations.WSDLDocumentationCollection;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ext.logging.Logging;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.feature.Features;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;

public class CxfAnnotationsTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".implementor",
                    HelloServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.client.hello.client-endpoint-url", "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.hello.service-interface", HelloService.class.getName())
            .setLogRecordPredicate(msg -> msg.getLoggerName().startsWith("org.apache.cxf.services.HelloService"))
            .assertLogRecords(logRecords -> {
                List<String> messages = logRecords.stream()
                        .map(LogRecord::getMessage)
                        .collect(Collectors.toList());
                Assertions.assertThat(messages).hasSize(2);
                Assertions.assertThat(messages.get(0)).contains("<arg0>Joe</arg0>");
                Assertions.assertThat(messages.get(1)).contains("<return>Hello Joe</return>");
            });

    private static final String HELLO_SERVICE_IMPL_TOP_LEVEL_DOCS = "HelloServiceImpl top level docs";
    private static final String HELLO_SERVICE_IMPL_BINDING_DOCS = "HelloServiceImpl binding docs";
    private static final String HELLO_METHOD_DOCS = "hello method docs";
    private static final String HELLO_SERVICE_IMPL_DOCS = "HelloServiceImpl docs";

    @CXFClient
    HelloService helloService;

    @Test
    void feature() {
        Assertions.assertThat(helloService.hello("Joe")).isEqualTo("Hello Joe");

        Assertions.assertThat(TestFeatureImpl.initializedBus).isFalse(); // we did not register the feature on the Bus
        Assertions.assertThat(TestFeatureImpl.initializedServerBus).isTrue();
        Assertions.assertThat(TestFeatureImpl.initializedClientBus).isFalse();

        Assertions.assertThat(TestFeatureIntf.initializedBus).isFalse(); // we did not register the feature on the Bus
        Assertions.assertThat(TestFeatureIntf.initializedServerBus).isFalse();
        Assertions.assertThat(TestFeatureIntf.initializedClientBus).isTrue();
    }

    @Test
    void wsdlDocumentation() throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        Response response = RestAssured.given().when().get("/services/hello?wsdl");
        response.then().statusCode(200);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        String wsdl = response.body().asString();
        Document doc = dBuilder.parse(new ByteArrayInputStream(wsdl.getBytes(StandardCharsets.UTF_8)));
        doc.getDocumentElement().normalize();
        XPath xpath = XPathFactory.newInstance().newXPath();

        Assertions.assertThat(
                xpath.compile("/definitions/documentation/text()")
                        .evaluate(doc))
                .isEqualTo(HELLO_SERVICE_IMPL_TOP_LEVEL_DOCS);
        Assertions.assertThat(
                xpath.compile("/definitions/binding/documentation/text()")
                        .evaluate(doc))
                .isEqualTo(HELLO_SERVICE_IMPL_BINDING_DOCS);

        Assertions.assertThat(
                xpath.compile("/definitions/service/documentation/text()")
                        .evaluate(doc))
                .isEqualTo(HELLO_SERVICE_IMPL_DOCS);

        Assertions.assertThat(
                xpath.compile("/definitions/portType/operation/documentation/text()")
                        .evaluate(doc))
                .isEqualTo(HELLO_METHOD_DOCS);
    }

    @WebService
    @Features(classes = { TestFeatureIntf.class })
    public interface HelloService {

        @WebMethod
        @WSDLDocumentation(HELLO_METHOD_DOCS)
        String hello(String person);

    }

    @WebService(serviceName = "HelloService")
    @WSDLDocumentationCollection({
            @WSDLDocumentation(HELLO_SERVICE_IMPL_DOCS),
            @WSDLDocumentation(value = HELLO_SERVICE_IMPL_TOP_LEVEL_DOCS, placement = WSDLDocumentation.Placement.TOP),
            @WSDLDocumentation(value = HELLO_SERVICE_IMPL_BINDING_DOCS, placement = WSDLDocumentation.Placement.BINDING)
    })
    @Features(classes = { TestFeatureImpl.class })
    @Logging
    public static class HelloServiceImpl implements HelloService {

        @Override
        public String hello(String person) {
            return "Hello " + person;
        }
    }

    public static class TestFeatureImpl implements Feature {

        static boolean initializedServerBus;
        static boolean initializedClientBus;
        static boolean initializedBus;

        @Override
        public void initialize(Server server, Bus bus) {
            initializedServerBus = true;
        }

        @Override
        public void initialize(Client client, Bus bus) {
            initializedClientBus = true;
        }

        @Override
        public void initialize(InterceptorProvider interceptorProvider, Bus bus) {
        }

        @Override
        public void initialize(Bus bus) {
            initializedBus = true;
        }

    }

    public static class TestFeatureIntf implements Feature {

        static boolean initializedServerBus;
        static boolean initializedClientBus;
        static boolean initializedBus;

        @Override
        public void initialize(Server server, Bus bus) {
            initializedServerBus = true;
        }

        @Override
        public void initialize(Client client, Bus bus) {
            initializedClientBus = true;
        }

        @Override
        public void initialize(InterceptorProvider interceptorProvider, Bus bus) {
        }

        @Override
        public void initialize(Bus bus) {
            initializedBus = true;
        }

    }
}
