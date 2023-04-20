package io.quarkiverse.cxf.it.ws.rm.server;

import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.WSAContextUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.derby.DerbyDatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(DerbyDatabaseTestResource.class)
public class WsReliableMessagingTest {

    @Test
    public void getSourceSequencesSize() throws Exception {

        QName serviceName = new QName("https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-rm",
                "WsrmHelloService");
        Service service = Service.create(new URL(io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil.getServerUrl()
                + "/services/wsrm/HelloWorld?wsdl"),
                serviceName);
        WsrmHelloService proxy = service.getPort(WsrmHelloService.class, new RMStoreFeature());
        String decoupledEndpoint = "/wsrm/decoupled_endpoint";
        Client client = ClientProxy.getClient(proxy);
        HTTPConduit hc = (HTTPConduit) (client.getConduit());
        HTTPClientPolicy cp = hc.getClient();
        cp.setDecoupledEndpoint(decoupledEndpoint);
        client.getBus().setProperty(WSAContextUtils.DECOUPLED_ENDPOINT_BASE_PROPERTY,
                "http://localhost:8081/services");
        final String reply = proxy.sayHello();
        String prefix = "WS-ReliableMessaging Hello World! seqSize: ";
        Assertions.assertThat(reply).startsWith(prefix);
        final String rawSeqSize = reply.substring(prefix.length());
        final int seqSize = Integer.parseInt(rawSeqSize);
        Assertions.assertThat(seqSize).isGreaterThan(0);

    }

}
