package io.quarkiverse.cxf.quarkus.vertx.http.client.deployment;

import io.quarkiverse.cxf.deployment.RuntimeBusCustomizerBuildItem;
import io.quarkiverse.cxf.quarkus.vertx.http.client.QuarkusHttpClientPool;
import io.quarkiverse.cxf.quarkus.vertx.http.client.VertxWebClientRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;

public class VertxWebClientProcessor {

    @BuildStep
    void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(QuarkusHttpClientPool.class));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void customizers(
            VertxWebClientRecorder recorder,
            BuildProducer<RuntimeBusCustomizerBuildItem> customizers) {
        customizers.produce(new RuntimeBusCustomizerBuildItem(recorder.customizeBus()));
    }

}
