package io.quarkiverse.cxf.deployment.test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.assertj.core.api.Assertions;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Make sure that a bean not injected anywhere, such as {@link ApplicationScopedDescriptionAppender}, which would
 * normally be removed by {@link Arc}, can be injected anyway, because we make it auto-unremovable in
 * {@code QuarkusCxfProcessor.unremovables()}.
 */
public class UnremovableInterceptorBeanTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(FruitWebService.class)
                    .addClass(FruitWebServiceImpl.class)
                    .addClass(Fruit.class)
                    .addClass(Add.class)
                    .addClass(Delete.class)
                    .addClass(ApplicationScopedDescriptionAppender.class))
            .overrideConfigKey("quarkus.cxf.endpoint.\"/fruit\".implementor",
                    "io.quarkiverse.cxf.deployment.test.FruitWebServiceImpl")
            .overrideConfigKey("quarkus.cxf.client.fruitClient.client-endpoint-url",
                    "http://localhost:8081/services/fruit")
            .overrideConfigKey("quarkus.cxf.client.fruitClient.out-interceptors",
                    "io.quarkiverse.cxf.deployment.test.UnremovableInterceptorBeanTest$ApplicationScopedDescriptionAppender")
            .setLogRecordPredicate(lr -> lr.getMessage().contains(">>> Hello from MyBean <<<"))
            .assertLogRecords(lrs -> Assertions.assertThat(lrs.size()).isGreaterThan(0));

    @Inject
    @CXFClient("fruitClient")
    FruitWebService client;

    @Test
    public void unremovableInterceptor() {
        client.add(new Fruit("Pear", "Sweet"));
        Assertions.assertThat(client.getDescriptionByName("Pear")).isEqualTo("Sweet");
    }

    @ApplicationScoped
    public static class MyBean {
        public String getMessage() {
            return "Hello from MyBean";
        }
    }

    @ApplicationScoped
    public static class ApplicationScopedDescriptionAppender extends AbstractSoapInterceptor {
        MyBean bean;

        ApplicationScopedDescriptionAppender() {
            super(Phase.PRE_LOGICAL);
        }

        @Inject
        ApplicationScopedDescriptionAppender(MyBean bean) {
            super(Phase.PRE_LOGICAL);
            this.bean = bean;
        }

        @Override
        public void handleMessage(SoapMessage message) throws Fault {
            Logger.getLogger(getClass()).info(">>> " + bean.getMessage() + " <<<");
        }
    }

}
