package io.quarkiverse.cxf;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * A class that provides configurable options of a CXF client.
 */
@ConfigGroup
public class CxfClientConfig {

    /**
     * The wsdl path
     */
    @ConfigItem(name = "wsdl")
    public Optional<String> wsdlPath;

    /**
     * The client endpoint url
     */
    @ConfigItem
    public Optional<String> clientEndpointUrl;

    /**
     * The client interface
     */
    @ConfigItem
    public Optional<String> serviceInterface;

    /**
     * The client endpoint namespace
     */
    @ConfigItem
    public Optional<String> endpointNamespace;

    /**
     * The client endpoint name
     */
    @ConfigItem
    public Optional<String> endpointName;

    /**
     * The username for HTTP Basic auth
     */
    @ConfigItem
    public Optional<String> username;

    /**
     * The password for HTTP Basic auth
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * The list of Feature class
     */
    @ConfigItem
    public Optional<List<String>> features;

    /**
     * The comma-separated list of InInterceptor class
     */
    @ConfigItem
    public Optional<List<String>> inInterceptors;

    /**
     * The comma-separated list of OutInterceptor class
     */
    @ConfigItem
    public Optional<List<String>> outInterceptors;

    /**
     * The comma-separated list of OutFaultInterceptor class
     */
    @ConfigItem
    public Optional<List<String>> outFaultInterceptors;

    /**
     * The comma-separated list of InFaultInterceptor class
     */
    @ConfigItem
    public Optional<List<String>> inFaultInterceptors;
}
