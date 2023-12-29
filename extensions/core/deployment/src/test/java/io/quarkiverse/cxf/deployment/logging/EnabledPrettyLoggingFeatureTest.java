package io.quarkiverse.cxf.deployment.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusUnitTest;

/**
 * {@code LoggingFeature} set in {@code application.properties} and a named bean is produced via
 * {@link NamedLoggingFeatureProducer}
 */
public class EnabledPrettyLoggingFeatureTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(
                    root -> root.addClasses(HelloService.class, HelloServiceImpl.class))
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".implementor", HelloServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".logging.pretty", "false")
            .overrideConfigKey("quarkus.cxf.client.hello.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.hello.client-endpoint-url", "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.hello.logging.enabled", "pretty")
            .setLogRecordPredicate(logRecord -> logRecord.getLoggerName().contains("org.apache.cxf.services.HelloService.RE")) // REQ_IN or RESP_OUT
            .assertLogRecords(records -> assertThat(records)
                    .extracting(LogRecord::getMessage)
                    .anyMatch(msg -> msg.contains(
                            /* The service in message is not pretty */
                            "Payload: <soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns2:hello xmlns:ns2=\"http://deployment.logging.features.cxf.quarkiverse.io/\"><arg0>Dolly</arg0></ns2:hello></soap:Body></soap:Envelope>"))
                    .anyMatch(msg -> msg.contains(
                            /* The service out message is not pretty */
                            "Payload: <soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns2:helloResponse xmlns:ns2=\"http://deployment.logging.features.cxf.quarkiverse.io/\"><return>Hello Dolly!</return></ns2:helloResponse></soap:Body></soap:Envelope>"))
                    .anyMatch(msg -> msg.contains(
                            "Payload: <soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                                    + "  <soap:Body>\n"
                                    + "    <ns2:hello xmlns:ns2=\"http://deployment.logging.features.cxf.quarkiverse.io/\">\n"
                                    + "      <arg0>Dolly</arg0>\n"
                                    + "    </ns2:hello>\n"
                                    + "  </soap:Body>\n"
                                    + "</soap:Envelope>"))
                    .anyMatch(msg -> msg.contains(
                            "Payload: <soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                                    + "  <soap:Body>\n"
                                    + "    <ns2:helloResponse xmlns:ns2=\"http://deployment.logging.features.cxf.quarkiverse.io/\">\n"
                                    + "      <return>Hello Dolly!</return>\n"
                                    + "    </ns2:helloResponse>\n"
                                    + "  </soap:Body>\n"
                                    + "</soap:Envelope>"))
                    .hasSize(4));

    @CXFClient("hello")
    HelloService helloService;

    @Test
    void payloadPrettyLogged() throws IOException {

        helloService.hello("Dolly");

    }

}
