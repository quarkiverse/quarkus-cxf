package io.quarkiverse.cxf.features.logging.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.QuarkusUnitTest;

public class ConfiglessServiceTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(HelloService.class)
                    .addClass(HelloServiceImpl.class));

    @Test
    public void noConfig() {
        final HelloService client = QuarkusCxfClientTestUtil.getClient(HelloService.class, "/HelloService");
        assertThat(client.hello("Dolly")).isEqualTo("Hello Dolly!");
    }
}
