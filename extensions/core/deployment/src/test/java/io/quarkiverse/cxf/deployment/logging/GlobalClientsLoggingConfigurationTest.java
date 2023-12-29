package io.quarkiverse.cxf.deployment.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * A global logging feature configured in {@code application.properties}.
 */
public class GlobalClientsLoggingConfigurationTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(
                    root -> root.addClasses(HelloService.class, HelloServiceImpl.class))
            .overrideConfigKey("quarkus.cxf.logging.enabled-for", "clients")
            .overrideConfigKey("quarkus.cxf.logging.pretty", "true")
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".implementor", HelloServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.client.hello.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.hello.client-endpoint-url", "http://localhost:8081/services/hello")

            .setLogRecordPredicate(logRecord -> logRecord.getLoggerName().contains("org.apache.cxf.services.HelloService.RE")) // REQ_IN or RESP_OUT
            .assertLogRecords(records -> assertThat(records)
                    .extracting(LogRecord::getMessage)
                    .anyMatch(QuarkusCxfClientTestUtil.messageExists("REQ_OUT",
                            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                                    + "  <soap:Body>\n"
                                    + "    <ns2:hello xmlns:ns2=\"http://deployment.logging.features.cxf.quarkiverse.io/\">\n"
                                    + "      <arg0>Dolly</arg0>\n"
                                    + "    </ns2:hello>\n"
                                    + "  </soap:Body>\n"
                                    + "</soap:Envelope>\n"))
                    .anyMatch(QuarkusCxfClientTestUtil.messageExists("RESP_IN",
                            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                                    + "  <soap:Body>\n"
                                    + "    <ns2:helloResponse xmlns:ns2=\"http://deployment.logging.features.cxf.quarkiverse.io/\">\n"
                                    + "      <return>Hello Dolly!</return>\n"
                                    + "    </ns2:helloResponse>\n"
                                    + "  </soap:Body>\n"
                                    + "</soap:Envelope>\n"))
                    .hasSize(2));

    @CXFClient("hello")
    HelloService helloService;

    @Test
    void payloadPrettyLogged() throws IOException {

        helloService.hello("Dolly");

    }

}