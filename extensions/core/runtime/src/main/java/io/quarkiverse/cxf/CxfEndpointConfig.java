package io.quarkiverse.cxf;

import java.util.List;
import java.util.Optional;

import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;

import io.quarkiverse.cxf.LoggingConfig.PerClientOrServiceLoggingConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithName;

@ConfigGroup
public interface CxfEndpointConfig {

    /**
     * The service endpoint implementation class
     *
     * @asciidoclet
     * @since 1.0.0
     */
    public Optional<String> implementor();

    /**
     * The service endpoint WSDL path
     *
     * @asciidoclet
     * @since 1.0.0
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
     * @since 1.0.0
     */
    public Optional<String> soapBinding();

    /**
     * The published service endpoint URL
     *
     * @asciidoclet
     * @since 1.0.0
     */
    public Optional<String> publishedEndpointUrl();

    /**
     * Logging related configuration
     *
     * @asciidoclet
     */
    PerClientOrServiceLoggingConfig logging();

    // The formatter breaks the java snippet
    // @formatter:off
    /**
     * A comma-separated list of fully qualified CXF Feature class names or named CDI beans.
     *
     * Examples:
     *
     * [source,properties]
     * ----
     * quarkus.cxf.endpoint."/hello".features = org.apache.cxf.ext.logging.LoggingFeature
     * quarkus.cxf.endpoint."/fruit".features = #myCustomLoggingFeature
     * ----
     *
     * In the second case, the `++#++myCustomLoggingFeature` bean can be produced as follows:
     *
     * [source,java]
     * ----
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
     * ----
     *
     * @asciidoclet
     * @since 1.0.0
     */
    // @formatter:on
    public Optional<List<String>> features();

    /**
     * The comma-separated list of Handler classes
     *
     * @asciidoclet
     * @since 1.0.0
     */
    public Optional<List<String>> handlers();

    /**
     * The comma-separated list of InInterceptor classes
     *
     * @asciidoclet
     * @since 1.0.0
     */
    public Optional<List<String>> inInterceptors();

    /**
     * The comma-separated list of OutInterceptor classes
     *
     * @asciidoclet
     * @since 1.0.0
     */
    public Optional<List<String>> outInterceptors();

    /**
     * The comma-separated list of OutFaultInterceptor classes
     *
     * @asciidoclet
     * @since 1.0.0
     */
    public Optional<List<String>> outFaultInterceptors();

    /**
     * The comma-separated list of InFaultInterceptor classes
     *
     * @asciidoclet
     * @since 1.0.0
     */
    public Optional<List<String>> inFaultInterceptors();

    /**
     * Select for which messages XML Schema validation should be enabled. If not specified, no XML Schema validation will be
     * enforced unless it is enabled by other means, such as `&#64;org.apache.cxf.annotations.SchemaValidation` or
     * `&#64;org.apache.cxf.annotations.EndpointProperty(key = "schema-validation-enabled", value = "true")` annotations.
     *
     * @since 2.7.0
     * @asciidoclet
     */
    @WithName("schema-validation.enabled-for")
    public Optional<SchemaValidationType> schemaValidationEnabledFor();
}
