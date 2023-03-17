package io.quarkiverse.cxf.graal;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;

import io.quarkiverse.cxf.CxfClientProducer;

/**
 *
 */
public class QuarkusCxfFeature implements Feature {
    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
        /*
         * We cannot do this using a RuntimeInitializedPackageBuildItem because it would cause a dependency cycle in
         * io.quarkiverse.cxf.deployment.CxfClientProcessor.collectClients()
         */
        RuntimeClassInitialization.initializeAtRunTime(CxfClientProducer.RUNTIME_INITIALIZED_PROXY_MARKER_INTERFACE_PACKAGE);
    }
}
