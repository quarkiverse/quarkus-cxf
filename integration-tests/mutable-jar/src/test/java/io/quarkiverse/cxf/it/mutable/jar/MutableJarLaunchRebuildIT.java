package io.quarkiverse.cxf.it.mutable.jar;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;

import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.assertj.core.api.Assertions;
import org.cliassured.Await;
import org.cliassured.Await.LineAwait;
import org.cliassured.CliAssured;
import org.cliassured.CommandProcess;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.test.QuarkusCxfTestUtil;

public class MutableJarLaunchRebuildIT {

    private static final String PORT = "8081";

    @Test
    void launchRebuild() {
        CliAssured.java()
                .args(
                        "-Dquarkus.launch.rebuild=true",
                        "-Dquarkus.cxf.path=/reaugmented-path",
                        "-jar", "target/quarkus-app/quarkus-run.jar")
                .then()
                .stdout()
                .log()
                .hasLinesContaining("Quarkus augmentation completed")
                .stderr()
                .log()
                .execute().assertSuccess();

        LineAwait<String> awaitStarted = Await.lineContaining("Installed features: [");
        LineAwait<String> awaitError = Await.lineContaining(
                "Interceptor for {http://jar.mutable.it.cxf.quarkiverse.io/}HelloService#{http://jar.mutable.it.cxf.quarkiverse.io/}hello has thrown exception, unwinding now: org.apache.cxf.binding.soap.SoapFault: Decoupled WS-Addressing ReplyTo (http://localhost:"
                        + PORT
                        + "/replyTo) is not permitted by this server. Enable with quarkus.cxf.endpoint.addressing.decoupled.enabled=true, or configure permitted URI schemes using quarkus.cxf.endpoint.addressing.decoupled.allowed-schemes configuration parameter");
        try (CommandProcess proc = CliAssured.java()
                .args("-Dquarkus.http.port=" + PORT, "-jar", "target/quarkus-app/quarkus-run.jar")
                .then()
                .stdout()
                .await(awaitStarted)
                .await(awaitError)
                .log()
                .stderr()
                .log()
                .start()) {

            awaitStarted.await(Duration.ofSeconds(10));
            HelloService hello = getClient("/reaugmented-path/hello");

            /* A request without addressing should pass */
            Assertions.assertThat(hello.hello("Joe")).isEqualTo("Hello Joe!");

            /*
             * A request with decoupled replyTo must fail and our overridden messages from
             * io.quarkiverse.cxf.QuarkusCxfContextUtils must be emitted
             */
            final AddressingProperties addrProperties = new AddressingProperties();
            final EndpointReferenceType replyTo = new EndpointReferenceType();
            final AttributedURIType replyToURI = new AttributedURIType();
            final String baseURL = "http://localhost:" + PORT;
            replyToURI.setValue(baseURL + "/replyTo");
            replyTo.setAddress(replyToURI);
            addrProperties.setReplyTo(replyTo);

            final String uuid = UUID.randomUUID().toString();
            final AttributedURIType messageId = new AttributedURIType();
            messageId.setValue(uuid);
            addrProperties.setMessageID(messageId);

            final Map<String, Object> requestContext = ((BindingProvider) hello).getRequestContext();
            requestContext.put(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES, addrProperties);

            Assertions.assertThatThrownBy(() -> hello.hello("Joe"))
                    .hasMessageContaining("Decoupled WS-Addressing ReplyTo (http://localhost:" + PORT
                            + "/replyTo) is not permitted by this server. Enable with quarkus.cxf.endpoint.addressing.decoupled.enabled=true, or configure permitted URI schemes using quarkus.cxf.endpoint.addressing.decoupled.allowed-schemes configuration parameter");

            /* Similar has to be logged in the console */
            awaitError.await(Duration.ofSeconds(10));

        }

    }

    static HelloService getClient(String path) {
        try {
            final URL serviceUrl = new URL("http://localhost:" + PORT + path + "?wsdl");
            final QName qName = new QName(QuarkusCxfTestUtil.getTargetNamespace(HelloService.class),
                    HelloService.class.getSimpleName());
            final Service service = Service.create(serviceUrl, qName);
            return service.getPort(HelloService.class);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
