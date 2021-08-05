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
     * The url of SOAP Binding
     * a list of standard value :
     * https://docs.oracle.com/javase/7/docs/api/constant-values.html#javax.xml.ws.soap.SOAPBinding.SOAP11HTTP_MTOM_BINDING
     */
    @ConfigItem
    public Optional<String> soapBinding;

    /**
     * The server endpoint url
     */
    @ConfigItem
    public Optional<String> publishedEndpointUrl;

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
