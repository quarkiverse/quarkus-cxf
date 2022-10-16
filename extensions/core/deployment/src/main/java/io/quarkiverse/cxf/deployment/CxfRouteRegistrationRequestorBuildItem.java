package io.quarkiverse.cxf.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Signals to {@link CxfEndpointImplementationProcessor} that the named extension (such as {@code camel-cxf-soap}) will
 * register Service endpoints at run time and thus it wants {@link CxfEndpointImplementationProcessor} to register a
 * Vert.x route for CXF even though no runtime CXF endpoints are discovered.
 */
public final class CxfRouteRegistrationRequestorBuildItem extends MultiBuildItem {

    private final String requestorName;

    /**
     * @param requestorName who requests {@link CxfEndpointImplementationProcessor} to register a Vert.x route for CXF;
     *        typically the name of a feature the given extension produces. This string is used only for logging,
     *        so the name should be informative for human readers in the first place.
     */
    public CxfRouteRegistrationRequestorBuildItem(String requestorName) {
        super();
        this.requestorName = requestorName;
    }

    public String getRequestorName() {
        return requestorName;
    }

}
