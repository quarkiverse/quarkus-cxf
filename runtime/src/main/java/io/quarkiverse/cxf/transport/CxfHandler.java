package io.quarkiverse.cxf.transport;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.DestinationRegistryImpl;
import org.apache.cxf.transport.servlet.BaseUrlHelper;
import org.apache.cxf.transport.servlet.ServletController;
import org.apache.cxf.transport.servlet.servicelist.ServiceListGeneratorServlet;
import org.jboss.logging.Logger;

import io.quarkiverse.cxf.CXFServletInfo;
import io.quarkiverse.cxf.CXFServletInfos;
import io.quarkiverse.cxf.QuarkusJaxWsServiceFactoryBean;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class CxfHandler implements Handler<RoutingContext> {
    private static final Logger LOGGER = Logger.getLogger(CxfHandler.class);
    private static final String ALLOWED_METHODS = "POST, GET, PUT, DELETE, HEAD, OPTIONS, TRACE";
    private ServiceListGeneratorServlet serviceListGeneratorServlet;
    private Bus bus;
    private ClassLoader loader;
    private DestinationRegistry destinationRegistry;
    private String contextPath;
    private String servletPath;
    private ServletController controller;
    private BeanContainer beanContainer;
    private CurrentIdentityAssociation association;
    private IdentityProviderManager identityProviderManager;
    private CurrentVertxRequest currentVertxRequest;
    private HttpConfiguration httpConfiguration;

    private static final Map<String, String> RESPONSE_HEADERS = new HashMap<>();

    private static final String X_FORWARDED_PROTO_HEADER = "X-Forwarded-Proto";
    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String X_FORWARDED_PREFIX_HEADER = "X-Forwarded-Prefix";
    private static final String X_FORWARDED_HOST_HEADER = "X-Forwarded-Host";
    private static final String X_FORWARDED_PORT_HEADER = "X-Forwarded-Port";

    static {
        RESPONSE_HEADERS.put("Access-Control-Allow-Origin", "*");
        RESPONSE_HEADERS.put("Access-Control-Allow-Credentials", "true");
        RESPONSE_HEADERS.put("Access-Control-Allow-Methods", ALLOWED_METHODS);
        RESPONSE_HEADERS.put("Access-Control-Allow-Headers", "Content-Type, Authorization");
        RESPONSE_HEADERS.put("Access-Control-Max-Age", "86400");
    }

    public CxfHandler() {
    }

    public CxfHandler(CXFServletInfos cxfServletInfos, BeanContainer beanContainer, HttpConfiguration httpConfiguration) {
        LOGGER.trace("CxfHandler created");
        this.beanContainer = beanContainer;
        this.httpConfiguration = httpConfiguration;
        Instance<CurrentIdentityAssociation> association = CDI.current().select(CurrentIdentityAssociation.class);
        this.association = association.isResolvable() ? association.get() : null;
        Instance<IdentityProviderManager> identityProviderManager = CDI.current().select(IdentityProviderManager.class);
        this.identityProviderManager = identityProviderManager.isResolvable() ? identityProviderManager.get() : null;
        this.currentVertxRequest = CDI.current().select(CurrentVertxRequest.class).get();
        if (cxfServletInfos == null || cxfServletInfos.getInfos() == null || cxfServletInfos.getInfos().isEmpty()) {
            LOGGER.warn("no info transmit to servlet");
            return;
        }
        this.bus = BusFactory.getDefaultBus();
        BusFactory.setDefaultBus(bus);
        this.loader = this.bus.getExtension(ClassLoader.class);

        LOGGER.trace("load destination");
        DestinationFactoryManager dfm = this.bus.getExtension(DestinationFactoryManager.class);
        destinationRegistry = new DestinationRegistryImpl();
        VertxDestinationFactory destinationFactory = new VertxDestinationFactory(destinationRegistry);
        dfm.registerDestinationFactory("http://cxf.apache.org/transports/quarkus", destinationFactory);
        ConduitInitiatorManager extension = bus.getExtension(ConduitInitiatorManager.class);
        extension.registerConduitInitiator("http://cxf.apache.org/transports/quarkus", destinationFactory);

        serviceListGeneratorServlet = new ServiceListGeneratorServlet(destinationRegistry, bus);
        VertxServletConfig servletConfig = new VertxServletConfig();
        serviceListGeneratorServlet.init(servletConfig);
        this.controller = new ServletController(destinationRegistry, servletConfig, serviceListGeneratorServlet);
        serviceListGeneratorServlet.init(new VertxServletConfig());
        servletPath = cxfServletInfos.getPath();
        contextPath = cxfServletInfos.getContextPath();
        for (CXFServletInfo servletInfo : cxfServletInfos.getInfos()) {
            JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean(
                    new QuarkusJaxWsServiceFactoryBean(cxfServletInfos.getWrappersclasses()));
            factory.setDestinationFactory(destinationFactory);
            factory.setBus(bus);
            //suboptimal because done it in loop but not a real issue...
            Object instanceService = getInstance(servletInfo.getClassName());
            if (instanceService != null) {
                factory.setServiceClass(instanceService.getClass());
                factory.setAddress(servletInfo.getRelativePath());
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
            return CDI.current().select(classObj).get();
        } catch (UnsatisfiedResolutionException e) {
            //silent fail
        }
        try {
            return classObj.getConstructor().newInstance();
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
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

    protected void generateNotFound(HttpServerRequest request, HttpServerResponse res) {
        res.setStatusCode(404);
        res.headers().add("Content-Type", "text/html");
        res.end("<html><body>No service was found.</body></html>");
    }

    protected void updateDestination(HttpServerRequest request, AbstractHTTPDestination d) {
        String base = getBaseURL(request);
        String ad = d.getEndpointInfo().getAddress();
        if (ad == null && d.getAddress() != null && d.getAddress().getAddress() != null) {
            ad = d.getAddress().getAddress().getValue();
            if (ad == null) {
                ad = "/";
            }
        }

        if (ad != null && !ad.startsWith("http")) {
            BaseUrlHelper.setAddress(d, base + ad);
        }

    }

    private String getBaseURL(HttpServerRequest request) {
        String reqPrefix = request.uri();
        String pathInfo = request.path();
        if (!"/".equals(pathInfo) || reqPrefix.contains(";")) {
            StringBuilder sb = new StringBuilder();
            URI uri = URI.create(reqPrefix);
            sb.append(uri.getScheme()).append("://").append(uri.getRawAuthority());
            String path = request.path();
            if (path != null) {
                sb.append(path);
            }
            reqPrefix = sb.toString();
        }

        return reqPrefix;
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
        } catch (IOException ioe) {
            LOGGER.warn("Cannot list or instantiate web service", ioe);
            event.fail(404, ioe);
        } finally {
            if (requestContext.isActive()) {
                requestContext.terminate();
            }
        }
    }
}
