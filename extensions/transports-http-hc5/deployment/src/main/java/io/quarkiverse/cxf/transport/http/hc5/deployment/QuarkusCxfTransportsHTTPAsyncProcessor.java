package io.quarkiverse.cxf.transport.http.hc5.deployment;

import java.util.Arrays;
import java.util.List;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

public class QuarkusCxfTransportsHTTPAsyncProcessor {

    @BuildStep
    List<RuntimeInitializedClassBuildItem> runtimeInitializedClasses() {
        return Arrays.asList(
                new RuntimeInitializedClassBuildItem("org.apache.hc.client5.http.ssl.ConscryptClientTlsStrategy"),
                new RuntimeInitializedClassBuildItem("org.apache.hc.client5.http.impl.auth.NTLMEngineImpl"));
    }

}
