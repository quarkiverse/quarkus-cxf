package io.quarkiverse.cxf.quarkus.vertx.http.client.deployment;

import java.util.List;

import io.quarkiverse.cxf.vertx.http.client.HttpClientPool;
import io.quarkiverse.cxf.vertx.http.client.HttpClientPool.HttpClientPoolRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;

public class VertxWebClientProcessor {

    @BuildStep
    void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(HttpClientPool.class));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void httpClientCustomizers(
            HttpClientPoolRecorder recorder,
            List<HttpClientCustomizerBuildItem> httpClientCustomizers) {
        for (HttpClientCustomizerBuildItem customizer : httpClientCustomizers) {
            recorder.addHttpClientCustomizer(customizer.getCustomizer());
        }
    }

}
