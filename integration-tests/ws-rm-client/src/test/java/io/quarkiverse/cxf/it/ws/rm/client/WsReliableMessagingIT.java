package io.quarkiverse.cxf.it.ws.rm.client;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class WsReliableMessagingIT extends WsReliableMessagingTest {

    @Override
    protected boolean isNative() {
        return true;
    }

}
