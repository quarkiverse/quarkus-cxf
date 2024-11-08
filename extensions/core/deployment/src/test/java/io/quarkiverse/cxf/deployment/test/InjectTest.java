package io.quarkiverse.cxf.deployment.test;

import jakarta.annotation.Resource;
import jakarta.annotation.Resources;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceContext;

import org.apache.cxf.interceptor.InInterceptors;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.assertj.core.api.Assertions;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.annotation.CXFEndpoint;
import io.quarkus.test.QuarkusUnitTest;

public class InjectTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloService.class, HelloServiceImpl.class))
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".implementor",
                    HelloServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.client.hello.client-endpoint-url", "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.hello.service-interface", HelloService.class.getName())

            .overrideConfigKey("quarkus.cxf.client.helloCxfEndpoint.client-endpoint-url",
                    "http://localhost:8081/services/helloCxfEndpoint")
            .overrideConfigKey("quarkus.cxf.client.helloCxfEndpoint.service-interface", HelloService.class.getName());

    @CXFClient("hello")
    HelloService helloService;

    @CXFClient("helloCxfEndpoint")
    HelloService helloCxfEndpoint;

    @Inject
    Logger logger;

    @Test
    void inject() {
        assertClient(helloService, "//services/hello");
        assertClient(helloCxfEndpoint, "//services/helloCxfEndpoint");
    }

    static void assertClient(HelloService helloService, String path) {
        Assertions.assertThat(helloService.helloInject("Joe")).isEqualTo("Hello Joe");
        Assertions.assertThat(helloService.helloContextField("foo")).isEqualTo("field bar");

        Assertions.assertThat(helloService.helloContextField("org.apache.cxf.message.Message.BASE_PATH"))
                .isEqualTo("field " + path);
        Assertions.assertThat(helloService.helloContextMethod("org.apache.cxf.message.Message.BASE_PATH"))
                .isEqualTo("method " + path);
        Assertions.assertThat(helloService.helloContextTypeField("org.apache.cxf.message.Message.BASE_PATH"))
                .isEqualTo("type field " + path);
        Assertions.assertThat(helloService.helloContextTypeMethod("org.apache.cxf.message.Message.BASE_PATH"))
                .isEqualTo("type method " + path);

        Assertions.assertThat(helloService.helloContextField(org.apache.cxf.message.Message.CONTENT_TYPE))
                .isEqualTo("field text/xml; charset=UTF-8");

    }

    @WebService
    public interface HelloService {

        @WebMethod
        String helloInject(String person);

        @WebMethod
        String helloContextField(String key);

        @WebMethod
        String helloContextMethod(String key);

        @WebMethod
        String helloContextTypeMethod(String key);

        @WebMethod
        String helloContextTypeField(String key);

        @WebMethod
        String helloContextTypeFieldSingle(String key);
    }

    @WebService(serviceName = "HelloService")
    @Resources({
            @Resource(name = "wsContextTypeField"),
            @Resource(name = "wsContextTypeMethod")
    })
    @Resource(name = "wsContextTypeFieldSingle")
    @InInterceptors(classes = { FooInterceptor.class })
    public static class HelloServiceImpl implements HelloService {

        @Inject
        HelloBean hello;

        @Resource
        WebServiceContext wsContext;

        WebServiceContext wsContextMethod;

        @Resource
        public void setWsContextMethod(WebServiceContext wsContextMethod) {
            this.wsContextMethod = wsContextMethod;
        }

        WebServiceContext wsContextTypeField;

        WebServiceContext wsContextTypeMethod;

        WebServiceContext wsContextTypeFieldSingle;

        public void setWsContextTypeMethod(WebServiceContext wsContextTypeMethod) {
            this.wsContextTypeMethod = wsContextTypeMethod;
        }

        @Override
        public String helloInject(String person) {
            return hello.hello(person);
        }

        @Override
        public String helloContextField(String key) {
            return "field " + String.valueOf(wsContext.getMessageContext().get(key));
        }

        @Override
        public String helloContextMethod(String key) {
            return "method " + String.valueOf(wsContextMethod.getMessageContext().get(key));
        }

        @Override
        public String helloContextTypeField(String key) {
            return "type field " + String.valueOf(wsContextTypeField.getMessageContext().get(key));
        }

        @Override
        public String helloContextTypeMethod(String key) {
            return "type method " + String.valueOf(wsContextTypeMethod.getMessageContext().get(key));
        }

        @Override
        public String helloContextTypeFieldSingle(String key) {
            return "type field single " + String.valueOf(wsContextTypeFieldSingle.getMessageContext().get(key));
        }

    }

    @WebService(serviceName = "HelloService")
    @Resources({
            @Resource(name = "wsContextTypeField"),
            @Resource(name = "wsContextTypeMethod")
    })
    @Resource(name = "wsContextTypeFieldSingle")
    @InInterceptors(classes = { FooInterceptor.class })
    @CXFEndpoint("/helloCxfEndpoint")
    public static class HelloServiceWithCxfEndpoint implements HelloService {

        @Inject
        HelloBean hello;

        @Resource
        WebServiceContext wsContext;

        WebServiceContext wsContextMethod;

        @Resource
        public void setWsContextMethod(WebServiceContext wsContextMethod) {
            this.wsContextMethod = wsContextMethod;
        }

        WebServiceContext wsContextTypeField;

        WebServiceContext wsContextTypeMethod;

        public void setWsContextTypeMethod(WebServiceContext wsContextTypeMethod) {
            this.wsContextTypeMethod = wsContextTypeMethod;
        }

        WebServiceContext wsContextTypeFieldSingle;

        @Override
        public String helloInject(String person) {
            return hello.hello(person);
        }

        @Override
        public String helloContextField(String key) {
            return "field " + String.valueOf(wsContext.getMessageContext().get(key));
        }

        @Override
        public String helloContextMethod(String key) {
            return "method " + String.valueOf(wsContextMethod.getMessageContext().get(key));
        }

        @Override
        public String helloContextTypeField(String key) {
            return "type field " + String.valueOf(wsContextTypeField.getMessageContext().get(key));
        }

        @Override
        public String helloContextTypeMethod(String key) {
            return "type method " + String.valueOf(wsContextTypeMethod.getMessageContext().get(key));
        }

        @Override
        public String helloContextTypeFieldSingle(String key) {
            return "type field single " + String.valueOf(wsContextTypeFieldSingle.getMessageContext().get(key));
        }

    }

    @ApplicationScoped
    public static class HelloBean {
        public String hello(String person) {
            return "Hello " + person;
        }
    }

    public static class FooInterceptor extends AbstractPhaseInterceptor<Message> {

        public FooInterceptor() {
            super(Phase.RECEIVE);
        }

        @Override
        public void handleMessage(final Message message) {
            message.getExchange().put("foo", "bar");
        }
    }
}
