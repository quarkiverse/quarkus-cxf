package io.quarkiverse.cxf.vertx.http.client;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.annotation.Resource;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.handler.MessageContext;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KeepAliveTimeoutTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(EchoHeadersService.class, EchoHeadersServiceImpl.class))
            .overrideConfigKey("quarkus.cxf.endpoint.\"/echoHeaders\".implementor", EchoHeadersServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.endpoint.\"/echoHeaders\".logging.enabled", "pretty")

            .overrideConfigKey("quarkus.cxf.client.keepAliveClient.client-endpoint-url",
                    "http://localhost:8081/services/echoHeaders")
            .overrideConfigKey("quarkus.cxf.client.keepAliveClient.service-interface", EchoHeadersService.class.getName())
            /* This scenario works only with vert.x client */
            .overrideConfigKey("quarkus.cxf.client.keepAliveClient.http-conduit-factory", "VertxHttpClientHTTPConduitFactory")
            .overrideConfigKey("quarkus.cxf.client.keepAliveClient.connection", "keep-alive")
            .overrideConfigKey("quarkus.cxf.client.keepAliveClient.vertx.keep-alive-timeout", "123")

            .overrideConfigKey("quarkus.cxf.client.closeClient.client-endpoint-url",
                    "http://localhost:8081/services/echoHeaders")
            .overrideConfigKey("quarkus.cxf.client.closeClient.service-interface", EchoHeadersService.class.getName())
            /* This scenario works only with vert.x client */
            .overrideConfigKey("quarkus.cxf.client.closeClient.http-conduit-factory", "VertxHttpClientHTTPConduitFactory")
            .overrideConfigKey("quarkus.cxf.client.closeClient.connection", "close");

    @CXFClient("keepAliveClient")
    EchoHeadersService keepAliveClient;

    @CXFClient("closeClient")
    EchoHeadersService closeClient;

    @Test
    void keepAliveTimeout() {
        Assertions.assertThat(
                        keepAliveClient.getRequestHeaders("connection"))
                .isEqualTo("connection:" + ConnectionType.KEEP_ALIVE.value());
    }

    @Test
    void closeClient() {
        Assertions.assertThat(
                        closeClient.getRequestHeaders("connection"))
                .isEqualTo("connection:" + ConnectionType.CLOSE.value());
    }

    @WebService
    public interface EchoHeadersService {

        @WebMethod
        String getRequestHeaders(String keys);

    }

    @WebService(serviceName = "EchoHeaders")
    public static class EchoHeadersServiceImpl implements EchoHeadersService {

        @Resource
        WebServiceContext wsContext;

        @Override
        public String getRequestHeaders(String keys) {
            HttpServletRequest req = (HttpServletRequest) wsContext.getMessageContext().get(MessageContext.SERVLET_REQUEST);

            return Stream.of(keys.split(","))
                    .map(k -> k + ":" + req.getHeader(k))
                    .collect(Collectors.joining(","));
        }
    }
}
