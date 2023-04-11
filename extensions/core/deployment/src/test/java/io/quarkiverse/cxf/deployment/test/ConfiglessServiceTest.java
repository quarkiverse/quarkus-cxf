package io.quarkiverse.cxf.deployment.test;

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
                    .addClass(FruitWebService.class)
                    .addClass(FruitWebServiceImpl.class)
                    .addClass(Fruit.class)
                    .addClass(Add.class)
                    .addClass(Delete.class));

    @Test
    public void noConfig() {
        final FruitWebService client = QuarkusCxfClientTestUtil.getClient(FruitWebService.class, "/services/FruitWebService");
        client.add(new Fruit("Kiwi", "Yummy"));
        assertThat(client.getDescriptionByName("Kiwi")).isEqualTo("Yummy");
    }
}
