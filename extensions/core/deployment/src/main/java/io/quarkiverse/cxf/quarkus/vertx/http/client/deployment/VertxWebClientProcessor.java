package io.quarkiverse.cxf.quarkus.vertx.http.client.deployment;

import io.quarkiverse.cxf.vertx.http.client.HttpClientPool;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;

public class VertxWebClientProcessor {

    @BuildStep
    void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(HttpClientPool.class));
    }

}
