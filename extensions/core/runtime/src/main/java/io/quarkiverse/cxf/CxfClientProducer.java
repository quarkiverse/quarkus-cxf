package io.quarkiverse.cxf;

import static java.util.stream.Collectors.toList;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.handler.Handler;

import org.apache.cxf.Bus;
import org.apache.cxf.common.spi.GeneratedNamespaceClassLoader;
import org.apache.cxf.common.spi.NamespaceClassCreator;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.dynamic.ExceptionClassCreator;
import org.apache.cxf.endpoint.dynamic.ExceptionClassLoader;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxb.FactoryClassCreator;
import org.apache.cxf.jaxb.FactoryClassLoader;
import org.apache.cxf.jaxb.WrapperHelperClassLoader;
import org.apache.cxf.jaxb.WrapperHelperCreator;
import org.apache.cxf.jaxws.spi.WrapperClassCreator;
import org.apache.cxf.jaxws.spi.WrapperClassLoader;
import org.apache.cxf.message.Message;
import org.apache.cxf.wsdl.ExtensionClassCreator;
import org.apache.cxf.wsdl.ExtensionClassLoader;
import org.jboss.logging.Logger;

import io.quarkiverse.cxf.annotation.CXFClient;

/**
 * Base producer class for setting up CXF client proxies.
 * <p>
 * During augementation (build-time) a bean is created derived from this class for each SEI. The producing method calls
 * loadCxfClient() to get a WS client proxy.
 * <p>
 * Notice the InjectionPoint parameter present in signature of loadCxfClient. Via that meta information we calculate the
 * proper configuration to use.
 */
public abstract class CxfClientProducer {

    private static final Logger LOGGER = Logger.getLogger(CxfClientProducer.class);

    public static final String RUNTIME_INITIALIZED_PROXY_MARKER_INTERFACE_PACKAGE = "io.quarkiverse.cxf.runtime.proxy";
    public static final String RUNTIME_INITIALIZED_PROXY_MARKER_INTERFACE_NAME = "io.quarkiverse.cxf.runtime.proxy.RuntimeInitializedProxyMarker";

    @Inject
    CxfConfig config;

    @Inject
    CxfFixedConfig fixedConfig;

    /**
     * Must be public, otherwise: java.lang.VerifyError: Bad access to protected data in invokevirtual
     */
    public Object loadCxfClient(InjectionPoint ip, CXFClientInfo meta) {
        return produceCxfClient(selectorCXFClientInfo(config, fixedConfig, ip, meta));
    }

    /**
     * The main workhorse producing a CXF client proxy.
     *
     * @param cxfClientInfo
     * @return
     */
    private Object produceCxfClient(CXFClientInfo cxfClientInfo) {
        Class<?> seiClass;
        try {
            seiClass = Class.forName(cxfClientInfo.getSei(), false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            LOGGER.errorf("WebService interface (client) class %s not found", cxfClientInfo.getSei());
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
        QuarkusClientFactoryBean quarkusClientFactoryBean = new QuarkusClientFactoryBean(cxfClientInfo.getClassNames());
        QuarkusJaxWsProxyFactoryBean factory = new QuarkusJaxWsProxyFactoryBean(quarkusClientFactoryBean, interfaces);
        Bus bus = quarkusClientFactoryBean.getBus(true);
        bus.setExtension(new WrapperHelperClassLoader(bus), WrapperHelperCreator.class);
        bus.setExtension(new ExtensionClassLoader(bus), ExtensionClassCreator.class);
        bus.setExtension(new ExceptionClassLoader(bus), ExceptionClassCreator.class);
        bus.setExtension(new WrapperClassLoader(bus), WrapperClassCreator.class);
        bus.setExtension(new FactoryClassLoader(bus), FactoryClassCreator.class);
        bus.setExtension(new GeneratedNamespaceClassLoader(bus), NamespaceClassCreator.class);
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
        if (cxfClientInfo.getUsername() != null) {
            factory.setUsername(cxfClientInfo.getUsername());
        }
        if (cxfClientInfo.getPassword() != null) {
            factory.setPassword(cxfClientInfo.getPassword());
        }
        for (String feature : cxfClientInfo.getFeatures()) {
            addToCols(feature, factory.getFeatures(), Feature.class);
        }
        for (String handler : cxfClientInfo.getHandlers()) {
            addToCols(handler, factory.getHandlers(), Handler.class);
        }
        for (String inInterceptor : cxfClientInfo.getInInterceptors()) {
            addToCols(inInterceptor, factory.getInInterceptors());
        }
        for (String outInterceptor : cxfClientInfo.getOutInterceptors()) {
            addToCols(outInterceptor, factory.getOutInterceptors());
        }
        for (String outFaultInterceptor : cxfClientInfo.getOutFaultInterceptors()) {
            addToCols(outFaultInterceptor, factory.getOutFaultInterceptors());
        }
        for (String inFaultInterceptor : cxfClientInfo.getInFaultInterceptors()) {
            addToCols(inFaultInterceptor, factory.getInFaultInterceptors());
        }

        LOGGER.debug("cxf client loaded for " + cxfClientInfo.getSei());
        return factory.create();
    }

    private void addToCols(String className, List<Interceptor<? extends Message>> cols) {
        /*
         * We use CastUtils to simplify an unchecked cast from
         * List<Interceptor<? extends Message>> to List<Interceptor>. For our
         * purposes this is ok since the parameterization of Interceptor is lost
         * at runtime anyway and we wouldn't be able enforce it without some
         * very complicated and very Interceptor-specific reflection code.
         */
        addToCols(className, CastUtils.<Interceptor> cast(cols), Interceptor.class);
    }

    private <T> void addToCols(String className, List<T> cols, Class<T> clazz) {
        Class<? extends T> cls;
        try {
            cls = Class.forName(className, false, Thread.currentThread().getContextClassLoader()).asSubclass(clazz);
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new RuntimeException("No such class " + className, e);
        }
        T item = null;
        try {
            item = CDI.current().select(cls).get();
        } catch (ClassCastException | UnsatisfiedResolutionException e) {
            // ignored
        }
        // if not found with beans just generate it.

        try {
            item = item == null ? cls.getConstructor().newInstance() : item;
        } catch (ReflectiveOperationException | RuntimeException e) {
            /* ignore */
        }

        if (item == null) {
            LOGGER.warnf("unable to create instance of class %s", className);
        } else {
            cols.add(item);
        }
    }

    /**
     * Calculates the client info to use for producing a JAXWS client proxy.
     *
     * @param cxfConfig The current configuration
     * @param ip Meta information about where injection of client proxy takes place
     * @param meta The default to return
     * @return not null
     */
    private static CXFClientInfo selectorCXFClientInfo(
            CxfConfig config,
            CxfFixedConfig fixedConfig,
            InjectionPoint ip,
            CXFClientInfo meta) {
        CXFClientInfo info = new CXFClientInfo(meta);

        // If injection point is annotated with @CXFClient then determine a
        // configuration by looking up annotated config value:

        if (ip.getAnnotated().isAnnotationPresent(CXFClient.class)) {
            CXFClient anno = ip.getAnnotated().getAnnotation(CXFClient.class);
            String configKey = anno.value();

            if (config.isClientPresent(configKey)) {
                return info.withConfig(config.getClient(configKey));
            }

            // If config-key is present and not default: This is an error:
            if (configKey != null && !configKey.isEmpty()) {
                throw new IllegalStateException(
                        "quarkus.cxf.\"" + configKey + "\" is referenced in " + ip.getMember()
                                + " but no such build time configuration entry exists");
            }
        }
        // User did not specify any client config value. Thus we make a smart guess
        // about which configuration is to be used.
        //
        // Determine all matching configurations for given SEI
        List<String> keylist = fixedConfig.clients
                .entrySet()
                .stream()
                .filter(kv -> kv.getValue() != null)
                .filter(kv -> kv.getValue().serviceInterface.isPresent())
                .filter(kv -> kv.getValue().serviceInterface.get().equals(meta.getSei()))
                .filter(kv -> kv.getValue().alternative == false)
                .map(Map.Entry::getKey)
                .collect(toList());

        switch (keylist.size()) {
            case 0:
                // It is legal to have no matching configuration. Then we go ahead and use default values derived from
                // the service itself.
                LOGGER.warnf(
                        "No configuration found for quarkus.cxf.*.service-interface = %s and alternative = false. Using the values from the service instead: %s.",
                        meta.getSei(), meta);
                return meta;
            case 1:
                return info.withConfig(config.clients.get(keylist.get(0)));
            default:
                throw new IllegalStateException("quarkus.cxf.*.service-interface = " + meta.getSei()
                        + " with alternative = false expected once, but found " + keylist.size() + " times in "
                        + keylist.stream().map(k -> "quarkus.cxf.\"" + k + "\".service-interface")
                                .collect(Collectors.joining(", ")));
        }

    }

}
