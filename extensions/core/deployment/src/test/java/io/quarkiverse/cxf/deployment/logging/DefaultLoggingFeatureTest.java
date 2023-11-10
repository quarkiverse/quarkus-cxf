package io.quarkiverse.cxf.deployment.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusUnitTest;

/**
 * {@code LoggingFeature} set in {@code application.properties} but no bean is produced
 */
public class DefaultLoggingFeatureTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(HelloService.class, HelloServiceImpl.class))
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".implementor", HelloServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".features", "org.apache.cxf.ext.logging.LoggingFeature")
            .overrideConfigKey("quarkus.cxf.client.hello.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.hello.client-endpoint-url", "http://localhost:8081/services/hello")
            .setLogRecordPredicate(logRecord -> logRecord.getLoggerName().contains("org.apache.cxf.services.HelloService.RE")) // REQ_IN or RESP_OUT
            .assertLogRecords(records -> assertThat(records)
                    .extracting(LogRecord::getMessage)
                    .anyMatch(msg -> msg.contains(
                            "Payload: <soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns2:hello xmlns:ns2=\"http://deployment.logging.features.cxf.quarkiverse.io/\"><arg0>Dolly</arg0></ns2:hello></soap:Body></soap:Envelope>"))
                    .anyMatch(msg -> msg.contains(
                            "Payload: <soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns2:helloResponse xmlns:ns2=\"http://deployment.logging.features.cxf.quarkiverse.io/\"><return>Hello Dolly!</return></ns2:helloResponse></soap:Body></soap:Envelope>"))
                    .hasSize(2));

    @CXFClient("hello")
    HelloService helloService;

    @Test
    void payloadLogged() throws IOException {

        helloService.hello("Dolly");

    }

}
