package io.quarkiverse.cxf.it.ws.rm.server;

import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.WSAContextUtils;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.assertj.core.api.Assertions;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.derby.DerbyDatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(DerbyDatabaseTestResource.class)
public class WsReliableMessagingTest {

    private static final Logger LOG = Logger.getLogger(WsReliableMessagingTest.class);

    @Test
    public void getSourceSequencesSize() throws Exception {

        QName serviceName = new QName("https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-rm",
                "WsrmHelloService");
        Service service = Service.create(new URL(io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil.getServerUrl()
                + "/services/wsrm?wsdl"),
                serviceName);
        final LoggingFeature loggingFeature = new LoggingFeature();
        loggingFeature.setPrettyLogging(true);
        WsrmHelloService proxy = service.getPort(
                WsrmHelloService.class,
                new WSAddressingFeature(),
                new RMStoreFeature(),
                loggingFeature);
        String decoupledEndpoint = "/wsrm/decoupled_endpoint";
        Client client = ClientProxy.getClient(proxy);
        client.getOutInterceptors().add(new MessageLossSimulator());
        HTTPConduit hc = (HTTPConduit) (client.getConduit());
        HTTPClientPolicy cp = hc.getClient();
        cp.setDecoupledEndpoint(decoupledEndpoint);
        client.getBus().setProperty(WSAContextUtils.DECOUPLED_ENDPOINT_BASE_PROPERTY,
                "http://localhost:8081/services");

        LOG.info("About to send Joe");
        Assertions.assertThat(seqSize(proxy.sayHello("Joe"), "Joe")).isEqualTo(1);
        LOG.info("Received Joe, about to send Tom");
        Assertions.assertThat(seqSize(proxy.sayHello("Tom"), "Tom")).isEqualTo(2);
        LOG.info("Received Tom, about to send Paul");
        Assertions.assertThat(seqSize(proxy.sayHello("Paul"), "Paul")).isEqualTo(2);
        LOG.info("Received Paul, about to send Max");
        Assertions.assertThat(seqSize(proxy.sayHello("Max"), "Max")).isEqualTo(2);
        LOG.info("Received Max");
    }

    static int seqSize(String reply, String name) {
        String prefix = "WS-ReliableMessaging Hello " + name + "! seqSize: ";
        Assertions.assertThat(reply).startsWith(prefix);
        final String rawSeqSize = reply.substring(prefix.length());
        return Integer.parseInt(rawSeqSize);
    }

}
