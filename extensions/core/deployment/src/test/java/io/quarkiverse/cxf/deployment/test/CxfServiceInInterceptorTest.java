package io.quarkiverse.cxf.deployment.test;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.ws.Service;
import javax.xml.xpath.XPathExpressionException;

import org.apache.cxf.interceptor.InInterceptors;
import org.apache.cxf.interceptor.OutInterceptors;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xml.sax.SAXException;

import io.quarkus.test.QuarkusUnitTest;

public class CxfServiceInInterceptorTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloService.class, HelloServiceImpl.class, AnnotationCounterImplInterceptor.class,
                            AnnotationCounterIntfInterceptor.class, PropertiesCounterInterceptor.class))
            .withConfigurationResource("application-cxf-server-test.properties")
            .overrideConfigKey("quarkus.cxf.endpoint.\"/intercepted\".implementor",
                    HelloServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.endpoint.\"/intercepted\".in-interceptors",
                    PropertiesCounterInterceptor.class.getName());

    @Test
    public void intercepted() throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {

        Assertions.assertThat(AnnotationCounterImplInterceptor.counter.get()).isZero();
        Assertions.assertThat(AnnotationCounterIntfInterceptor.counter.get()).isZero();
        Assertions.assertThat(PropertiesCounterInterceptor.counter.get()).isZero();

        QName serviceName = new QName("http://test.deployment.cxf.quarkiverse.io/", "HelloService");
        Service service = Service
                .create(new URL(io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil.getServerUrl() + "/intercepted?wsdl"),
                        serviceName);
        HelloService proxy = service.getPort(HelloService.class);

        Assertions.assertThat(AnnotationCounterImplInterceptor.counter.get()).isEqualTo(1);
        // the interceptor on the interface seem to have no effect when getting the WSDL
        Assertions.assertThat(AnnotationCounterIntfInterceptor.counter.get()).isZero();
        Assertions.assertThat(PropertiesCounterInterceptor.counter.get()).isEqualTo(1);

        Assertions.assertThat(proxy.sayHi()).isEqualTo("hi");

        Assertions.assertThat(AnnotationCounterImplInterceptor.counter.get()).isEqualTo(2);
        // the interceptor on the interface is called both on client and server, therefore the increment is 2
        Assertions.assertThat(AnnotationCounterIntfInterceptor.counter.get()).isEqualTo(2);
        Assertions.assertThat(PropertiesCounterInterceptor.counter.get()).isEqualTo(2);

    }

    @WebService(targetNamespace = "http://test.deployment.cxf.quarkiverse.io/")
    @OutInterceptors(classes = AnnotationCounterIntfInterceptor.class)
    public interface HelloService {
        @WebMethod
        String sayHi();
    }

    @WebService(serviceName = "HelloService", endpointInterface = "io.quarkiverse.cxf.deployment.test.CxfServiceInInterceptorTest$HelloService", targetNamespace = "http://test.deployment.cxf.quarkiverse.io/")
    @InInterceptors(classes = AnnotationCounterImplInterceptor.class)
    public static class HelloServiceImpl implements HelloService {
        @WebMethod
        @Override
        public String sayHi() {
            return "hi";
        }
    }

    public static class AnnotationCounterImplInterceptor extends AbstractPhaseInterceptor<Message> {
        private static final AtomicInteger counter = new AtomicInteger(0);

        public AnnotationCounterImplInterceptor() {
            super(Phase.RECEIVE);
        }

        @Override
        public void handleMessage(Message message) {
            counter.incrementAndGet();
        }

    }

    public static class AnnotationCounterIntfInterceptor extends AbstractPhaseInterceptor<Message> {
        private static final AtomicInteger counter = new AtomicInteger(0);

        public AnnotationCounterIntfInterceptor() {
            super(Phase.SEND);
        }

        @Override
        public void handleMessage(Message message) {
            counter.incrementAndGet();
        }

    }

    public static class PropertiesCounterInterceptor extends AbstractPhaseInterceptor<Message> {
        private static final AtomicInteger counter = new AtomicInteger(0);

        public PropertiesCounterInterceptor() {
            super(Phase.RECEIVE);
        }

        @Override
        public void handleMessage(Message message) {
            counter.incrementAndGet();
        }

    }

}
