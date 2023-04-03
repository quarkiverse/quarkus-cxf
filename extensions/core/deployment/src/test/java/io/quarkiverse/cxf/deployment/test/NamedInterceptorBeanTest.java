package io.quarkiverse.cxf.deployment.test;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusUnitTest;

public class NamedInterceptorBeanTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(FruitWebService.class)
                    .addClass(FruitWebServiceImpl.class)
                    .addClass(Fruit.class)
                    .addClass(Add.class)
                    .addClass(Delete.class)
                    .addClass(FruitDescriptionAppender.class))
            .overrideConfigKey("quarkus.cxf.endpoint.\"/fruit\".implementor",
                    "io.quarkiverse.cxf.deployment.test.FruitWebServiceImpl")
            .overrideConfigKey("quarkus.cxf.endpoint.\"/fruit\".handlers", "#barDescriptionAppender,#fooDescriptionAppender")
            .overrideConfigKey("quarkus.cxf.client.\"fruitClient\".client-endpoint-url", "http://localhost:8081/fruit");

    @Inject
    @CXFClient("fruitClient")
    FruitWebService client;

    @Produces
    @Named("fooDescriptionAppender")
    FruitDescriptionAppender fooDescriptionAppender() {
        return new FruitDescriptionAppender(" Foo");
    }

    @Produces
    @Named("barDescriptionAppender")
    FruitDescriptionAppender barDescriptionAppender() {
        return new FruitDescriptionAppender(" Bar");
    }

    @Test
    public void namedInterceptor() {
        client.add(new Fruit("Pear", "Sweet"));

        Assertions.assertEquals("Sweet Foo Bar", client.getDescriptionByName("Pear"));
    }

}
