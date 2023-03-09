package io.quarkiverse.cxf.features.logging.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusUnitTest;

/**
 * {@code LoggingFeature} set in {@code application.properties} and an unnamed bean is produced via
 * {@link NamedLoggingFeatureProducer}
 */
public class UnnamedLoggingFeatureTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(
                    root -> root.addClasses(HelloService.class, HelloServiceImpl.class, NamedLoggingFeatureProducer.class))
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".implementor", HelloServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".features", "org.apache.cxf.ext.logging.LoggingFeature")
            .overrideConfigKey("quarkus.cxf.client.hello.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.hello.client-endpoint-url", "http://localhost:8081/hello")
            // Workaround for https://github.com/quarkusio/quarkus/issues/31646
            // Should not be needed with the Quarkus release coming after 3.0.0.Alpha5
            .overrideConfigKey("quarkus.jaxb.validate-jaxb-context", "false")
            .setLogRecordPredicate(logRecord -> logRecord.getLoggerName().contains("org.apache.cxf.services.HelloService.RE")) // REQ_IN or RESP_OUT
            .assertLogRecords(records -> assertThat(records)
                    .extracting(LogRecord::getMessage)
                    .anyMatch(msg -> msg.contains(
                            "Payload: <soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                                    + "  <soap:Body>\n"
                                    + "    <ns1:hello xmlns:ns1=\"http://deployment.logging.features.cxf.quarkiverse.io/\">\n"
                                    + "      <arg0>Dolly</arg0>\n"
                                    + "    </ns1:hello>\n"
                                    + "  </soap:Body>\n"
                                    + "</soap:Envelope>"))
                    .anyMatch(msg -> msg.contains(
                            "Payload: <soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                                    + "  <soap:Body>\n"
                                    + "    <ns1:helloResponse xmlns:ns1=\"http://deployment.logging.features.cxf.quarkiverse.io/\">\n"
                                    + "      <return>Hello Dolly!</return>\n"
                                    + "    </ns1:helloResponse>\n"
                                    + "  </soap:Body>\n"
                                    + "</soap:Envelope>"))
                    .hasSize(2));

    @CXFClient("hello")
    HelloService helloService;

    @Test
    void payloadPrettyLogged() throws IOException {

        helloService.hello("Dolly");

    }

}
