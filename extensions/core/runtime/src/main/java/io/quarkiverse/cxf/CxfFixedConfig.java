package io.quarkiverse.cxf;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Quarkus CXF build time configuration options that are also available at runtime but only in read-only mode.
 */
@ConfigMapping(prefix = "quarkus.cxf")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface CxfFixedConfig {

    /**
     * The default path for CXF resources.
     *
     * [NOTE]
     * .Earlier versions
     * ====
     * The default value before Quarkus CXF version 2.0.0 was `/`.
     * ====
     *
     * @asciidoclet
     * @since 1.0.0
     */
    @WithDefault("/services")
    String path();

    /**
     * The size in bytes of the chunks of memory allocated when writing data.
     *
     * This is a very advanced setting that should only be set if you understand exactly how it affects the output IO operations
     * of the application.
     *
     * @asciidoclet
     * @since 2.6.0
     */
    @WithDefault("128")
    int minChunkSize();

    /**
     * The size of the output stream response buffer in bytes. If a response is larger than this and no content-length is
     * provided then the response will be chunked.
     *
     * Larger values may give slight performance increases for large responses, at the expense of more memory usage.
     *
     * @asciidoclet
     * @since 2.6.0
     */
    @WithDefault("8191")
    int outputBufferSize();

    /**
     * Select the `HTTPConduitFactory` implementation for all clients except the ones that override this setting via
     * `quarkus.cxf.client."client-name".http-conduit-factory`.
     *
     * - `QuarkusCXFDefault` (default): since 3.22.0, this value is equivalent with `VertxHttpClientHTTPConduitFactory`.
     * Before 3.22.0, if `io.quarkiverse.cxf:quarkus-cxf-rt-transports-http-hc5` was present in class path,
     * then its `HTTPConduitFactory` implementation was be used.
     * Before 3.16.0, the effective default was `URLConnectionHTTPConduitFactory` rather than
     * `VertxHttpClientHTTPConduitFactory`.
     * `VertxHttpClientHTTPConduitFactory`. Before 3.16.0, the effective default was `URLConnectionHTTPConduitFactory`.
     * - `CXFDefault`: the selection of `HTTPConduitFactory` implementation is left to CXF
     * - `VertxHttpClientHTTPConduitFactory`: *(Experimental)* the `HTTPConduitFactory` for this client will be set to
     * an implementation always returning `io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit`. This will
     * use `io.vertx.core.http.HttpClient` as the underlying HTTP client. Since {quarkus-cxf-project-name} 3.13.0.
     * - `URLConnectionHTTPConduitFactory`: the `HTTPConduitFactory` will be set to an implementation always returning
     * `org.apache.cxf.transport.http.URLConnectionHTTPConduit`. This will use `java.net.HttpURLConnection` as the underlying
     * HTTP client.
     *
     * Historical note: `HttpClientHTTPConduitFactory` was removed in {quarkus-cxf-project-name} 3.22.0
     *
     * @asciidoclet
     * @since 2.3.0
     */
    public Optional<HTTPConduitImpl> httpConduitFactory();

    /**
     * The build time part of the client configuration.
     *
     * @asciidoclet
     */
    @WithName("client")
    @ConfigDocMapKey("client-name")

    public Map<String, ClientFixedConfig> clients();

    @ConfigGroup
    public interface ClientFixedConfig {

        /**
         * The client service interface class name
         *
         * @asciidoclet
         * @since 1.0.0
         */
        public Optional<String> serviceInterface();

        /**
         * Indicates whether this is an alternative proxy client configuration. If true, then this configuration is ignored when
         * configuring a client without annotation `@CXFClient`.
         *
         * @asciidoclet
         * @since 1.0.0
         */
        @WithDefault("false")
        public boolean alternative();

        /**
         * Configuration options related to native mode
         *
         * @asciidoclet
         */
        @WithName("native")
        public NativeClientFixedConfig native_();

        public static ClientFixedConfig createDefault() {
            return new ClientFixedConfig() {

                @Override
                public Optional<String> serviceInterface() {
                    return Optional.empty();
                }

                @Override
                public boolean alternative() {
                    return false;
                }

                @Override
                public NativeClientFixedConfig native_() {
                    return null;
                }
            };
        }
    }

    @ConfigGroup
    public interface NativeClientFixedConfig {

        /**
         * If `true`, the client dynamic proxy class generated by native compiler will be initialized at runtime; otherwise the
         * proxy class will be initialized at build time.
         *
         * Setting this to `true` makes sense if your service endpoint interface references some class initialized at runtime in
         * its method signatures. E.g. Say, your service interface has method `int add(Operands o)` and the `Operands` class was
         * requested to be initialized at runtime. Then, without setting this configuration parameter to `true`, the native
         * compiler will throw an exception saying something like `Classes that should be initialized at run time got
         * initialized during image building: org.acme.Operands ... jdk.proxy<some-number>.$Proxy<some-number> caused
         * initialization of this class`. `jdk.proxy<some-number>.$Proxy<some-number>` is the proxy class generated by the
         * native compiler.
         *
         * @asciidoclet
         * @since 2.0.0
         */
        @WithDefault("false")
        public boolean runtimeInitialized();
    }
}
