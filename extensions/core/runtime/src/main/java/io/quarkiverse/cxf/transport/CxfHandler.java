package io.quarkiverse.cxf.transport;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.JAXWSMethodInvoker;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.servlet.ServletController;
import org.apache.cxf.transport.servlet.servicelist.ServiceListGeneratorServlet;
import org.jboss.logging.Logger;

import io.quarkiverse.cxf.CXFServletInfo;
import io.quarkiverse.cxf.CXFServletInfos;
import io.quarkiverse.cxf.QuarkusJaxWsServiceFactoryBean;
import io.quarkus.arc.Arc;
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

    private static final String X_FORWARDED_PROTO_HEADER = "X-Forwarded-Proto";
    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String X_FORWARDED_PORT_HEADER = "X-Forwarded-Port";

    public CxfHandler(CXFServletInfos cxfServletInfos, BeanContainer beanContainer, HttpConfiguration httpConfiguration) {
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

        LOGGER.trace("load destination");
        DestinationFactoryManager dfm = this.bus.getExtension(DestinationFactoryManager.class);
        final VertxDestinationFactory destinationFactory = new VertxDestinationFactory();
        final DestinationRegistry destinationRegistry = destinationFactory.getDestinationRegistry();
        dfm.registerDestinationFactory("http://cxf.apache.org/transports/quarkus", destinationFactory);
        ConduitInitiatorManager extension = bus.getExtension(ConduitInitiatorManager.class);
        extension.registerConduitInitiator("http://cxf.apache.org/transports/quarkus", destinationFactory);

        ServiceListGeneratorServlet serviceListGeneratorServlet = new ServiceListGeneratorServlet(destinationRegistry, bus);
        VertxServletConfig servletConfig = new VertxServletConfig();
        serviceListGeneratorServlet.init(servletConfig);
        this.controller = new ServletController(destinationRegistry, servletConfig, serviceListGeneratorServlet);
        servletPath = cxfServletInfos.getPath();
        contextPath = cxfServletInfos.getContextPath();

        // suboptimal because done it in loop but not a real issue...
        for (CXFServletInfo servletInfo : cxfServletInfos.getInfos()) {
            QuarkusJaxWsServiceFactoryBean jaxWsServiceFactoryBean = new QuarkusJaxWsServiceFactoryBean(
                    cxfServletInfos.getWrappersclasses());
            JaxWsServerFactoryBean jaxWsServerFactoryBean = new JaxWsServerFactoryBean(jaxWsServiceFactoryBean);
            jaxWsServerFactoryBean.setDestinationFactory(destinationFactory);
            jaxWsServerFactoryBean.setBus(bus);
            final String endpointType = servletInfo.getClassName();
            Object instanceService = getInstance(endpointType, false);

            if (instanceService != null) {
                if (servletInfo.isProvider()) {
                    // Needed for any Provider interface implementations
                    jaxWsServiceFactoryBean.setInvoker(new JAXWSMethodInvoker(instanceService));
                }

                jaxWsServerFactoryBean
                        .setServiceName(new QName(servletInfo.getServiceTargetNamespace(), servletInfo.getServiceName()));
                jaxWsServerFactoryBean.setServiceClass(instanceService.getClass());
                jaxWsServerFactoryBean.setAddress(servletInfo.getRelativePath());
                jaxWsServerFactoryBean.setServiceBean(instanceService);
                if (servletInfo.getWsdlPath() != null) {
                    jaxWsServerFactoryBean.setWsdlLocation(servletInfo.getWsdlPath());
                }
                if (!servletInfo.getFeatures().isEmpty()) {
                    List<Feature> features = new ArrayList<>();
                    for (String feature : servletInfo.getFeatures()) {
                        Feature instanceFeature = getInstance(feature, "feature", endpointType);
                        features.add(instanceFeature);
                    }
                    jaxWsServerFactoryBean.setFeatures(features);
                }
                if (!servletInfo.getHandlers().isEmpty()) {
                    List<javax.xml.ws.handler.Handler> handlers = new ArrayList<>();
                    for (String handler : servletInfo.getHandlers()) {
                        javax.xml.ws.handler.Handler instanceHandler = getInstance(handler, "handler", endpointType);
                        handlers.add(instanceHandler);
                    }
                    jaxWsServerFactoryBean.setHandlers(handlers);
                }
                if (servletInfo.getSOAPBinding() != null) {
                    jaxWsServerFactoryBean.setBindingId(servletInfo.getSOAPBinding());
                }
                if (servletInfo.getEndpointUrl() != null) {
                    jaxWsServerFactoryBean.setPublishedEndpointUrl(servletInfo.getEndpointUrl());
                }

                Server server = jaxWsServerFactoryBean.create();
                for (String className : servletInfo.getInFaultInterceptors()) {
                    Interceptor<? extends Message> interceptor = getInstance(className, "inFaultInterceptor", endpointType);
                    server.getEndpoint().getInFaultInterceptors().add(interceptor);
                }
                for (String className : servletInfo.getInInterceptors()) {
                    Interceptor<? extends Message> interceptor = getInstance(className, "inInterceptor", endpointType);
                    server.getEndpoint().getInInterceptors().add(interceptor);
                }
                for (String className : servletInfo.getOutFaultInterceptors()) {
                    Interceptor<? extends Message> interceptor = getInstance(className, "outFaultInterceptor", endpointType);
                    server.getEndpoint().getOutFaultInterceptors().add(interceptor);
                }
                for (String className : servletInfo.getOutInterceptors()) {
                    Interceptor<? extends Message> interceptor = getInstance(className, "outInterceptor", endpointType);
                    server.getEndpoint().getOutInterceptors().add(interceptor);
                }

                LOGGER.info(servletInfo.toString() + " available.");
            } else {
                LOGGER.error("Cannot initialize " + servletInfo.toString());
            }
        }
    }

    private static Class<?> loadClass(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e1) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e2) {
                e1.addSuppressed(e2);
                throw new RuntimeException("Could not load " + className + " using current thread class loader nor "
                        + CxfHandler.class.getName() + " class loader", e1);
            }
        }
    }

    /**
     * @param <T> a type to which the returned bean can be casted
     * @param beanRef a fully qualified class name or a name of a {@code @Named} bean prefixed with hash mark ({@code '#'})
     * @param namedBeansSupported if {@code true} then the {@code beanRef} argument may contain a name of a {@code @Named} bean;
     *        otherwise only fully qualified class names can be passed via {@code beanRef}
     * @return an instance of a Bean
     */
    private static <T> T getInstance(String beanRef, boolean namedBeansSupported) {
        if (namedBeansSupported && beanRef != null && beanRef.startsWith("#")) {
            final String beanName = beanRef.substring(1);
            return (T) Arc.container().instance(beanName).get();
        }

        Class<T> classObj = (Class<T>) loadClass(beanRef);
        if (classObj != null) {
            try {
                return CDI.current().select(classObj).get();
            } catch (UnsatisfiedResolutionException e) {
                // silent fail
            }
            try {
                return classObj.getConstructor().newInstance();
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    private static <T> T getInstance(String className, String kind, String targetType) {
        try {
            return getInstance(className, true);
        } catch (AmbiguousResolutionException e) {
            /*
             * There are multiple beans of this type
             * and we do not know which one to use
             */
            throw new IllegalStateException("Unable to add a " + kind + " to CXF endpoint " + targetType + ":"
                    + " there are multiple instances of " + className + " available in the CDI container."
                    + " Either make sure there is only one instance available in the container"
                    + " or create a unique subtype of " + className + " and set that one on " + targetType
                    + " or add @javax.inject.Named(\"myName\") to some of the beans and refer to that bean by #myName on "
                    + targetType,
                    e);
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

    /**
     * Leverages the Quarkus HTTP proxy configuration properties,
     * instead of relying on pure HTTP headers, and is based on the method referenced below.
     *
     * @see org.apache.cxf.transport.servlet.AbstractHTTPServlet#checkXForwardedHeaders(HttpServletRequest)
     * @see io.quarkus.vertx.http.runtime.ProxyConfig
     */
    private HttpServletRequest checkXForwardedHeaders(HttpServletRequest request) {
        if (httpConfiguration.proxy.proxyAddressForwarding) {
            String originalProtocol = request.getHeader(X_FORWARDED_PROTO_HEADER);
            String originalRemoteAddr = request.getHeader(X_FORWARDED_FOR_HEADER);
            String originalPrefix = httpConfiguration.proxy.enableForwardedPrefix ? null
                    : request.getHeader(httpConfiguration.proxy.forwardedPrefixHeader);
            String originalHost = httpConfiguration.proxy.enableForwardedHost ? null
                    : request.getHeader(httpConfiguration.proxy.forwardedHostHeader);
            String originalPort = request.getHeader(X_FORWARDED_PORT_HEADER);

            // If at least one of the X-Forwarded-Xxx headers is set, try to use them
            if (Stream.of(originalProtocol, originalRemoteAddr, originalPrefix,
                    originalHost, originalPort).anyMatch(Objects::nonNull)) {
                return new VertxHttpServletRequestXForwardedFilter(request,
                        originalProtocol,
                        originalRemoteAddr,
                        originalPrefix,
                        originalHost,
                        originalPort);
            }
        }

        return request;

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
            VertxHttpServletResponse resp = new VertxHttpServletResponse(event);
            req = checkXForwardedHeaders(req);
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
}
