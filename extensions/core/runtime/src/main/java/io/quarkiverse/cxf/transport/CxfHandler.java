package io.quarkiverse.cxf.transport;

import java.util.LinkedHashMap;

import javax.xml.namespace.QName;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxws.JAXWSMethodInvoker;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.jaxws.support.JaxWsImplementorInfo;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.servlet.ServletController;
import org.apache.cxf.transport.servlet.servicelist.ServiceListGeneratorServlet;
import org.jboss.logging.Logger;

import io.quarkiverse.cxf.CXFRuntimeUtils;
import io.quarkiverse.cxf.CXFServletInfo;
import io.quarkiverse.cxf.CXFServletInfos;
import io.quarkiverse.cxf.CxfConfig;
import io.quarkiverse.cxf.CxfFixedConfig;
import io.quarkiverse.cxf.QuarkusJaxWsServerFactoryBean;
import io.quarkiverse.cxf.QuarkusRuntimeJaxWsServiceFactoryBean;
import io.quarkiverse.cxf.auth.AuthFaultOutInterceptor;
import io.quarkiverse.cxf.logging.LoggingFactoryCustomizer;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class CxfHandler implements Handler<RoutingContext> {
    private static final Logger LOGGER = Logger.getLogger(CxfHandler.class);
    private final Bus bus;
    private final ClassLoader loader;
    private final String contextPath;
    private final String servletPath;
    private final ServletController controller;
    private final BeanContainer beanContainer;
    private final CurrentIdentityAssociation association;
    private final IdentityProviderManager identityProviderManager;
    private final CurrentVertxRequest currentVertxRequest;
    private final HttpConfiguration httpConfiguration;
    private final int outputBufferSize;
    private final int minChunkSize;

    private static final String X_FORWARDED_PROTO_HEADER = "X-Forwarded-Proto";
    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String X_FORWARDED_PORT_HEADER = "X-Forwarded-Port";

    public CxfHandler(CXFServletInfos cxfServletInfos, BeanContainer beanContainer, HttpConfiguration httpConfiguration,
            CxfFixedConfig fixedConfig) {
        LOGGER.trace("CxfHandler created");
        this.beanContainer = beanContainer;
        this.httpConfiguration = httpConfiguration;
        Instance<CurrentIdentityAssociation> identityAssociationInstance = CDI.current()
                .select(CurrentIdentityAssociation.class);
        this.association = identityAssociationInstance.isResolvable() ? identityAssociationInstance.get() : null;
        Instance<IdentityProviderManager> identityProviderManagerInstance = CDI.current().select(IdentityProviderManager.class);
        this.identityProviderManager = identityProviderManagerInstance.isResolvable() ? identityProviderManagerInstance.get()
                : null;
        this.currentVertxRequest = CDI.current().select(CurrentVertxRequest.class).get();
        this.bus = BusFactory.getDefaultBus();

        this.loader = this.bus.getExtension(ClassLoader.class);
        this.outputBufferSize = fixedConfig.outputBufferSize();
        this.minChunkSize = fixedConfig.minChunkSize();

        LOGGER.trace("load destination");
        DestinationFactoryManager dfm = this.bus.getExtension(DestinationFactoryManager.class);
        final VertxDestinationFactory destinationFactory = new VertxDestinationFactory();
        final DestinationRegistry destinationRegistry = destinationFactory.getRegistry();
        dfm.registerDestinationFactory("http://cxf.apache.org/transports/quarkus", destinationFactory);
        ConduitInitiatorManager extension = bus.getExtension(ConduitInitiatorManager.class);
        extension.registerConduitInitiator("http://cxf.apache.org/transports/quarkus", destinationFactory);

        ServiceListGeneratorServlet serviceListGeneratorServlet = new ServiceListGeneratorServlet(destinationRegistry, bus);
        VertxServletConfig servletConfig = new VertxServletConfig();
        serviceListGeneratorServlet.init(servletConfig);
        this.controller = new ServletController(destinationRegistry, servletConfig, serviceListGeneratorServlet);
        servletPath = cxfServletInfos.getPath();
        contextPath = cxfServletInfos.getContextPath();

        final LoggingFactoryCustomizer loggingFactoryCustomizer = new LoggingFactoryCustomizer(
                CDI.current().select(CxfConfig.class).get());
        final Instance<EndpointFactoryCustomizer> customizers = CDI.current().select(EndpointFactoryCustomizer.class);

        // suboptimal because done it in loop but not a real issue...
        for (CXFServletInfo servletInfo : cxfServletInfos.getInfos()) {
            final String endpointString = "endpoint " + servletInfo.getPath();
            final Object instanceService = servletInfo.lookupBean();
            if (instanceService != null) {
                final Class<?> instanceType = servletInfo.getImplementor();
                final QuarkusRuntimeJaxWsServiceFactoryBean jaxWsServiceFactoryBean = new QuarkusRuntimeJaxWsServiceFactoryBean(
                        new JaxWsImplementorInfo(instanceType));
                final JaxWsServerFactoryBean jaxWsServerFactoryBean = new QuarkusJaxWsServerFactoryBean(jaxWsServiceFactoryBean,
                        endpointString);
                jaxWsServerFactoryBean.setServiceClass(instanceType);

                jaxWsServerFactoryBean.setDestinationFactory(destinationFactory);
                jaxWsServerFactoryBean.setBus(bus);
                jaxWsServerFactoryBean.setProperties(new LinkedHashMap<>());
                final String endpointType = servletInfo.getClassName();
                if (servletInfo.isProvider()) {
                    // Needed for any Provider interface implementations
                    jaxWsServiceFactoryBean.setInvoker(new JAXWSMethodInvoker(instanceService));
                }

                jaxWsServerFactoryBean
                        .setServiceName(new QName(servletInfo.getServiceTargetNamespace(), servletInfo.getServiceName()));
                jaxWsServerFactoryBean.setAddress(servletInfo.getRelativePath());
                jaxWsServerFactoryBean.setServiceBean(instanceService);
                if (servletInfo.getWsdlPath() != null) {
                    jaxWsServerFactoryBean.setWsdlLocation(servletInfo.getWsdlPath());
                }
                CXFRuntimeUtils.addBeans(servletInfo.getFeatures(), "feature", endpointString, endpointType,
                        jaxWsServerFactoryBean.getFeatures());
                CXFRuntimeUtils.addBeans(servletInfo.getHandlers(), "handler", endpointString, endpointType,
                        jaxWsServerFactoryBean.getHandlers());
                CXFRuntimeUtils.addBeans(servletInfo.getInInterceptors(), "inInterceptor", endpointString, endpointType,
                        jaxWsServerFactoryBean.getInInterceptors());
                CXFRuntimeUtils.addBeans(servletInfo.getOutInterceptors(), "outInterceptor", endpointString, endpointType,
                        jaxWsServerFactoryBean.getOutInterceptors());
                CXFRuntimeUtils.addBeans(servletInfo.getOutFaultInterceptors(), "outFaultInterceptor", endpointString,
                        endpointType, jaxWsServerFactoryBean.getOutFaultInterceptors());
                jaxWsServerFactoryBean.getOutFaultInterceptors().add(new AuthFaultOutInterceptor());
                CXFRuntimeUtils.addBeans(servletInfo.getInFaultInterceptors(), "inFaultInterceptor", endpointString,
                        endpointType, jaxWsServerFactoryBean.getInFaultInterceptors());

                if (servletInfo.getSOAPBinding() != null) {
                    jaxWsServerFactoryBean.setBindingId(servletInfo.getSOAPBinding());
                }
                if (servletInfo.getEndpointUrl() != null) {
                    jaxWsServerFactoryBean.setPublishedEndpointUrl(servletInfo.getEndpointUrl());
                }
                loggingFactoryCustomizer.customize(servletInfo, jaxWsServerFactoryBean);
                customizers.forEach(customizer -> customizer.customize(servletInfo, jaxWsServerFactoryBean));

                Server service = jaxWsServerFactoryBean.create();
                {
                    final SchemaValidationType value = servletInfo.getSchemaValidationEnabledFor();
                    if (value != null) {
                        service.getEndpoint().getEndpointInfo().setProperty(Message.SCHEMA_VALIDATION_TYPE, value);
                    }
                }

                LOGGER.info(servletInfo.toString() + " available.");
            } else {
                throw new IllegalStateException("Cannot initialize " + servletInfo.toString());
            }
        }
    }

    @Override
    public void handle(RoutingContext event) {
        ClassLoaderUtils.ClassLoaderHolder origLoader = null;
        Bus origBus = null;
        try {
            if (this.loader != null) {
                origLoader = ClassLoaderUtils.setThreadContextClassloader(this.loader);
            }

            if (this.bus != null) {
                origBus = BusFactory.getAndSetThreadDefaultBus(this.bus);
            }

            process(event);
        } finally {
            if (origBus != this.bus) {
                BusFactory.setThreadDefaultBus(origBus);
            }

            if (origLoader != null) {
                origLoader.reset();
            }

        }
    }

    private void process(RoutingContext event) {
        ManagedContext requestContext = this.beanContainer.requestContext();
        requestContext.activate();
        if (association != null) {
            QuarkusHttpUser existing = (QuarkusHttpUser) event.user();
            if (existing != null) {
                SecurityIdentity identity = existing.getSecurityIdentity();
                association.setIdentity(identity);
            } else {
                association.setIdentity(QuarkusHttpUser.getSecurityIdentity(event, identityProviderManager));
            }
        }
        currentVertxRequest.setCurrent(event);
        try {
            HttpServletRequest req = new VertxHttpServletRequest(event, contextPath, servletPath);
            VertxHttpServletResponse resp = new VertxHttpServletResponse(event, outputBufferSize, minChunkSize);
            controller.invoke(req, resp);
            resp.end();
        } catch (ServletException se) {
            LOGGER.warn("Internal server error", se);
            event.fail(500, se);
        } catch (RuntimeException re) {
            LOGGER.warn("Cannot list or instantiate web service", re);
            event.fail(404, re);
        } finally {
            if (requestContext.isActive()) {
                requestContext.terminate();
            }
        }
    }

    public interface EndpointFactoryCustomizer {
        void customize(CXFServletInfo cxfServletInfo, JaxWsServerFactoryBean factory);
    }

}
