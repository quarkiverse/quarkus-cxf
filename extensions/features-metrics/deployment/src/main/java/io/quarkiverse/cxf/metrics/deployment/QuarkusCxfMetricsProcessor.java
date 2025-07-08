package io.quarkiverse.cxf.metrics.deployment;

import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.feature.DelegatingFeature;
import org.apache.cxf.metrics.MetricsFeature;
import org.apache.cxf.metrics.codahale.CodahaleMetricsProvider;

import io.quarkiverse.cxf.metrics.CxfMetricsRecorder;
import io.quarkiverse.cxf.metrics.MetricsCustomizer;
import io.quarkiverse.cxf.metrics.QuarkusCxfMetricsFeature;
import io.quarkiverse.cxf.quarkus.vertx.http.client.deployment.HttpClientCustomizerBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

public class QuarkusCxfMetricsProcessor {
    private static final String FEATURE = "cxf-rt-features-metrics";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerMetricsReflectionItems(BuildProducer<ReflectiveClassBuildItem> reflectiveItems) {
        reflectiveItems.produce(ReflectiveClassBuildItem.builder(
                QuarkusCxfMetricsFeature.class.getName(),
                AbstractPortableFeature.class.getName(),
                MetricsFeature.class.getName(),
                DelegatingFeature.class.getName(),
                MetricsFeature.Portable.class.getName()).methods().build());
    }

    @BuildStep
    void runtimeInitializedClass(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(
                /*
                 * We do not support Dropwizard Metrics.
                 * We'd actually prefer exclusing the class from the native image analysis,
                 * but that's currently not possible - see https://github.com/oracle/graal/issues/3225
                 */
                CodahaleMetricsProvider.class.getName()));
    }

    @BuildStep
    void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(
                new AdditionalBeanBuildItem(MetricsCustomizer.class));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void httpClientCustomizers(
            CxfMetricsRecorder recorder,
            BuildProducer<HttpClientCustomizerBuildItem> httpClientCustomizers) {
        httpClientCustomizers.produce(new HttpClientCustomizerBuildItem(recorder.httpClientCustomizer()));
    }

}
