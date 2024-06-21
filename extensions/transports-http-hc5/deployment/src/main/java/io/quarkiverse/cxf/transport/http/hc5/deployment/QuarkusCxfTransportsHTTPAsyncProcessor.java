package io.quarkiverse.cxf.transport.http.hc5.deployment;

import java.util.Arrays;
import java.util.List;

import io.quarkiverse.cxf.deployment.QuarkusCxfFeature;
import io.quarkiverse.cxf.deployment.RuntimeBusCustomizerBuildItem;
import io.quarkiverse.cxf.transport.http.hc5.Hc5Recorder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

public class QuarkusCxfTransportsHTTPAsyncProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return QuarkusCxfFeature.CXF_RT_TRANSPORTS_HTTP_HC5.asFeature();
    }

    @BuildStep
    List<RuntimeInitializedClassBuildItem> runtimeInitializedClasses() {
        return Arrays.asList(
                new RuntimeInitializedClassBuildItem("org.apache.hc.client5.http.ssl.ConscryptClientTlsStrategy"),
                new RuntimeInitializedClassBuildItem("org.apache.hc.client5.http.impl.auth.NTLMEngineImpl"));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void customizers(
            Hc5Recorder recorder,
            BuildProducer<RuntimeBusCustomizerBuildItem> customizers) {
        customizers.produce(new RuntimeBusCustomizerBuildItem(recorder.customizeBus()));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setHc5Present(io.quarkiverse.cxf.CXFRecorder recorder) {
        recorder.setHc5Present();
    }

}
