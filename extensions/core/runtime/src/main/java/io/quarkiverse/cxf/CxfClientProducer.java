package io.quarkiverse.cxf;

import static java.util.stream.Collectors.toList;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.namespace.QName;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.xml.ws.BindingProvider;

import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.configuration.security.ProxyAuthorizationPolicy;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitFactory;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.WSAContextUtils;
import org.jboss.logging.Logger;

import io.quarkiverse.cxf.CxfClientConfig.HTTPConduitImpl;
import io.quarkiverse.cxf.CxfClientConfig.WellKnownHostnameVerifier;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.logging.LoggingFactoryCustomizer;

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
    QuarkusHttpConduitConfigurer httpConduitConfigurer;

    @Inject
    @Any
    Instance<ClientFactoryCustomizer> customizers;

    private LoggingFactoryCustomizer loggingFactoryCustomizer;

    @PostConstruct
    void init() {
        this.loggingFactoryCustomizer = new LoggingFactoryCustomizer(config);
    }

    /**
     * Must be public, otherwise: java.lang.VerifyError: Bad access to protected data in invokevirtual
     */
    public Object loadCxfClient(InjectionPoint ip, CXFClientData meta) {
        return produceCxfClient(selectorCXFClientInfo(config, fixedConfig, ip, meta));
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
        return selectorCXFClientInfo(config, fixedConfig, ip, meta);
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
        QuarkusClientFactoryBean quarkusClientFactoryBean = new QuarkusClientFactoryBean();
        QuarkusJaxWsProxyFactoryBean factory = new QuarkusJaxWsProxyFactoryBean(quarkusClientFactoryBean, interfaces);
        final Map<String, Object> props = new LinkedHashMap<>();
        factory.setProperties(props);
        props.put(CXFClientInfo.class.getName(), cxfClientInfo);
        factory.setServiceClass(seiClass);
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
        final String username = cxfClientInfo.getUsername();
        if (username != null) {
            final String password = cxfClientInfo.getPassword();
            final AuthorizationPolicy authPolicy = new AuthorizationPolicy();
            authPolicy.setUserName(username);
            if (password != null) {
                authPolicy.setPassword(password);
            }
            if (cxfClientInfo.isSecureWsdlAccess()) {
                /*
                 * This is the only way how the AuthorizationPolicy can be set early enough to be effective for the WSDL
                 * GET request. We do not do it by default because of backwards compatibility and for the user to think
                 * twice whether his WSDL URL uses HTTPS and only then enable secureWsdlAccess
                 */
                httpConduitConfigurer.addConfigurer(cxfClientInfo.getEndpointAddress(),
                        conduit -> conduit.setAuthorization(authPolicy));
            } else {
                props.put(AuthorizationPolicy.class.getName(), authPolicy);
            }
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

        final HTTPConduitImpl httpConduitImpl = cxfClientInfo.getHttpConduitImpl();
        if (httpConduitImpl != null) {
            switch (httpConduitImpl) {
                case CXFDefault:
                    // nothing to do
                    break;
                case QuarkusCXFDefault:
                case URLConnectionHTTPConduitFactory: {
                    props.put(HTTPConduitFactory.class.getName(), new URLConnectionHTTPConduitFactory());
                    break;
                }
                case HttpClientHTTPConduitFactory: {
                    props.put(HTTPConduitFactory.class.getName(), new HttpClientHTTPConduitFactory());
                    break;
                }
                default:
                    throw new IllegalStateException("Unexpected " + HTTPConduitImpl.class.getSimpleName() + " value: "
                            + httpConduitImpl);
            }
        }

        {
            final String value = cxfClientInfo.getDecoupledEndpointBase();
            if (value != null) {
                props.put(WSAContextUtils.DECOUPLED_ENDPOINT_BASE_PROPERTY, value);
            }
        }

        loggingFactoryCustomizer.customize(cxfClientInfo, factory);
        customizers.forEach(customizer -> customizer.customize(cxfClientInfo, factory));

        LOGGER.debug("cxf client loaded for " + sei);
        Object result = factory.create();
        final Client client = ClientProxy.getClient(result);
        final HTTPConduit httpConduit = (HTTPConduit) client.getConduit();
        final HTTPClientPolicy policy = httpConduit.getClient();
        policy.setConnectionTimeout(cxfClientInfo.getConnectionTimeout());
        policy.setReceiveTimeout(cxfClientInfo.getReceiveTimeout());
        policy.setConnectionRequestTimeout(cxfClientInfo.getConnectionRequestTimeout());
        policy.setAutoRedirect(cxfClientInfo.isAutoRedirect());
        policy.setMaxRetransmits(cxfClientInfo.getMaxRetransmits());
        policy.setAllowChunking(cxfClientInfo.isAllowChunking());
        policy.setChunkingThreshold(cxfClientInfo.getChunkingThreshold());
        policy.setChunkLength(cxfClientInfo.getChunkLength());
        {
            final String value = cxfClientInfo.getAccept();
            if (value != null) {
                policy.setAccept(value);
            }
        }
        {
            final String value = cxfClientInfo.getAcceptLanguage();
            if (value != null) {
                policy.setAcceptLanguage(value);
            }
        }
        {
            final String value = cxfClientInfo.getAcceptEncoding();
            if (value != null) {
                policy.setAcceptEncoding(value);
            }
        }
        {
            final String value = cxfClientInfo.getContentType();
            if (value != null) {
                policy.setContentType(value);
            }
        }
        {
            final String value = cxfClientInfo.getHost();
            if (value != null) {
                policy.setHost(value);
            }
        }
        policy.setConnection(cxfClientInfo.getConnection());
        {
            final String value = cxfClientInfo.getCacheControl();
            if (value != null) {
                policy.setCacheControl(value);
            }
        }
        policy.setVersion(cxfClientInfo.getVersion());
        {
            final String value = cxfClientInfo.getBrowserType();
            if (value != null) {
                policy.setBrowserType(value);
            }
        }
        {
            final String value = cxfClientInfo.getDecoupledEndpoint();
            if (value != null) {
                policy.setDecoupledEndpoint(value);
            }
        }
        {
            final String value = cxfClientInfo.getProxyServer();
            if (value != null) {
                policy.setProxyServer(value);
            }
        }
        {
            final Integer value = cxfClientInfo.getProxyServerPort();
            if (value != null) {
                policy.setProxyServerPort(value);
            }
        }
        {
            final String value = cxfClientInfo.getNonProxyHosts();
            if (value != null) {
                policy.setNonProxyHosts(value);
            }
        }
        policy.setProxyServerType(cxfClientInfo.getProxyServerType());

        final String proxyUsername = cxfClientInfo.getProxyUsername();
        if (proxyUsername != null) {
            final String proxyPassword = cxfClientInfo.getProxyPassword();
            final ProxyAuthorizationPolicy proxyAuth = new ProxyAuthorizationPolicy();
            proxyAuth.setUserName(proxyUsername);
            proxyAuth.setPassword(proxyPassword);
            httpConduit.setProxyAuthorization(proxyAuth);
        }

        final String trustStorePath = cxfClientInfo.getTrustStore();
        if (trustStorePath != null) {
            TLSClientParameters tlsCP = new TLSClientParameters();
            final KeyStore trustStore;
            final TrustManagerFactory tmf;
            try (InputStream is = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(trustStorePath)) {
                trustStore = KeyStore.getInstance(cxfClientInfo.getTrustStoreType());
                final String pwd = cxfClientInfo.getTrustStorePassword();
                trustStore.load(is, pwd == null ? null : pwd.toCharArray());
                tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustStore);
            } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException e) {
                throw new RuntimeException("Could not load client-truststore.jks from class path", e);
            }
            tlsCP.setTrustManagers(tmf.getTrustManagers());

            final String hostnameVerifierName = cxfClientInfo.getHostnameVerifier();
            if (hostnameVerifierName != null) {
                final Optional<WellKnownHostnameVerifier> wellKnownHostNameVerifierName = WellKnownHostnameVerifier
                        .of(hostnameVerifierName);
                if (wellKnownHostNameVerifierName.isPresent()) {
                    wellKnownHostNameVerifierName.get().configure(tlsCP);
                } else {
                    final HostnameVerifier hostnameVerifier = CXFRuntimeUtils.getInstance(hostnameVerifierName, true);
                    if (hostnameVerifier == null) {
                        throw new RuntimeException("Could not find or instantiate " + hostnameVerifierName);
                    }
                    tlsCP.setHostnameVerifier(hostnameVerifier);
                }
            }

            httpConduit.setTlsClientParameters(tlsCP);
        }
        {
            final SchemaValidationType value = cxfClientInfo.getSchemaValidationEnabledFor();
            if (value != null) {
                client.getEndpoint().getEndpointInfo().setProperty(Message.SCHEMA_VALIDATION_TYPE, value);
            }
        }

        return result;
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
            CXFClientData meta) {

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
                                + " but no such build time configuration entry exists"));
    }

    public static CXFClientInfo selectorCXFClientInfo(
            CxfConfig config,
            CxfFixedConfig fixedConfig,
            CXFClientData meta,
            String configKey,
            Supplier<IllegalStateException> exceptionSupplier) {

        // If injection point is annotated with @CXFClient then determine a
        // configuration by looking up annotated config value:

        if (configKey != null && !configKey.isEmpty()) {
            if (config.isClientPresent(configKey)) {
                return new CXFClientInfo(meta, config, config.getClient(configKey), configKey);
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
                return new CXFClientInfo(meta, config, config.internal().client(), null);
            case 1:
                return new CXFClientInfo(meta, config, config.clients().get(keylist.get(0)), keylist.get(0));
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
