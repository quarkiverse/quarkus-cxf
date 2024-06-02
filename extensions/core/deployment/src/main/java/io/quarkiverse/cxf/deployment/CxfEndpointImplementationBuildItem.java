package io.quarkiverse.cxf.deployment;

import java.util.Objects;

import io.quarkiverse.cxf.CXFRecorder.BeanLookupStrategy;

/**
 * Holds service endpoint implementation metadata.
 */
public final class CxfEndpointImplementationBuildItem extends AbstractEndpointBuildItem {

    private final String sei;
    private final String implementor;
    private final boolean provider;
    private final String relativePath;
    private final BeanLookupStrategy beanLookupStrategy;

    public CxfEndpointImplementationBuildItem(
            String sei,
            String implementor,
            String soapBinding,
            String wsNamespace,
            String wsName,
            boolean provider,
            String relativePath,
            BeanLookupStrategy beanLookupStrategy) {
        super(soapBinding, wsNamespace, wsName);
        this.sei = sei;
        this.implementor = Objects.requireNonNull(implementor, "implementor cannot be null");
        this.provider = provider;
        this.relativePath = relativePath;
        this.beanLookupStrategy = beanLookupStrategy;
    }

    public String getSei() {
        return sei;
    }

    public String getImplementor() {
        return implementor;
    }

    public boolean isProvider() {
        return provider;
    }

    /**
     * @return the relative path under which this endpoint should be exposed relative to {@code quarkus.cxf.path} or
     *         {@code null], if {@link CXFEndpoint#path()} was
     *         not specified for this endpoint
     */
    public String getRelativePath() {
        return relativePath;
    }

    public BeanLookupStrategy getBeanLookupStrategy() {
        return beanLookupStrategy;
    }

}
