package io.quarkiverse.cxf;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;
import javax.servlet.ServletConfig;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.spi.GeneratedNamespaceClassLoader;
import org.apache.cxf.common.spi.NamespaceClassCreator;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.dynamic.ExceptionClassCreator;
import org.apache.cxf.endpoint.dynamic.ExceptionClassLoader;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxb.FactoryClassCreator;
import org.apache.cxf.jaxb.FactoryClassLoader;
import org.apache.cxf.jaxb.WrapperHelperClassLoader;
import org.apache.cxf.jaxb.WrapperHelperCreator;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.jaxws.spi.WrapperClassCreator;
import org.apache.cxf.jaxws.spi.WrapperClassLoader;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.apache.cxf.wsdl.ExtensionClassCreator;
import org.apache.cxf.wsdl.ExtensionClassLoader;
import org.jboss.logging.Logger;

import io.quarkus.arc.Unremovable;

@Singleton
@Unremovable
public class CXFQuarkusServlet extends CXFNonSpringServlet {

    private static final Logger LOGGER = Logger.getLogger(CXFQuarkusServlet.class);

    public CXFQuarkusServlet() {
    }

    private Class<?> loadClass(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            //silent fail
        }
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            LOGGER.warn("failed to load class " + className);
            return null;
        }
    }

    private Object getInstance(String className) {
        Class<?> classObj = loadClass(className);
        try {
            return classObj.getConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void loadBus(ServletConfig servletConfig) {
        LOGGER.info("Load CXF bus");

        super.loadBus(servletConfig);

    }

    public void build(CXFServletInfos cxfServletInfos) {
        if (cxfServletInfos == null || cxfServletInfos.getInfos() == null || cxfServletInfos.getInfos().isEmpty()) {
            LOGGER.warn("no info transmit to servlet");
            return;
        }
        Bus bus = getBus();
        bus.setExtension(new WrapperHelperClassLoader(bus), WrapperHelperCreator.class);
        bus.setExtension(new ExtensionClassLoader(bus), ExtensionClassCreator.class);
        bus.setExtension(new ExceptionClassLoader(bus), ExceptionClassCreator.class);
        bus.setExtension(new WrapperClassLoader(bus), WrapperClassCreator.class);
        bus.setExtension(new FactoryClassLoader(bus), FactoryClassCreator.class);
        bus.setExtension(new GeneratedNamespaceClassLoader(bus), NamespaceClassCreator.class);
        BusFactory.setDefaultBus(bus);
        for (CXFServletInfo servletInfo : cxfServletInfos.getInfos()) {
            JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean(
                    new QuarkusJaxWsServiceFactoryBean(cxfServletInfos.getWrappersclasses()));
            factory.setBus(bus);
            Object instanceService = getInstance(servletInfo.getClassName());
            if (instanceService != null) {
                Class<?> seiClass = null;
                if (servletInfo.getSei() != null) {
                    seiClass = loadClass(servletInfo.getSei());
                    factory.setServiceClass(seiClass);
                }
                if (seiClass == null) {
                    LOGGER.warn("sei not found: " + servletInfo.getSei());
                }
                factory.setAddress(servletInfo.getPath());
                factory.setServiceBean(instanceService);
                if (servletInfo.getWsdlPath() != null) {
                    factory.setWsdlLocation(servletInfo.getWsdlPath());
                }
                if (!servletInfo.getFeatures().isEmpty()) {
                    List<Feature> features = new ArrayList<>();
                    for (String feature : servletInfo.getFeatures()) {
                        Feature instanceFeature = (Feature) getInstance(feature);
                        features.add(instanceFeature);
                    }
                    factory.setFeatures(features);
                }
                if (servletInfo.getSOAPBinding() != null) {
                    factory.setBindingId(servletInfo.getSOAPBinding());
                }
                if (servletInfo.getEndpointUrl() != null) {
                    factory.setPublishedEndpointUrl(servletInfo.getEndpointUrl());
                }

                Server server = factory.create();
                for (String className : servletInfo.getInFaultInterceptors()) {
                    Interceptor<? extends Message> interceptor = (Interceptor<? extends Message>) getInstance(className);
                    server.getEndpoint().getInFaultInterceptors().add(interceptor);
                }
                for (String className : servletInfo.getInInterceptors()) {
                    Interceptor<? extends Message> interceptor = (Interceptor<? extends Message>) getInstance(className);
                    server.getEndpoint().getInInterceptors().add(interceptor);
                }
                for (String className : servletInfo.getOutFaultInterceptors()) {
                    Interceptor<? extends Message> interceptor = (Interceptor<? extends Message>) getInstance(className);
                    server.getEndpoint().getOutFaultInterceptors().add(interceptor);
                }
                for (String className : servletInfo.getOutInterceptors()) {
                    Interceptor<? extends Message> interceptor = (Interceptor<? extends Message>) getInstance(className);
                    server.getEndpoint().getOutInterceptors().add(interceptor);
                }

                LOGGER.info(servletInfo.toString() + " available.");
            } else {
                LOGGER.error("Cannot initialize " + servletInfo.toString());
            }
        }
    }
}
