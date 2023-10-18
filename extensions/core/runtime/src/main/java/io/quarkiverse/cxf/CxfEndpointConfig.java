package io.quarkiverse.cxf;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithName;

@ConfigGroup
public interface CxfEndpointConfig {

    /**
     * The service endpoint implementation class
     */
    public Optional<String> implementor();

    /**
     * The service endpoint WSDL path
     */
    @WithName("wsdl")
    public Optional<String> wsdlPath();

    /**
     * The URL of the SOAP Binding, should be one of four values:
     *
     * * `+http://schemas.xmlsoap.org/wsdl/soap/http+` for SOAP11HTTP_BINDING
     * * `+http://schemas.xmlsoap.org/wsdl/soap/http?mtom=true+` for SOAP11HTTP_MTOM_BINDING
     * * `+http://www.w3.org/2003/05/soap/bindings/HTTP/+` for SOAP12HTTP_BINDING
     * * `+http://www.w3.org/2003/05/soap/bindings/HTTP/?mtom=true+` for SOAP12HTTP_MTOM_BINDING
     *
     * @asciidoclet
     */
    public Optional<String> soapBinding();

    /**
     * The published service endpoint URL
     */
    public Optional<String> publishedEndpointUrl();

    /**
     * A comma-separated list of fully qualified CXF Feature class names or named CDI beans.
     * <p>
     * Examples:
     *
     * <pre>
     * quarkus.cxf.endpoint."/hello".features = org.apache.cxf.ext.logging.LoggingFeature
     * quarkus.cxf.endpoint."/fruit".features = #myCustomLoggingFeature
     * </pre>
     *
     * In the second case, the {@code #myCustomLoggingFeature} bean can be produced as follows:
     *
     * <pre>
     * import org.apache.cxf.ext.logging.LoggingFeature;
     * import javax.enterprise.context.ApplicationScoped;
     * import javax.enterprise.inject.Produces;
     *
     * class Producers {
     *
     *     &#64;Produces
     *     &#64;ApplicationScoped
     *     LoggingFeature myCustomLoggingFeature() {
     *         LoggingFeature loggingFeature = new LoggingFeature();
     *         loggingFeature.setPrettyLogging(true);
     *         return loggingFeature;
     *     }
     * }
     * </pre>
     * <p>
     * Note that the {@code LoggingFeature} is available through the
     * <a href="../extensions/quarkus-cxf-rt-features-logging.html">Logging
     * Feature</a> extension.
     */
    public Optional<List<String>> features();

    /**
     * The comma-separated list of Handler classes
     */
    public Optional<List<String>> handlers();

    /**
     * The comma-separated list of InInterceptor classes
     */
    public Optional<List<String>> inInterceptors();

    /**
     * The comma-separated list of OutInterceptor classes
     */
    public Optional<List<String>> outInterceptors();

    /**
     * The comma-separated list of OutFaultInterceptor classes
     */
    public Optional<List<String>> outFaultInterceptors();

    /**
     * The comma-separated list of InFaultInterceptor classes
     */
    public Optional<List<String>> inFaultInterceptors();
}
