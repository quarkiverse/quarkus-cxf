package io.quarkiverse.cxf.quarkus.vertx.http.client.deployment;

import java.util.Collection;
import java.util.List;

import io.quarkiverse.cxf.vertx.http.client.HttpClientPool;
import io.quarkiverse.cxf.vertx.http.client.HttpClientPool.HttpClientPoolRecorder;
import io.quarkiverse.cxf.vertx.http.client.QuicSupport;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ArtifactCoords;

public class VertxWebClientProcessor {

    /**
     * {@code quarkus-vertx} removes the QUIC support from Vert.x in native mode, unless this class is available.
     * We require {@code quarkus-http3} in JVM mode too, so that both modes behave the same.
     */
    private static final String QUARKUS_HTTP3_CLASS = "io.quarkus.http3.runtime.Http3Recorder";

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

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void quicSupport(
            HttpClientPoolRecorder recorder,
            CurateOutcomeBuildItem curateOutcome) {
        recorder.setQuicSupport(findQuicSupport(
                QuarkusClassLoader.isClassPresentAtRuntime(QUARKUS_HTTP3_CLASS),
                curateOutcome.getApplicationModel().getRuntimeDependencies()));
    }

    /**
     * @param quarkusHttp3Present {@code true} if {@code io.quarkus:quarkus-http3} is available in the application
     * @param runtimeDependencies the runtime dependencies of the application
     * @return the availability of QUIC in the application
     */
    static QuicSupport findQuicSupport(boolean quarkusHttp3Present,
            Collection<? extends ArtifactCoords> runtimeDependencies) {
        if (!quarkusHttp3Present) {
            return QuicSupport.QUARKUS_HTTP3_MISSING;
        }
        /*
         * The native QUIC libraries are in the netty-codec-native-quic artifacts having a platform specific classifier.
         * quarkus-http3 adds the one matching the build platform through Maven profiles, which does not work with Gradle.
         */
        final boolean nativeLibraryPresent = runtimeDependencies.stream()
                .anyMatch(dep -> "io.netty".equals(dep.getGroupId())
                        && "netty-codec-native-quic".equals(dep.getArtifactId())
                        && dep.getClassifier() != null
                        && !dep.getClassifier().isEmpty());
        return nativeLibraryPresent ? QuicSupport.AVAILABLE : QuicSupport.NATIVE_LIBRARY_MISSING;
    }

}
