package io.quarkiverse.cxf;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;

import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.common.spi.GeneratedNamespaceClassLoader;
import org.apache.cxf.common.spi.NamespaceClassCreator;
import org.apache.cxf.endpoint.dynamic.ExceptionClassCreator;
import org.apache.cxf.endpoint.dynamic.ExceptionClassLoader;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxb.FactoryClassCreator;
import org.apache.cxf.jaxb.FactoryClassLoader;
import org.apache.cxf.jaxb.WrapperHelperClassLoader;
import org.apache.cxf.jaxb.WrapperHelperCreator;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.spi.WrapperClassCreator;
import org.apache.cxf.jaxws.spi.WrapperClassLoader;
import org.apache.cxf.message.Message;
import org.apache.cxf.wsdl.ExtensionClassCreator;
import org.apache.cxf.wsdl.ExtensionClassLoader;
import org.jboss.logging.Logger;

import io.quarkiverse.cxf.annotation.CXF;

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
    private static final String DEFAULT_EP_ADDR = "http://localhost:8080";

    @Inject
    CxfConfig config;

    /**
     * Must be public, otherwise: java.lang.VerifyError: Bad access to protected data in invokevirtual
     */
    public Object loadCxfClient(
            InjectionPoint ip,
            CXFClientInfo metainfo) {
        LOGGER.debug(format("ip(%s) config(%s)", ip, this.config));

        CXFClientInfo info = this.cxfClientInfoSupplier(config, ip, metainfo);

        return produceCxfClient(info);
    }

    /**
     * The main workhorse producing a CXF client proxy.
     *
     * @param info
     * @return
     */
    private Object produceCxfClient(CXFClientInfo info) {
        Class<?> seiClass;
        try {
            seiClass = Class.forName(info.getSei(), false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            LOGGER.error("either webservice interface (client) or implementation (server) is mandatory");
            return null;
        }
        QuarkusClientFactoryBean quarkusClientFactoryBean = new QuarkusClientFactoryBean(info.getClassNames());
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean(quarkusClientFactoryBean);
        Bus bus = quarkusClientFactoryBean.getBus(true);
        bus.setExtension(new WrapperHelperClassLoader(bus), WrapperHelperCreator.class);
        bus.setExtension(new ExtensionClassLoader(bus), ExtensionClassCreator.class);
        bus.setExtension(new ExceptionClassLoader(bus), ExceptionClassCreator.class);
        bus.setExtension(new WrapperClassLoader(bus), WrapperClassCreator.class);
        bus.setExtension(new FactoryClassLoader(bus), FactoryClassCreator.class);
        bus.setExtension(new GeneratedNamespaceClassLoader(bus), NamespaceClassCreator.class);
        factory.setServiceClass(seiClass);
        LOGGER.info(format("using servicename %s%s", info.getWsNamespace(), info.getWsName()));
        factory.setServiceName(new QName(info.getWsNamespace(), info.getWsName()));
        LOGGER.info(format("using  servicename %s", factory.getServiceName()));
        if (info.getEpName() != null) {
            factory.setEndpointName(new QName(info.getEpNamespace(), info.getEpName()));
        }
        factory.setAddress(info.getEndpointAddress());
        if (info.getSoapBinding() != null) {
            factory.setBindingId(info.getSoapBinding());
        }
        if (info.getWsdlUrl() != null && !info.getWsdlUrl().isEmpty()) {
            factory.setWsdlURL(info.getWsdlUrl());
        }
        if (info.getUsername() != null) {
            factory.setUsername(info.getUsername());
        }
        if (info.getPassword() != null) {
            factory.setPassword(info.getPassword());
        }
        for (String feature : info.getFeatures()) {
            addToCols(feature, factory.getFeatures(), Feature.class);
        }
        for (String inInterceptor : info.getInInterceptors()) {
            addToCols(inInterceptor, factory.getInInterceptors());
        }
        for (String outInterceptor : info.getOutInterceptors()) {
            addToCols(outInterceptor, factory.getOutInterceptors());
        }
        for (String outFaultInterceptor : info.getOutFaultInterceptors()) {
            addToCols(outFaultInterceptor, factory.getOutFaultInterceptors());
        }
        for (String inFaultInterceptor : info.getInFaultInterceptors()) {
            addToCols(inFaultInterceptor, factory.getInFaultInterceptors());
        }

        LOGGER.info("cxf client loaded for " + info.getSei());
        return factory.create();
    }

    private static void addToCols(
            String className,
            List<Interceptor<? extends Message>> cols) {
        /*
         * We use CastUtils to simplify an unchecked cast from
         * List<Interceptor<? extends Message>> to List<Interceptor>. For our
         * purposes this is ok since the parameterization of Interceptor is lost
         * at runtime anyway and we wouldn't be able enforce it without some
         * very complicated and very Interceptor-specific reflection code.
         */
        addToCols(className, CastUtils.<Interceptor> cast(cols), Interceptor.class);
    }

    private static <T> void addToCols(
            String className,
            List<T> cols,
            Class<T> clazz) {
        Class<? extends T> cls;
        try {
            cls = Class.forName(className, false, Thread.currentThread().getContextClassLoader()).asSubclass(clazz);
        } catch (ClassNotFoundException | ClassCastException e) {
            // silent fail
            LOGGER.warn(format("no such class %s", className));
            return;
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
            LOGGER.warn(format("unable to create instance of class %s", className));
        } else {
            cols.add(item);
        }
    }

    static private CXFClientInfo cxfClientInfoSupplier(
            CxfConfig cxfConfig,
            InjectionPoint ip,
            CXFClientInfo meta) {
        CXFClientInfo info = new CXFClientInfo(meta);

        // TODO: mingle around with IP info as well.
        //        // Is this client annotated with CXF?
        if (ip.getAnnotated().isAnnotationPresent(CXF.class)) {
            CXF anno = ip.getAnnotated().getAnnotation(CXF.class);
            String configKey = anno.config();

            if (cxfConfig.isClientPresent(configKey)) {
                CxfClientConfig cfg = cxfConfig.getClient(configKey);
                info = info.withConfig(cfg);
            } else {
                LOGGER.warn("no such CXF client configuration in your app properties: " + configKey);
            }
            // That's it.
            return info;
        }

        // determine all matching configurations for given SEI
        List<String> keylist = cxfConfig.clients
                .entrySet()
                .stream()
                .filter(kv -> kv.getValue() != null)
                .filter(kv -> kv.getValue().serviceInterface.isPresent())
                .filter(kv -> kv.getValue().serviceInterface.get().equals(meta.getSei()))
                .map(Map.Entry::getKey)
                .collect(toList());

        //
        // keylist contains all configurations for given SEI.
        //
        if (keylist.size() > 1) {
            // TODO: either bail out with error or merge (in a predictable way).
            // Alternative: Use "serviceinterface" instead of "endpoints" where the service interface
            // class is the configuration key.
            LOGGER.warn(format(
                    "multiple client configurations found for SEI %s: %s, all but first are ignored.",
                    meta.getSei(),
                    join(",", keylist)));
        }

        if (!keylist.isEmpty()) {
            info = info.withConfig(cxfConfig.clients.get(keylist.get(0)));
        }

        return info;
    }

}
