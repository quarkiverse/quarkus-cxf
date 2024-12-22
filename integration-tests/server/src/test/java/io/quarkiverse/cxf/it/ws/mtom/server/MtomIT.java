package io.quarkiverse.cxf.it.ws.mtom.server;

import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.http.HTTPConduitFactory;

import io.quarkiverse.cxf.URLConnectionHTTPConduitFactory;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class MtomIT extends MtomTest {

    public MtomIT() {
        /*
         * quarkus.cxf.http-conduit-factory = URLConnectionHTTPConduitFactory is not effective for the JVM
         * running the native tests. Thus we have to set the ConduitFactory manually.
         */
        BusFactory.getDefaultBus().setExtension(new URLConnectionHTTPConduitFactory(), HTTPConduitFactory.class);
    }
}
