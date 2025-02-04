package io.quarkiverse.cxf.deployment.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Predicate;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusUnitTest;

public class PerClientOrServiceLoggingLimitTest {

    private static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
    private static final String REQUEST_BODY_170_CHARS = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns2:echo xmlns:ns2=\"http://logging.deployment.cxf.quarkiverse.io/\"><message>Lorem ipsum ";
    private static final String REQUEST_BODY_180_CHARS = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns2:echo xmlns:ns2=\"http://logging.deployment.cxf.quarkiverse.io/\"><message>Lorem ipsum dolor sit ";
    private static final String RESPONSE_BODY_180_CHARS = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns2:echoResponse xmlns:ns2=\"http://logging.deployment.cxf.quarkiverse.io/\"><return>Lorem ipsum dol";
    private static final String RESPONSE_BODY_170_CHARS = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns2:echoResponse xmlns:ns2=\"http://logging.deployment.cxf.quarkiverse.io/\"><return>Lorem";
    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(EchoService.class, EchoServiceImpl.class))

            /* Per client or service limit */
            .overrideConfigKey("quarkus.cxf.endpoint.\"/echo\".implementor", EchoServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.endpoint.\"/echo\".logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.endpoint.\"/echo\".logging.limit", "180")

            .overrideConfigKey("quarkus.cxf.client.echo.client-endpoint-url", "http://localhost:8081/services/echo")
            .overrideConfigKey("quarkus.cxf.client.echo.service-interface", EchoService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.echo.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.echo.logging.limit", "170")

            .setLogRecordPredicate(logRecord -> logRecord.getLoggerName().contains("EchoService.RE")) // REQ_[IN|OUT] or RESP_[IN|OUT]
            .assertLogRecords(records ->

            {

                assertThat(records)
                        .extracting(LogRecord::getMessage)
                        .anyMatch(messageExists("REQ_OUT", REQUEST_BODY_170_CHARS))
                        .anyMatch(messageExists("REQ_IN", REQUEST_BODY_180_CHARS))
                        .anyMatch(messageExists("RESP_OUT", RESPONSE_BODY_180_CHARS))
                        .anyMatch(messageExists("RESP_IN", RESPONSE_BODY_170_CHARS))
                        .hasSize(4);
            });

    ;

    static Predicate<String> messageExists(String messageKind, String payloadSubstring) {
        return msg -> Pattern.compile(
                "^" + messageKind + ".*Payload: .*" + payloadSubstring + "\n$",
                Pattern.DOTALL).matcher(msg).matches();
    }

    @CXFClient("echo")
    EchoService echo;

    @Test
    void limit() {
        Assertions.assertThat(echo.echo(LOREM_IPSUM)).isEqualTo(LOREM_IPSUM);

        Assertions.assertThat(REQUEST_BODY_170_CHARS).hasSize(170);
        Assertions.assertThat(REQUEST_BODY_180_CHARS).hasSize(180);
        Assertions.assertThat(RESPONSE_BODY_170_CHARS).hasSize(170);
        Assertions.assertThat(RESPONSE_BODY_180_CHARS).hasSize(180);
    }

    @WebService
    public interface EchoService {

        @WebMethod
        String echo(@WebParam(name = "message") String person);

    }

    @WebService(serviceName = "EchoService")
    public static class EchoServiceImpl implements EchoService {

        @Override
        public String echo(@WebParam(name = "message") String message) {
            return message;
        }
    }

}
