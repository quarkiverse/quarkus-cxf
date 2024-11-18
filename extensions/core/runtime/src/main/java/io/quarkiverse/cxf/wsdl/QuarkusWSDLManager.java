package io.quarkiverse.cxf.wsdl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.XMLStreamReaderWrapper;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.TransportURIResolver;
import org.apache.cxf.wsdl11.CatalogWSDLLocator;
import org.apache.cxf.wsdl11.ResourceManagerWSDLLocator;
import org.apache.cxf.wsdl11.WSDLManagerImpl;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Temporary workaround for https://github.com/quarkiverse/quarkus-cxf/issues/1608
 */
public class QuarkusWSDLManager extends WSDLManagerImpl {

    private final ExtensionRegistry registry;
    private XMLStreamReaderWrapper xmlStreamReaderWrapper;

    public static QuarkusWSDLManager newInstance(Bus b) {
        try {
            return new QuarkusWSDLManager(b);
        } catch (BusException e) {
            throw new RuntimeException(e);
        }
    }

    private QuarkusWSDLManager(Bus b) throws BusException {
        super();
        this.registry = getField(WSDLManagerImpl.class, this, "registry");
        setBus(b);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Class<?> cl, Object inst, String fieldName) {
        try {
            final Field fld = cl.getDeclaredField(fieldName);
            fld.setAccessible(true);
            return (T) fld.get(inst);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setField(Class<?> cl, Object inst, String fieldName, Object value) {
        try {
            final Field fld = cl.getDeclaredField(fieldName);
            fld.setAccessible(true);
            fld.set(inst, value);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({ "removal", "deprecation" })
    @Override
    protected Definition loadDefinition(String url) throws WSDLException {
        final Bus bus = getBus();
        final WSDLReader reader = getWSDLFactory().newWSDLReader();
        reader.setFeature("javax.wsdl.verbose", false);
        reader.setFeature("javax.wsdl.importDocuments", true);
        reader.setExtensionRegistry(registry);

        //we'll create a new String here to make sure the passed in key is not referenced in the loading of
        //the wsdl and thus would be held onto from the cached map from both the weak reference (key) and
        //from the strong reference (Definition).  For example, the Definition sometimes keeps the original
        //string as the documentBaseLocation which would result in it being held onto strongly
        //from the definition.  With this, the String the definition holds onto would be unique
        url = new String(url);
        CatalogWSDLLocator catLocator = new CatalogWSDLLocator(url, bus);
        setField(CatalogWSDLLocator.class, catLocator, "resolver", new QuarkusTransportURIResolver(bus));

        final ResourceManagerWSDLLocator wsdlLocator = new ResourceManagerWSDLLocator(url,
                catLocator,
                bus);
        InputSource src = wsdlLocator.getBaseInputSource();
        final Definition def;
        if (src.getByteStream() != null || src.getCharacterStream() != null) {
            final Document doc;
            XMLStreamReader xmlReader = null;
            try {
                xmlReader = StaxUtils.createXMLStreamReader(src);
                if (xmlStreamReaderWrapper != null) {
                    xmlReader = xmlStreamReaderWrapper.wrap(xmlReader);
                }
                doc = StaxUtils.read(xmlReader, true);
                if (src.getSystemId() != null) {
                    try {
                        doc.setDocumentURI(new String(src.getSystemId()));
                    } catch (Exception e) {
                        //ignore - probably not DOM level 3
                    }
                }
            } catch (Exception e) {
                throw new WSDLException(WSDLException.PARSER_ERROR, e.getMessage(), e);
            } finally {
                try {
                    StaxUtils.close(xmlReader);
                } catch (XMLStreamException ex) {
                    throw new WSDLException(WSDLException.PARSER_ERROR, ex.getMessage(), ex);
                }
            }

            // This is needed to avoid security exceptions when running with a security manager
            if (System.getSecurityManager() == null) {
                def = reader.readWSDL(wsdlLocator, doc.getDocumentElement());
            } else {
                try {
                    def = AccessController.doPrivileged((PrivilegedExceptionAction<Definition>) () -> reader
                            .readWSDL(wsdlLocator, doc.getDocumentElement()));
                } catch (PrivilegedActionException paex) {
                    throw new WSDLException(WSDLException.PARSER_ERROR, paex.getMessage(), paex);
                }
            }
        } else {
            if (System.getSecurityManager() == null) {
                def = reader.readWSDL(wsdlLocator);
            } else {
                try {
                    def = AccessController
                            .doPrivileged((PrivilegedExceptionAction<Definition>) () -> reader.readWSDL(wsdlLocator));
                } catch (PrivilegedActionException paex) {
                    throw new WSDLException(WSDLException.PARSER_ERROR, paex.getMessage(), paex);
                }
            }
        }

        return def;
    }

    @Override
    public void setXMLStreamReaderWrapper(XMLStreamReaderWrapper wrapper) {
        super.setXMLStreamReaderWrapper(wrapper);
        this.xmlStreamReaderWrapper = wrapper;
    }

    static class QuarkusTransportURIResolver extends TransportURIResolver {
        static final Logger LOG = LogUtils.getL7dLogger(TransportURIResolver.class);
        private static final Set<String> DEFAULT_URI_RESOLVER_HANDLES = new HashSet<>();
        static {
            //bunch we really don't want to have the conduits checked for
            //as we know the conduits don't handle.  No point
            //wasting the time checking/loading conduits and such
            DEFAULT_URI_RESOLVER_HANDLES.add("file");
            DEFAULT_URI_RESOLVER_HANDLES.add("classpath");
            DEFAULT_URI_RESOLVER_HANDLES.add("wsjar");
            DEFAULT_URI_RESOLVER_HANDLES.add("jar");
            DEFAULT_URI_RESOLVER_HANDLES.add("zip");
        }

        public QuarkusTransportURIResolver(Bus b) {
            super(b);
        }

        @Override
        public InputSource resolve(String curUri, String baseUri) {
            // Spaces must be encoded or URI.resolve() will choke
            curUri = curUri.replace(" ", "%20");

            InputSource is = null;
            URI base;
            try {
                if (baseUri == null) {
                    base = new URI(curUri);
                } else {
                    base = new URI(baseUri);
                    base = base.resolve(curUri);
                }
            } catch (URISyntaxException use) {
                //ignore
                base = null;
                LOG.log(Level.FINEST, "Could not resolve curUri " + curUri, use);
            }
            try {
                if (base == null
                        || DEFAULT_URI_RESOLVER_HANDLES.contains(base.getScheme())) {
                    is = super.resolve(curUri, baseUri);
                }
            } catch (Exception ex) {
                //nothing
                LOG.log(Level.FINEST, "Default URI handlers could not resolve " + baseUri + " " + curUri, ex);
            }
            if (is == null && base != null
                    && base.getScheme() != null
                    && !DEFAULT_URI_RESOLVER_HANDLES.contains(base.getScheme())) {
                try {
                    ConduitInitiatorManager mgr = bus.getExtension(ConduitInitiatorManager.class);
                    ConduitInitiator ci = null;
                    if ("http".equals(base.getScheme()) || "https".equals(base.getScheme())) {
                        //common case, don't "search"
                        ci = mgr.getConduitInitiator("http://cxf.apache.org/transports/http");
                    }
                    if (ci == null) {
                        ci = mgr.getConduitInitiatorForUri(base.toString());
                    }
                    if (ci != null) {
                        EndpointInfo info = new EndpointInfo();
                        // set the endpointInfo name which could be used for configuration
                        info.setName(new QName("http://cxf.apache.org", "TransportURIResolver"));
                        info.setAddress(base.toString());
                        Conduit c = ci.getConduit(info, bus);
                        Message message = new MessageImpl();
                        Exchange exch = new ExchangeImpl();
                        exch.put(Bus.class, bus);
                        message.setExchange(exch);

                        message.put(Message.HTTP_REQUEST_METHOD, "GET");
                        c.setMessageObserver(new MessageObserver() {
                            @Override
                            public void onMessage(Message message) {
                                LoadingByteArrayOutputStream bout = new LoadingByteArrayOutputStream();
                                try {
                                    IOUtils.copyAndCloseInput(message.getContent(InputStream.class), bout);
                                    message.getExchange().put(InputStream.class, bout.createInputStream());
                                } catch (IOException e) {
                                    //ignore
                                }
                            }
                        });
                        c.prepare(message);
                        c.close(message);
                        if (exch.getInMessage() != null) {
                            c.close(exch.getInMessage());
                        }
                        if (exch.getInFaultMessage() != null) {
                            c.close(exch.getInFaultMessage());
                        }
                        c.close();
                        InputStream ins = exch.get(InputStream.class);
                        resourceOpened.add(ins);
                        InputSource src = new InputSource(ins);
                        String u = (String) message.get("transport.retransmit.url");
                        if (u == null) {
                            u = base.toString();
                        }
                        src.setPublicId(u);
                        src.setSystemId(u);
                        lastestImportUri = u;
                        currentResolver.unresolve();
                        return src;
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Conduit initiator could not resolve " + baseUri + " " + curUri, e);
                }
            }
            if (is == null
                    && (base == null
                            || base.getScheme() == null
                            || !DEFAULT_URI_RESOLVER_HANDLES.contains(base.getScheme()))) {
                is = super.resolve(curUri, baseUri);
            }
            return is;
        }
    }

}
