package io.quarkiverse.cxf.ws.rm;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.cxf.ws.rm.feature.RMFeature;
import org.apache.cxf.ws.rm.persistence.RMStore;

import io.quarkiverse.cxf.CXFRuntimeUtils;
import io.quarkus.arc.Unremovable;

@ApplicationScoped
@Unremovable
public class DefaultRmFeatureProducer {

    public static final String DEFAULT_RM_FEATURE_NAME = "defaultRmFeature";
    public static final String DEFAULT_RM_FEATURE_REF = "#defaultRmFeature";

    @Inject
    CxfWsRmConfig config;

    @Produces
    @ApplicationScoped
    @Named(DEFAULT_RM_FEATURE_NAME)
    RMFeature rmFeature() {
        final RMFeature rmFeature = new RMFeature();
        config.rm().store().ifPresent(storeRef -> {
            final RMStore store = CXFRuntimeUtils.getInstance((String) storeRef, true);
            rmFeature.setStore(store);
        });
        return rmFeature;
    }

}
