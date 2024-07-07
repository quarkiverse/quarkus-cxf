package io.quarkiverse.cxf.it.server;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class GreetingWebServiceWithWebServiceContextTest {
    @Test
    void context() {
        GreetingWebService client = QuarkusCxfClientTestUtil.getClient(GreetingWebService.class,
                "/soap/greeting-with-web-service-context");
        Assertions.assertThat(client.reply("org.apache.cxf.message.Message.BASE_PATH"))
                .isEqualTo("//soap/greeting-with-web-service-context");
    }

}
