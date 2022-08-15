package io.quarkiverse.it.cxf;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class GreetingWebServiceImplTest extends AbstractGreetingWebServiceTest {

    @BeforeAll
    static void setup() {
        greetingWS = ClientTestUtil.getClient(GreetingWebService.class, "/soap/greeting");
    }

    @Test
    void endpointUrl() {
        Assertions.assertThat(ClientTestUtil.getEndpointUrl(greetingWS)).endsWith("/soap/greeting");
    }

    @Override
    protected String getServiceInterface() {
        return "GreetingWebService";
    }

}
