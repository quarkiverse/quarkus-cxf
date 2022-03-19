package io.quarkiverse.cxf;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class CxfEndpointConfig {

    /**
     * The server class implementor
     */
    @ConfigItem
    public Optional<String> implementor;

    /**
     * The wsdl path
     */
    @ConfigItem(name = "wsdl")
    public Optional<String> wsdlPath;

    /**
     * The URL of the SOAP Binding, should be one of four values:
     * {@code "http://schemas.xmlsoap.org/wsdl/soap/http"} for SOAP11HTTP_BINDING<br/>
     * {@code "http://schemas.xmlsoap.org/wsdl/soap/http?mtom=true"} for SOAP11HTTP_MTOM_BINDING<br/>
     * {@code "http://www.w3.org/2003/05/soap/bindings/HTTP/"} for SOAP12HTTP_BINDING<br/>
     * {@code "http://www.w3.org/2003/05/soap/bindings/HTTP/?mtom=true"} for SOAP12HTTP_MTOM_BINDING<br/>
     */
    @ConfigItem
    public Optional<String> soapBinding;

    /**
     * The server endpoint url
     */
    @ConfigItem
    public Optional<String> publishedEndpointUrl;

    /**
     * The comma-separated list of Feature classes
     */
    @ConfigItem
    public Optional<List<String>> features;

    /**
     * The comma-separated list of Handler classes
     */
    @ConfigItem
    public Optional<List<String>> handlers;

    /**
     * The comma-separated list of InInterceptor classes
     */
    @ConfigItem
    public Optional<List<String>> inInterceptors;

    /**
     * The comma-separated list of OutInterceptor classes
     */
    @ConfigItem
    public Optional<List<String>> outInterceptors;

    /**
     * The comma-separated list of OutFaultInterceptor classes
     */
    @ConfigItem
    public Optional<List<String>> outFaultInterceptors;

    /**
     * The comma-separated list of InFaultInterceptor classes
     */
    @ConfigItem
    public Optional<List<String>> inFaultInterceptors;
}
