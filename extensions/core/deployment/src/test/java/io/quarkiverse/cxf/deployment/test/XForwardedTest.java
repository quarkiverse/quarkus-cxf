package io.quarkiverse.cxf.deployment.test;

import java.util.List;
import java.util.Map;

import jakarta.annotation.Resource;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.soap.Addressing;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusUnitTest;

public class XForwardedTest {
    private static final String X_FORWARDED_PROTO_HEADER = "X-Forwarded-Proto";
    private static final String X_FORWARDED_PREFIX_HEADER = "X-Forwarded-Prefix";
    private static final String X_FORWARDED_HOST_HEADER = "X-Forwarded-Host";
    private static final String X_FORWARDED_PORT_HEADER = "X-Forwarded-Port";

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MessageContextService.class, MessageContextServiceImpl.class))
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".implementor", MessageContextServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".logging.enabled", "pretty")

            .overrideConfigKey("quarkus.cxf.client.hello.client-endpoint-url", "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.hello.service-interface", MessageContextService.class.getName())

            .overrideConfigKey("quarkus.http.proxy.proxy-address-forwarding", "true")
            .overrideConfigKey("quarkus.http.proxy.enable-forwarded-host", "true")
            .overrideConfigKey("quarkus.http.proxy.enable-forwarded-prefix", "true");

    @CXFClient("hello")
    MessageContextService ctxService;

    private String getRequestFieldValue(String headerName, String headerValue, String contextKey) {
        return setHeaders(headerName, headerValue).getRequestFieldValue(contextKey);
    }

    private String getMessageContextValue(String headerName, String headerValue, String contextKey) {
        return setHeaders(headerName, headerValue).getMessageContextValue(contextKey);
    }

    private MessageContextService setHeaders(String headerName, String headerValue) {
        final Map<String, List<String>> headers = headerName != null ? Map.of(headerName, List.of(headerValue)) : Map.of();
        ((BindingProvider) ctxService).getRequestContext().put(MessageContext.HTTP_REQUEST_HEADERS, headers);
        return ctxService;
    }

    @Test
    void xForwardedProto() {
        Assertions.assertThat(
                getRequestFieldValue(X_FORWARDED_PROTO_HEADER, "https", "getRequestURL"))
                .isEqualTo("https://localhost/services/hello");
        Assertions.assertThat(
                getRequestFieldValue(X_FORWARDED_PROTO_HEADER, "https", "isSecure"))
                .isEqualTo("true");
    }

    @Test
    void noHeader() {
        Assertions.assertThat(
                getRequestFieldValue(null, null, "getRequestURL"))
                .isEqualTo("http://localhost:8081/services/hello");
        Assertions.assertThat(
                getRequestFieldValue(null, null, "isSecure"))
                .isEqualTo("false");
        Assertions.assertThat(
                getRequestFieldValue(null, null, "getServerName"))
                .isEqualTo("localhost");
        Assertions.assertThat(
                getRequestFieldValue(null, null, "getServerPort"))
                .isEqualTo("8081");
        Assertions.assertThat(
                getRequestFieldValue(null, null, "getRequestURI"))
                .isEqualTo("/services/hello");
        Assertions.assertThat(
                getRequestFieldValue(null, null, "getServletPath"))
                .isEqualTo("/services");
        Assertions.assertThat(
                getRequestFieldValue(null, null, "getContextPath"))
                .isEqualTo("/");
    }

    @Test
    void xForwardedPort() {
        Assertions.assertThat(
                getRequestFieldValue(X_FORWARDED_PORT_HEADER, "12345", "getRequestURL"))
                .isEqualTo("http://localhost:12345/services/hello");
        Assertions.assertThat(
                getRequestFieldValue(X_FORWARDED_PORT_HEADER, "12345", "getServerPort"))
                .isEqualTo("12345");
    }

    @Test
    void xForwardedHost() {
        Assertions.assertThat(
                getRequestFieldValue(X_FORWARDED_HOST_HEADER, "foo", "getRequestURL"))
                .isEqualTo("http://foo/services/hello");
        Assertions.assertThat(
                getRequestFieldValue(X_FORWARDED_HOST_HEADER, "foo", "getServerName"))
                .isEqualTo("foo");
    }

    @Test
    void xForwardedPrefix() {
        Assertions.assertThat(
                getRequestFieldValue(X_FORWARDED_PREFIX_HEADER, "/prefix", "getRequestURL"))
                .isEqualTo("http://localhost:8081/prefix/services/hello");
        Assertions.assertThat(
                getRequestFieldValue(X_FORWARDED_PREFIX_HEADER, "/prefix", "getRequestURI"))
                .isEqualTo("/prefix/services/hello");
        Assertions.assertThat(
                getRequestFieldValue(X_FORWARDED_PREFIX_HEADER, "/prefix", "getContextPath"))
                .isEqualTo("/");
        Assertions.assertThat(
                getRequestFieldValue(X_FORWARDED_PREFIX_HEADER, "/prefix", "getServletPath"))
                .isEqualTo("/services");
        Assertions.assertThat(
                getRequestFieldValue(null, null, "getRequestURL"))
                .isEqualTo("http://localhost:8081/services/hello");
    }

    @WebService
    @Addressing(required = true)
    public interface MessageContextService {

        @WebMethod
        String getRequestFieldValue(String key);

        @WebMethod
        String getMessageContextValue(String key);

    }

    @WebService(serviceName = "HelloService")
    public static class MessageContextServiceImpl implements MessageContextService {

        @Resource
        WebServiceContext wsContext;

        @Override
        public String getRequestFieldValue(String key) {
            HttpServletRequest req = (HttpServletRequest) wsContext.getMessageContext().get(MessageContext.SERVLET_REQUEST);

            return switch (key) {
                case "isSecure": {
                    yield String.valueOf(req.isSecure());
                }
                case "getRemoteAddr": {
                    yield req.getRemoteAddr();
                }
                case "getRequestURL": {
                    yield req.getRequestURL().toString();
                }
                case "getRequestURI": {
                    yield req.getRequestURI();
                }
                case "getContextPath": {
                    yield req.getContextPath();
                }
                case "getServletPath": {
                    yield req.getServletPath();
                }
                case "getServerPort": {
                    yield String.valueOf(req.getServerPort());
                }
                case "getServerName": {
                    yield req.getServerName();
                }
                default:
                    throw new IllegalArgumentException(
                            "Unexpected getter name for a " + HttpServletRequest.class.getName() + ": " + key);
            };
        }

        @Override
        public String getMessageContextValue(String key) {
            return String.valueOf(wsContext.getMessageContext().get(key));
        }
    }

}
