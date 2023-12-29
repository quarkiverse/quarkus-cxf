
package io.quarkiverse.cxf.deployment.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * A global logging feature configured in {@code application.properties}.
 */
public class GlobalLoggingConfigurationTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(
                    root -> root.addClasses(HelloService.class, HelloServiceImpl.class))
            .overrideConfigKey("quarkus.cxf.logging.enabled-for", "clients-and-services")
            .overrideConfigKey("quarkus.cxf.logging.pretty", "true")
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".implementor", HelloServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.client.hello.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.hello.client-endpoint-url", "http://localhost:8081/services/hello")

            .overrideConfigKey("quarkus.cxf.endpoint.\"/helloUgly\".implementor", HelloServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.endpoint.\"/helloUgly\".logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.endpoint.\"/helloUgly\".logging.pretty", "false")
            .overrideConfigKey("quarkus.cxf.client.helloUgly.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloUgly.client-endpoint-url", "http://localhost:8081/services/helloUgly")
            .overrideConfigKey("quarkus.cxf.client.helloUgly.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.helloUgly.logging.pretty", "false")

            .overrideConfigKey("quarkus.cxf.endpoint.\"/helloSilent\".implementor", HelloServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.endpoint.\"/helloSilent\".logging.enabled", "false")
            .overrideConfigKey("quarkus.cxf.client.helloSilent.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloSilent.client-endpoint-url",
                    "http://localhost:8081/services/helloSilent")
            .overrideConfigKey("quarkus.cxf.client.helloSilent.logging.enabled", "false")

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
                    .anyMatch(QuarkusCxfClientTestUtil.messageExists("REQ_IN",
                            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                                    + "  <soap:Body>\n"
                                    + "    <ns2:hello xmlns:ns2=\"http://deployment.logging.features.cxf.quarkiverse.io/\">\n"
                                    + "      <arg0>Dolly</arg0>\n"
                                    + "    </ns2:hello>\n"
                                    + "  </soap:Body>\n"
                                    + "</soap:Envelope>\n"))
                    .anyMatch(QuarkusCxfClientTestUtil.messageExists("RESP_OUT",
                            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                                    + "  <soap:Body>\n"
                                    + "    <ns2:helloResponse xmlns:ns2=\"http://deployment.logging.features.cxf.quarkiverse.io/\">\n"
                                    + "      <return>Hello Dolly!</return>\n"
                                    + "    </ns2:helloResponse>\n"
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
                    /* helloUgly has pretty = false */
                    .anyMatch(QuarkusCxfClientTestUtil.messageExists("REQ_OUT",
                            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                                    + "<soap:Body>"
                                    + "<ns2:hello xmlns:ns2=\"http://deployment.logging.features.cxf.quarkiverse.io/\">"
                                    + "<arg0>Joe</arg0>"
                                    + "</ns2:hello>"
                                    + "</soap:Body>"
                                    + "</soap:Envelope>"))
                    .anyMatch(QuarkusCxfClientTestUtil.messageExists("REQ_IN",
                            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                                    + "<soap:Body>"
                                    + "<ns2:hello xmlns:ns2=\"http://deployment.logging.features.cxf.quarkiverse.io/\">"
                                    + "<arg0>Joe</arg0>"
                                    + "</ns2:hello>"
                                    + "</soap:Body>"
                                    + "</soap:Envelope>"))
                    .anyMatch(QuarkusCxfClientTestUtil.messageExists("RESP_OUT",
                            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                                    + "<soap:Body>"
                                    + "<ns2:helloResponse xmlns:ns2=\"http://deployment.logging.features.cxf.quarkiverse.io/\">"
                                    + "<return>Hello Joe!</return>"
                                    + "</ns2:helloResponse>"
                                    + "</soap:Body>"
                                    + "</soap:Envelope>"))
                    .anyMatch(QuarkusCxfClientTestUtil.messageExists("RESP_IN",
                            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                                    + "<soap:Body>"
                                    + "<ns2:helloResponse xmlns:ns2=\"http://deployment.logging.features.cxf.quarkiverse.io/\">"
                                    + "<return>Hello Joe!</return>"
                                    + "</ns2:helloResponse>"
                                    + "</soap:Body>"
                                    + "</soap:Envelope>"))
                    /* There should be no messages logged from the silent client and endpoint */
                    .noneMatch(msg -> Pattern.compile("^.*Darkness.*$", Pattern.DOTALL).matcher(msg).matches())
                    .hasSize(8));

    @CXFClient("hello")
    HelloService helloService;

    @CXFClient("helloUgly")
    HelloService helloUglyService;

    @CXFClient("helloSilent")
    HelloService helloSilentService;

    @Test
    void payloadPrettyLogged() throws IOException {

        helloService.hello("Dolly");
        helloUglyService.hello("Joe");
        helloSilentService.hello("Darkness");

    }

}
