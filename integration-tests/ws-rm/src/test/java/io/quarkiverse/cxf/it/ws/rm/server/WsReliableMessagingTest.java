package io.quarkiverse.cxf.it.ws.rm.server;

import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.derby.DerbyDatabaseTestResource;
// import io.quarkus.test.common.QuarkusTestResource;
// import io.quarkus.test.derby.DerbyDatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(DerbyDatabaseTestResource.class)
public class WsReliableMessagingTest {

    @Test
    public void getSourceSequencesSize() throws Exception {

        QName serviceName = new QName("https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-rm",
                "WsrmHelloService");
        Service service = Service
                .create(new URL(io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil.getServerUrl() + "/services/wsrm?wsdl"),
                        serviceName);
        WsrmHelloService proxy = service.getPort(WsrmHelloService.class, new RMStoreFeature());
        final String reply = proxy.sayHello();
        String prefix = "WS-ReliableMessaging Hello World! seqSize: ";
        Assertions.assertThat(reply).startsWith(prefix);
        final String rawSeqSize = reply.substring(prefix.length());
        final int seqSize = Integer.parseInt(rawSeqSize);
        Assertions.assertThat(seqSize).isGreaterThan(0);

    }

}
