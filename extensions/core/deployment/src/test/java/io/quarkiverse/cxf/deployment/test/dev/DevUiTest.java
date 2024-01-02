package io.quarkiverse.cxf.deployment.test.dev;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.QuarkusDevModeTest;

public class DevUiTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap
                    .create(JavaArchive.class)
                    .addClasses(
                            DevUiStats.class,
                            DevUiStatsImpl.class,
                            DevUiRemoteStatsImpl.class)
                    .addAsResource(applicationProperties(), "application.properties"));

    @Test
    void clientsAndServices() throws InterruptedException {
        final DevUiStats client = getClient(DevUiStats.class, "/services/stats");
        Assertions.assertThat(client.getClientCount()).isEqualTo(2);
        Assertions.assertThat(client.getServiceCount()).isEqualTo(2);

        int i = 0;
        Assertions.assertThat(client.getClient(i++)).isEqualTo(
                "DevUiClientInfo [configKey=, sei=io.quarkiverse.cxf.deployment.test.dev.DevUiStats, address=http://localhost:8080/services/stats, wsdl=null]");
        Assertions.assertThat(client.getClient(i++)).isEqualTo(
                "DevUiClientInfo [configKey=stats, sei=io.quarkiverse.cxf.deployment.test.dev.DevUiStats, address=http://localhost:8080/services/stats, wsdl=null]");

        i = 0;
        Assertions.assertThat(client.getService(i++)).isEqualTo(
                "DevUiServiceInfo [path=/services/remote-stats, implementor=io.quarkiverse.cxf.deployment.test.dev.DevUiRemoteStatsImpl]");
        Assertions.assertThat(client.getService(i++)).isEqualTo(
                "DevUiServiceInfo [path=/services/stats, implementor=io.quarkiverse.cxf.deployment.test.dev.DevUiStatsImpl]");
    }

    public static Asset applicationProperties() {
        Writer writer = new StringWriter();
        Properties props = new Properties();
        props.setProperty("quarkus.cxf.endpoint.\"/stats\".implementor", DevUiStatsImpl.class.getName());
        props.setProperty("quarkus.cxf.endpoint.\"/remote-stats\".implementor", DevUiRemoteStatsImpl.class.getName());
        props.setProperty("quarkus.cxf.client.stats.client-endpoint-url", "http://localhost:8080/services/stats");
        props.setProperty("quarkus.cxf.client.stats.service-interface", DevUiStats.class.getName());
        try {
            props.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new StringAsset(writer.toString());
    }

    public static <T> T getClient(Class<T> serviceInterface, String path) {
        try {
            final String namespace = QuarkusCxfClientTestUtil.getDefaultNameSpace(serviceInterface);
            final URL serviceUrl = new URL("http://localhost:8080" + path + "?wsdl");
            final QName qName = new QName(namespace, serviceInterface.getSimpleName());
            final Service service = Service.create(serviceUrl, qName);
            return service.getPort(serviceInterface);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}
