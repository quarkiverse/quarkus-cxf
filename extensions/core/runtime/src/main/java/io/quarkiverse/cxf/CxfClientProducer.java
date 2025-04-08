package io.quarkiverse.cxf;

import static java.util.stream.Collectors.toList;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

        final String clientString = "client"
                + (cxfClientInfo.getConfigKey() != null ? (" " + cxfClientInfo.getConfigKey()) : "");
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
        final HTTPConduitSpec origConduitImpl = bus.getExtension(HTTPConduitSpec.class);
        final QuarkusHTTPConduitFactory conduitFactory = new QuarkusHTTPConduitFactory(
                fixedConfig,
                cxfClientInfo,
                origConduitImpl,
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
     * Calculates the client info to use for producing a JAXWS client proxy.
     *
     * @param cxfConfig The current configuration
     * @param ip Meta information about where injection of client proxy takes place
     * @param meta The default to return
     * @return not null
     */
    protected static CXFClientInfo selectorCXFClientInfo(
            CxfConfig config,
            CxfFixedConfig fixedConfig,
            InjectionPoint ip,
            CXFClientData meta,
            Vertx vertx) {

        // If injection point is annotated with @CXFClient then determine a
        // configuration by looking up annotated config value:

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
                () -> new IllegalStateException(
                        "quarkus.cxf.client.\"" + configKey + "\" is referenced in " + ip.getMember()
                                + " but no such build time configuration entry exists"),
                vertx);
    }

    public static CXFClientInfo selectorCXFClientInfo(
            CxfConfig config,
            CxfFixedConfig fixedConfig,
            CXFClientData meta,
            String configKey,
            Supplier<IllegalStateException> exceptionSupplier,
            Vertx vertx) {

        // If injection point is annotated with @CXFClient then determine a
        // configuration by looking up annotated config value:

        if (configKey != null && !configKey.isEmpty()) {
            if (config.isClientPresent(configKey)) {
                return new CXFClientInfo(meta, config, config.getClient(configKey), configKey, vertx);
            }
            // If config-key is present and not default: This is an error:
            throw exceptionSupplier.get();
        }

        // User did not specify any client config value. Thus we make a smart guess
        // about which configuration is to be used.
        //
        // Determine all matching configurations for given SEI
        List<String> keylist = fixedConfig.clients()
                .entrySet()
                .stream()
                .filter(kv -> kv.getValue() != null)
                .filter(kv -> kv.getValue().serviceInterface().isPresent())
                .filter(kv -> kv.getValue().serviceInterface().get().equals(meta.getSei()))
                .filter(kv -> kv.getValue().alternative() == false)
                .map(Map.Entry::getKey)
                .collect(toList());

        switch (keylist.size()) {
            case 0:
                // It is legal to have no matching configuration. Then we go ahead and use default values derived from
                // the service itself.
                LOGGER.warnf(
                        "No configuration found for quarkus.cxf.*.service-interface = %s and alternative = false. Using the values from the service instead: %s.",
                        meta.getSei(), meta);
                return new CXFClientInfo(meta, config, config.internal().client(), null, vertx);
            case 1:
                return new CXFClientInfo(meta, config, config.clients().get(keylist.get(0)), keylist.get(0), vertx);
            default:
                throw new IllegalStateException("quarkus.cxf.*.service-interface = " + meta.getSei()
                        + " with alternative = false expected once, but found " + keylist.size() + " times in "
                        + keylist.stream().map(k -> "quarkus.cxf.\"" + k + "\".service-interface")
                                .collect(Collectors.joining(", ")));
        }

    }

    public interface ClientFactoryCustomizer {
        void customize(CXFClientInfo cxfClientInfo, JaxWsProxyFactoryBean factory);
    }
}
