package io.quarkiverse.cxf;

import static java.util.stream.Collectors.toList;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.xml.ws.BindingProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.HTTPConduitFactory;
import org.apache.cxf.ws.addressing.WSAContextUtils;
import org.jboss.logging.Logger;

import io.quarkiverse.cxf.CxfClientConfig.Auth;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.logging.LoggingFactoryCustomizer;
import io.quarkiverse.cxf.vertx.http.client.HttpClientPool;
import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit;
import io.vertx.core.Vertx;

/**
 * Base producer class for setting up CXF client proxies and {@link CXFClientInfo}s.
 * <p>
 * A class extending this class is generated for each Service Endpoint Interface (SEI) at build time. Those generated
 * classes delegate the client creation to {@link #loadCxfClient(InjectionPoint, CXFClientData)}.
 * <p>
 * Notice the {@link InjectionPoint} parameter of {@link #loadCxfClient(InjectionPoint, CXFClientData)}. It is used to
 * find the configuration for the specific client by key given in <code>@CXFClient("myClient")</code>.
 */
public abstract class CxfClientProducer {

    private static final Logger LOGGER = Logger.getLogger(CxfClientProducer.class);

    public static final String RUNTIME_INITIALIZED_PROXY_MARKER_INTERFACE_PACKAGE = "io.quarkiverse.cxf.runtime.proxy";
    public static final String RUNTIME_INITIALIZED_PROXY_MARKER_INTERFACE_NAME = "io.quarkiverse.cxf.runtime.proxy.RuntimeInitializedProxyMarker";

    @Inject
    CxfConfig config;

    @Inject
    CxfFixedConfig fixedConfig;

    @Inject
    @Any
    Instance<ClientFactoryCustomizer> customizers;

    @Inject
    HttpClientPool httpClientPool;

    @Inject
    Vertx vertx;

    private LoggingFactoryCustomizer loggingFactoryCustomizer;

    @PostConstruct
    void init() {
        this.loggingFactoryCustomizer = new LoggingFactoryCustomizer(config);
    }

    /**
     * Must be public, otherwise: java.lang.VerifyError: Bad access to protected data in invokevirtual
     */
    public Object loadCxfClient(InjectionPoint ip, CXFClientData meta) {
        return produceCxfClient(selectorCXFClientInfo(config, fixedConfig, ip, meta, vertx));
    }

    /**
     * Called from the <code>{SEI}CxfClientProducer.closeClient(@Disposes @CXFClient {SEI} client)</code> generated in
     * {@code io.quarkiverse.cxf.deployment.CxfClientProcessor.generateCxfClientProducer()}.
     *
     * @param client the CXF client to close
     */
    public void closeCxfClient(Object client) {
        try {
            ((Closeable) client).close();
        } catch (IOException e) {
            throw new RuntimeException("Could not close CXF client " + client.getClass().getName(), e);
        }
    }

    /**
     * Must be public, otherwise: java.lang.VerifyError: Bad access to protected data in invokevirtual
     */
    public CXFClientInfo loadCxfClientInfo(InjectionPoint ip, CXFClientData meta) {
        return selectorCXFClientInfo(config, fixedConfig, ip, meta, vertx);
    }

    /**
     * The main workhorse producing a CXF client proxy.
     *
     * @param cxfClientInfo
     * @return
     */
    private Object produceCxfClient(CXFClientInfo cxfClientInfo) {
        final String sei = cxfClientInfo.getSei();
        Class<?> seiClass;
        try {
            seiClass = Class.forName(sei, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            LOGGER.errorf("WebService interface (client) class %s not found", sei);
            return null;
        }
        Class<?>[] interfaces;
        try {
            interfaces = cxfClientInfo.isProxyClassRuntimeInitialized()
                    ? new Class<?>[] {
                            BindingProvider.class,
                            Closeable.class,
                            Client.class,
                            Class.forName(RUNTIME_INITIALIZED_PROXY_MARKER_INTERFACE_NAME, true,
                                    Thread.currentThread().getContextClassLoader())
                    }
                    : new Class<?>[] {
                            BindingProvider.class,
                            Closeable.class,
                            Client.class
                    };
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load " + RUNTIME_INITIALIZED_PROXY_MARKER_INTERFACE_NAME, e);
        }
        final QuarkusClientFactoryBean quarkusClientFactoryBean = new QuarkusClientFactoryBean(seiClass);
        final QuarkusJaxWsProxyFactoryBean factory = new QuarkusJaxWsProxyFactoryBean(
                quarkusClientFactoryBean,
                vertx,
                cxfClientInfo.getWorkerDispatchTimeout(),
                interfaces);
        final Map<String, Object> props = new LinkedHashMap<>();
        factory.setProperties(props);
        props.put(CXFClientInfo.class.getName(), cxfClientInfo);
        LOGGER.debugf("using servicename {%s}%s", cxfClientInfo.getWsNamespace(), cxfClientInfo.getWsName());
        factory.setServiceName(new QName(cxfClientInfo.getWsNamespace(), cxfClientInfo.getWsName()));
        if (cxfClientInfo.getEpName() != null) {
            factory.setEndpointName(new QName(cxfClientInfo.getEpNamespace(), cxfClientInfo.getEpName()));
        }
        factory.setAddress(cxfClientInfo.getEndpointAddress());
        if (cxfClientInfo.getSoapBinding() != null) {
            factory.setBindingId(cxfClientInfo.getSoapBinding());
        }
        if (cxfClientInfo.getWsdlUrl() != null && !cxfClientInfo.getWsdlUrl().isEmpty()) {
            factory.setWsdlURL(cxfClientInfo.getWsdlUrl());
        }

        final AuthorizationPolicy authorizationPolicy = authorizationPolicy(cxfClientInfo.getAuth());
        if (authorizationPolicy != null && !cxfClientInfo.isSecureWsdlAccess()) {
            props.put(AuthorizationPolicy.class.getName(), authorizationPolicy);
        }

        final String clientString = "client " + cxfClientInfo.getConfigKey();
        CXFRuntimeUtils.addBeans(cxfClientInfo.getFeatures(), "feature", clientString, sei, factory.getFeatures());
        CXFRuntimeUtils.addBeans(cxfClientInfo.getHandlers(), "handler", clientString, sei, factory.getHandlers());
        CXFRuntimeUtils.addBeans(cxfClientInfo.getInInterceptors(), "inInterceptor", clientString, sei,
                factory.getInInterceptors());
        CXFRuntimeUtils.addBeans(cxfClientInfo.getOutInterceptors(), "outInterceptor", clientString, sei,
                factory.getOutInterceptors());
        CXFRuntimeUtils.addBeans(cxfClientInfo.getOutFaultInterceptors(), "outFaultInterceptor", clientString, sei,
                factory.getOutFaultInterceptors());
        CXFRuntimeUtils.addBeans(cxfClientInfo.getInFaultInterceptors(), "inFaultInterceptor", clientString, sei,
                factory.getInFaultInterceptors());

        {
            final String value = cxfClientInfo.getDecoupledEndpointBase();
            if (value != null) {
                props.put(WSAContextUtils.DECOUPLED_ENDPOINT_BASE_PROPERTY, value);
            }
        }
        if (cxfClientInfo.isRedirectRelativeUri()) {
            props.put(VertxHttpClientHTTPConduit.AUTO_REDIRECT_ALLOW_REL_URI, Boolean.TRUE);
        }
        {
            final int value = cxfClientInfo.getMaxSameUri();
            if (value > 0) {
                /* 0 is the deafult that makes no difference in the handling so we can ignore it here */
                props.put(VertxHttpClientHTTPConduit.AUTO_REDIRECT_MAX_SAME_URI_COUNT, value);
            }
        }

        loggingFactoryCustomizer.customize(cxfClientInfo, factory);
        customizers.forEach(customizer -> customizer.customize(cxfClientInfo, factory));

        final Bus bus = BusFactory.getDefaultBus();
        final QuarkusHTTPConduitFactory conduitFactory = new QuarkusHTTPConduitFactory(
                fixedConfig,
                cxfClientInfo,
                authorizationPolicy,
                vertx,
                httpClientPool);
        props.put(HTTPConduitFactory.class.getName(), conduitFactory);
        Object result;
        final HTTPConduitFactory origConduitFactory = bus.getExtension(HTTPConduitFactory.class);
        try {
            /*
             * Workaround for https://github.com/quarkiverse/quarkus-cxf/issues/1264
             * We set the client specific HTTPConduitFactory on the bus temporarily,
             * so that it is honored for getting the WSDL.
             * We assume that no other tread is accessing the HTTPConduitFactory.class extension in parallel
             */
            bus.setExtension(conduitFactory, HTTPConduitFactory.class);

            LOGGER.debug("cxf client loaded for " + sei);
            result = factory.create();
        } finally {
            bus.setExtension(origConduitFactory, HTTPConduitFactory.class);
        }

        final Client client = ClientProxy.getClient(result);
        {
            final SchemaValidationType value = cxfClientInfo.getSchemaValidationEnabledFor();
            if (value != null) {
                client.getEndpoint().getEndpointInfo().setProperty(Message.SCHEMA_VALIDATION_TYPE, value);
            }
        }

        return result;
    }

    private static AuthorizationPolicy authorizationPolicy(Auth authorization) {
        final String username = authorization.username().orElse(null);
        final String type = authorization.scheme().orElse(null);
        final String header = authorization.token().orElse(null);
        if (username != null || type != null || header != null) {
            final String password = authorization.password().orElse(null);
            final AuthorizationPolicy authPolicy = new AuthorizationPolicy();
            authPolicy.setUserName(username);
            if (password != null) {
                authPolicy.setPassword(password);
            }
            authPolicy.setAuthorizationType(type);
            authPolicy.setAuthorization(header);
            return authPolicy;
        }
        return null;
    }

    /**
     * Selects the configuration for the client injected at the given {@link InjectionPoint} and creates a
     * {@link CXFClientInfo} out of it.
     *
     * @param config the runtime configuration
     * @param fixedConfig the build time configuration
     * @param ip the injection point of the client or of its {@link CXFClientInfo}
     * @param meta the build time metadata of the client's Service Endpoint Interface
     * @param vertx the {@link Vertx} instance to pass to the new {@link CXFClientInfo}
     * @return a new {@link CXFClientInfo}
     * @throws IllegalStateException if no configuration can be selected for the given {@link InjectionPoint}
     *         or if the selected configuration is not valid
     */
    protected static CXFClientInfo selectorCXFClientInfo(
            CxfConfig config,
            CxfFixedConfig fixedConfig,
            InjectionPoint ip,
            CXFClientData meta,
            Vertx vertx) {

        final String configKey;
        if (ip.getAnnotated().isAnnotationPresent(CXFClient.class)) {
            final CXFClient anno = ip.getAnnotated().getAnnotation(CXFClient.class);
            configKey = anno.value();
        } else {
            configKey = "";
        }
        return selectorCXFClientInfo(
                config,
                fixedConfig,
                meta,
                configKey,
                /*
                 * FIXME: (review) "or inject the client using @CXFClient("client-name")" reads as if naming the client
                 * was enough, while the named client fails with "client-endpoint-url is not set" in the next step.
                 * Perhaps: "... because there is no quarkus.cxf.client."client-name" having service-interface = <sei>;
                 * add such a configuration or inject the client by name; in both cases
                 * quarkus.cxf.client."client-name".client-endpoint-url has to be set".
                 */
                () -> configKey.isEmpty()
                        ? new IllegalStateException(
                                "Cannot create a client for the service interface " + meta.getSei() + " injected into "
                                        + ip.getMember() + " without a client name,"
                                        + " because there is no quarkus.cxf.client.\"client-name\".service-interface = "
                                        + meta.getSei() + " in the configuration; either add it together with"
                                        + " quarkus.cxf.client.\"client-name\".client-endpoint-url"
                                        + " or inject the client using @" + CXFClient.class.getSimpleName()
                                        + "(\"client-name\")")
                        /*
                         * FIXME: (review) This branch is unreachable since this commit: for a non-empty configKey,
                         * selectClientConfig() never returns an empty Optional, because CxfConfig.clients() is annotated
                         * with @WithDefaults (ClientEndpointUrlRequiredTest.notConfigured() proves it - it gets
                         * "client-endpoint-url is not set" rather than "configuration is missing").
                         * Please remove this branch. If the injection point (ip.getMember()) is worth keeping in the
                         * message for misspelled client names, it would have to be passed to CXFClientInfo's message
                         * in some other way.
                         */
                        : new IllegalStateException(
                                "@" + CXFClient.class.getName() + "(\"" + configKey + "\") is present on " + ip.getMember()
                                        + " but quarkus.cxf.client.\"" + configKey + "\".* configuration is missing"),
                vertx);
    }

    /*
     * FIXME: (review) AGENTS.md, section JavaDoc: "Methods and constructors must have @since <version>".
     * The JavaDoc of both selectorCXFClientInfo() methods was rewritten in this commit without @since.
     * I added @since 2.7.0 here, because this overload was introduced by 697d3008f (2.7.0.CR1);
     * the protected overload above has its current signature since 127f70bc3 (2.4.0) - please add it there too.
     * Question: Dev UI does not call this method anymore, so the protected overload is the only caller.
     * Does it still need to be public and to take exceptionSupplier, which is used only for clients without a name now?
     */
    /**
     * Selects the configuration for the given client using
     * {@link #selectClientConfig(CxfConfig, CxfFixedConfig, String, String)} and creates a {@link CXFClientInfo} out of it.
     *
     * @param config the runtime configuration
     * @param fixedConfig the build time configuration
     * @param meta the build time metadata of the client's Service Endpoint Interface
     * @param configKey the client name, such as {@code client-name} in
     *        {@code quarkus.cxf.client."client-name".client-endpoint-url}; {@code null} or empty if the client was
     *        injected without a name
     * @param exceptionSupplier the source of the exception to throw if no configuration can be selected for the given
     *        client
     * @param vertx the {@link Vertx} instance to pass to the new {@link CXFClientInfo}
     * @return a new {@link CXFClientInfo}
     * @throws IllegalStateException if no configuration can be selected for the given client
     *         or if the selected configuration is not valid
     * @since 2.7.0
     */
    public static CXFClientInfo selectorCXFClientInfo(
            CxfConfig config,
            CxfFixedConfig fixedConfig,
            CXFClientData meta,
            String configKey,
            Supplier<IllegalStateException> exceptionSupplier,
            Vertx vertx) {
        final ClientConfigSelection selection = selectClientConfig(config, fixedConfig, meta.getSei(), configKey)
                .orElseThrow(exceptionSupplier);
        return new CXFClientInfo(meta, config, selection.config(), selection.configKey(), vertx);
    }

    /**
     * Selects the configuration for the given client without validating it.
     * <p>
     * If the given {@code configKey} is neither {@code null} nor empty, then the configuration available under
     * {@code quarkus.cxf.client."configKey"} is selected.
     * That configuration consists of default values, if no runtime option is set for the given {@code configKey}.
     * Otherwise, the only {@code quarkus.cxf.client."client-name"} having {@code service-interface} equal to the given
     * {@code sei} is selected.
     *
     * @param config the runtime configuration
     * @param fixedConfig the build time configuration
     * @param sei the fully qualified class name of the client's Service Endpoint Interface
     * @param configKey the client name, such as {@code client-name} in
     *        {@code quarkus.cxf.client."client-name".client-endpoint-url}; {@code null} or empty if the client was
     *        injected without a name
     * @return the selected configuration or an empty {@link Optional} if there is no configuration for the given
     *         client
     * @throws IllegalStateException if the given {@code configKey} is {@code null} or empty and there are multiple
     *         clients having {@code service-interface} equal to the given {@code sei}
     * @since 4.0.0
     */
    public static Optional<ClientConfigSelection> selectClientConfig(
            CxfConfig config,
            CxfFixedConfig fixedConfig,
            String sei,
            String configKey) {

        if (configKey != null && !configKey.isEmpty()) {
            /*
             * CxfConfig.clients() does not contain the keys of the clients having only build time options, such as
             * service-interface, set. But as it is annotated with @WithDefaults, it returns a configuration with
             * default values for those.
             */
            return Optional.ofNullable(config.getClient(configKey))
                    .map(clientConfig -> new ClientConfigSelection(configKey, clientConfig));
        }

        /* No client name was specified, thus we look for the configuration by the SEI */
        final List<String> keylist = fixedConfig.clients()
                .entrySet()
                .stream()
                .filter(kv -> kv.getValue() != null)
                .filter(kv -> kv.getValue().serviceInterface().isPresent())
                .filter(kv -> kv.getValue().serviceInterface().get().equals(sei))
                .map(Map.Entry::getKey)
                .collect(toList());

        switch (keylist.size()) {
            case 0:
                return Optional.empty();
            case 1:
                return Optional.of(new ClientConfigSelection(keylist.get(0), config.clients().get(keylist.get(0))));
            default:
                throw new IllegalStateException("quarkus.cxf.client.\"client-name\".service-interface = " + sei
                        + " expected once, but found " + keylist.size() + " times in "
                        + keylist.stream().map(k -> "quarkus.cxf.client.\"" + k + "\".service-interface")
                                .collect(Collectors.joining(", "))
                        + "; either inject the client using an explicit client name, e.g. @CXFClient(\"client-name\"),"
                        + " or keep that service-interface only in one of those configurations");
        }
    }

    /**
     * A client name together with the configuration available under that name.
     *
     * @param configKey the client name, such as {@code client-name} in
     *        {@code quarkus.cxf.client."client-name".client-endpoint-url}; never {@code null} or empty
     * @param config the configuration available under {@code quarkus.cxf.client."configKey"}
     * @since 4.0.0
     */
    public record ClientConfigSelection(String configKey, CxfClientConfig config) {
        /*
         * FIXME: (review) AGENTS.md, section Coding: "Clear, unambiguous contracts".
         * The JavaDoc promises "never null or empty", but nothing enforces it on this public record.
         * Suggested compact constructor (needs import java.util.Objects):
         *
         * public ClientConfigSelection {
         * Objects.requireNonNull(config, "config");
         * if (configKey == null || configKey.isEmpty()) {
         * throw new IllegalArgumentException("configKey must be neither null nor empty");
         * }
         * }
         */
    }

    public interface ClientFactoryCustomizer {
        void customize(CXFClientInfo cxfClientInfo, JaxWsProxyFactoryBean factory);
    }
}
