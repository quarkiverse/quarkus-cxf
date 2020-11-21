package io.quarkiverse.cxf;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.servlet.BaseUrlHelper;
import org.jboss.logging.Logger;

import io.quarkiverse.cxf.transport.UndertowDestinationFactory;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class CxfHandler implements Handler<RoutingContext> {
    private static final Logger LOGGER = Logger.getLogger(CxfHandler.class);
    private static final String ALLOWED_METHODS = "POST, GET, PUT, DELETE, HEAD, OPTIONS, TRACE";
    private static final String QUERY_PARAM_FORMAT = "format";
    private Bus bus;
    private ClassLoader loader;
    private DestinationRegistry destinationRegistry;
    private boolean loadBus;
    protected String serviceListRelativePath = "/services";

    private static final Map<String, String> RESPONSE_HEADERS = new HashMap<>();

    static {
        RESPONSE_HEADERS.put("Access-Control-Allow-Origin", "*");
        RESPONSE_HEADERS.put("Access-Control-Allow-Credentials", "true");
        RESPONSE_HEADERS.put("Access-Control-Allow-Methods", ALLOWED_METHODS);
        RESPONSE_HEADERS.put("Access-Control-Allow-Headers", "Content-Type, Authorization");
        RESPONSE_HEADERS.put("Access-Control-Max-Age", "86400");
    }

    public CxfHandler() {
        loadBus = false;
    }

    protected DestinationRegistry getDestinationRegistryFromBusOrDefault() {
        DestinationFactoryManager dfm = this.bus.getExtension(DestinationFactoryManager.class);
        UndertowDestinationFactory soapDF = new UndertowDestinationFactory();
        dfm.registerDestinationFactory("http://cxf.apache.org/transports/quarkus", soapDF);
        try {
            UndertowDestinationFactory df = (UndertowDestinationFactory) dfm
                    .getDestinationFactory("http://cxf.apache.org/transports/quarkus");
            return df.getRegistry();
        } catch (BusException ex) {
            //ignored
        }
        return null;
    }

    public Bus getBus() {
        return this.bus;
    }

    public void init() {
        if (this.bus == null && this.loadBus) {
            this.bus = BusFactory.getDefaultBus();
        }
        if (this.bus != null) {
            this.loader = this.bus.getExtension(ClassLoader.class);
            if (this.destinationRegistry == null) {
                this.destinationRegistry = this.getDestinationRegistryFromBusOrDefault();
            }
        }
    }

    @Override
    public void handle(RoutingContext event) {
        HttpServerRequest req = event.request();
        HttpServerResponse resp = event.response();
        ClassLoaderUtils.ClassLoaderHolder origLoader = null;
        Bus origBus = null;
        try {
            if (this.loader != null) {
                origLoader = ClassLoaderUtils.setThreadContextClassloader(this.loader);
            }

            if (this.bus != null) {
                origBus = BusFactory.getAndSetThreadDefaultBus(this.bus);
            }

            process(req, resp);
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

    private void process(HttpServerRequest request, HttpServerResponse res) {
        String pathInfo = request.path() == null ? "" : request.path();
        AbstractHTTPDestination d = this.destinationRegistry.getDestinationForPath(pathInfo, true);
        if (d == null) {
            if ((request.uri().endsWith(this.serviceListRelativePath)
                    || request.uri().endsWith(this.serviceListRelativePath + "/")
                    || StringUtils.isEmpty(pathInfo)
                    || "/".equals(pathInfo))) {

                //TODO list of services (ServiceListGeneratorServlet)
            } else {
                d = this.destinationRegistry.checkRestfulRequest(pathInfo);
                if (d == null || d.getMessageObserver() == null) {
                    LOGGER.warn("Can't find the request for " + request.uri() + "'s Observer ");
                    this.generateNotFound(request, res);
                    return;
                }
            }
        }

        if (d != null && d.getMessageObserver() != null) {
            Bus bus = d.getBus();
            ClassLoaderUtils.ClassLoaderHolder orig = null;

            try {
                if (bus != null) {
                    ClassLoader loader = bus.getExtension(ClassLoader.class);
                    if (loader == null) {
                        ResourceManager manager = bus.getExtension(ResourceManager.class);
                        if (manager != null) {
                            loader = manager.resolveResource("", ClassLoader.class);
                        }
                    }

                    if (loader != null) {
                        orig = ClassLoaderUtils.setThreadContextClassloader(loader);
                    }
                }

                this.updateDestination(request, d);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Service http request on thread: " + Thread.currentThread());
                }

                try {
                    //todo call undertowDestination special invoke
                    //d.invoke(request, res);
                } finally {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Finished servicing http request on thread: " + Thread.currentThread());
                    }

                }
            } finally {
                if (orig != null) {
                    orig.reset();
                }

            }
        }

    }
}
