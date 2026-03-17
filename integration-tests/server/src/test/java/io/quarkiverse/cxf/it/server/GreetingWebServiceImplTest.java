package io.quarkiverse.cxf.it.server;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.test.QuarkusCxfTestUtil;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class GreetingWebServiceImplTest extends AbstractGreetingWebServiceTest {

    @BeforeAll
    static void setup() {
        greetingWS = QuarkusCxfTestUtil.getClient(GreetingWebService.class, "/soap/greeting");
    }

    @Test
    void endpointUrl() {
        Assertions.assertThat(QuarkusCxfTestUtil.getEndpointUrl(greetingWS)).endsWith("/soap/greeting");
    }

    @Override
    protected String getServiceInterface() {
        return "GreetingWebService";
    }

}
