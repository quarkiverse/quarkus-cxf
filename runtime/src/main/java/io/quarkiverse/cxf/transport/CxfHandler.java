package io.quarkiverse.cxf.transport;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.ServletException;

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
    private String servletPath;
    private ServletController controller;

    private static final Map<String, String> RESPONSE_HEADERS = new HashMap<>();

    static {
        RESPONSE_HEADERS.put("Access-Control-Allow-Origin", "*");
        RESPONSE_HEADERS.put("Access-Control-Allow-Credentials", "true");
        RESPONSE_HEADERS.put("Access-Control-Allow-Methods", ALLOWED_METHODS);
        RESPONSE_HEADERS.put("Access-Control-Allow-Headers", "Content-Type, Authorization");
        RESPONSE_HEADERS.put("Access-Control-Max-Age", "86400");
    }

    public CxfHandler() {
    }

    public CxfHandler(CXFServletInfos cxfServletInfos) {
        LOGGER.trace("CxfHandler created");
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
        for (CXFServletInfo servletInfo : cxfServletInfos.getInfos()) {
            JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean(
                    new QuarkusJaxWsServiceFactoryBean(cxfServletInfos.getWrappersclasses()));
            factory.setDestinationFactory(destinationFactory);
            factory.setBus(bus);
            //suboptimal because done it in loop but not a real issue...
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
            String contextPath = request.path();
            if (contextPath != null) {
                sb.append(contextPath);
            }
            reqPrefix = sb.toString();
        }

        return reqPrefix;
    }

    private void process(RoutingContext event) {
        try {
            VertxHttpServletRequest req = new VertxHttpServletRequest(event, "", servletPath);
            VertxHttpServletResponse resp = new VertxHttpServletResponse(event);
            controller.invoke(req, resp);
            resp.end();
        } catch (ServletException | IOException e) {
            LOGGER.warn("Can not get list of web service.", e);
        }
    }
}
