package io.quarkiverse.cxf.deployment;

import java.io.*;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.extension.ExtensionManagerImpl;
import org.apache.cxf.common.spi.GeneratedClassClassLoaderCapture;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import io.quarkiverse.cxf.CXFRecorder;
import io.quarkiverse.cxf.deployment.CxfWrapperClassNamesBuildItem.Builder;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.UberJarMergedResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.UberJarRequiredBuildItem;
import io.quarkus.gizmo.ClassOutput;

class QuarkusCxfProcessor {

    private static final String FEATURE_CXF = "cxf";
    private static final Logger LOGGER = Logger.getLogger(QuarkusCxfProcessor.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE_CXF);
    }

    @BuildStep
    public void filterLogging(BuildProducer<LogCleanupFilterBuildItem> logCleanupProducer) {
        logCleanupProducer.produce(
                new LogCleanupFilterBuildItem(
                        "org.apache.cxf.wsdl.service.factory.ReflectionServiceFactoryBean", Level.FINE, "Creating Service"));
    }

    @BuildStep
    public void generateWSDL(BuildProducer<NativeImageResourceBuildItem> resources,
            CxfBuildTimeConfig cxfBuildTimeConfig) {
        if (cxfBuildTimeConfig.wsdlPath.isPresent()) {
            for (String wsdlPath : cxfBuildTimeConfig.wsdlPath.get()) {
                resources.produce(new NativeImageResourceBuildItem(wsdlPath));
            }
        }
    }

    @BuildStep
    void markBeansAsUnremovable(BuildProducer<UnremovableBeanBuildItem> unremovables) {
        unremovables.produce(new UnremovableBeanBuildItem(beanInfo -> {
            String nameWithPackage = beanInfo.getBeanClass().local();
            return nameWithPackage.contains(".jaxws_asm") || nameWithPackage.endsWith("ObjectFactory");
        }));
    }

    @BuildStep
    CxfBusBuildItem bus() {
        final Bus bus = BusFactory.getDefaultBus();
        // setup class capturing
        return new CxfBusBuildItem(bus);
    }

    @BuildStep
    CxfWrapperClassNamesBuildItem cxfWrapperClassNames(
            CxfBusBuildItem bus,
            List<CxfClientBuildItem> clients,
            List<CxfEndpointImplementationBuildItem> endpointImplementations,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans) {

        bus.getBus().setExtension(new QuarkusCapture(new GeneratedBeanGizmoAdaptor(generatedBeans)),
                GeneratedClassClassLoaderCapture.class);

        Builder b = CxfWrapperClassNamesBuildItem.builder();

        Stream.concat(
                clients.stream().map(CxfClientBuildItem::getSei),
                endpointImplementations.stream().map(CxfEndpointImplementationBuildItem::getImplementor))
                .forEach(sei -> {
                    final QuarkusJaxWsServiceFactoryBean factoryBean = CxfDeploymentUtils
                            .createQuarkusJaxWsServiceFactoryBean(sei, bus.getBus());
                    b.put(sei, factoryBean.getWrappersClassNames());
                });

        return b.build();

    }

    @BuildStep
    List<UberJarMergedResourceBuildItem> uberJarMergedResourceBuildItem() {
        return Arrays.asList(
                new UberJarMergedResourceBuildItem("META-INF/cxf/bus-extensions.txt"),
                new UberJarMergedResourceBuildItem("META-INF/wsdl.plugin.xml"));
    }

    @BuildStep
    void buildResources(BuildProducer<ReflectiveClassBuildItem> reflectiveItems,
            List<UberJarRequiredBuildItem> uberJarRequired,
            PackageConfig packageConfig,
            BuildProducer<GeneratedResourceBuildItem> generatedResources) {

        try (ByteArrayOutputStream os = new ByteArrayOutputStream();
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os))) {
            Enumeration<URL> urls = ExtensionManagerImpl.class.getClassLoader().getResources("META-INF/cxf/bus-extensions.txt");
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try (InputStream openStream = url.openStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(openStream, StandardCharsets.UTF_8))) {
                    String line = reader.readLine();
                    while (line != null) {
                        String[] cols = line.split(":");
                        // org.apache.cxf.bus.managers.PhaseManagerImpl:org.apache.cxf.phase.PhaseManager:true
                        if (cols.length > 1) {
                            if (cols[0].length() > 0) {
                                reflectiveItems.produce(new ReflectiveClassBuildItem(true, true, cols[0]));
                            }
                            if (cols[1].length() > 0) {
                                reflectiveItems.produce(new ReflectiveClassBuildItem(true, true, cols[1]));
                            }
                        }
                        out.write(line);
                        out.newLine();
                        line = reader.readLine();
                    }
                }
            }

            // for uber jar merge bus-extensions
            if ((!uberJarRequired.isEmpty() || packageConfig.type.equalsIgnoreCase(PackageConfig.UBER_JAR))
                    && (os.size() > 0)) {
                generatedResources.produce(
                        new GeneratedResourceBuildItem("META-INF/cxf/bus-extensions.txt", os.toByteArray()));
            }
        } catch (IOException e) {
            LOGGER.warn("cannot merge bus-extensions.txt");
        }
    }

    @BuildStep
    void buildXmlResources(List<UberJarRequiredBuildItem> uberJarRequired,
            PackageConfig packageConfig,
            BuildProducer<GeneratedResourceBuildItem> generatedResources) {
        // for uber jar only merge xml resource
        if (uberJarRequired.isEmpty() && !packageConfig.type.equalsIgnoreCase(PackageConfig.UBER_JAR)) {
            return;
        }
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            XPath xpath = XPathFactory.newInstance().newXPath();
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");

            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            builder.setEntityResolver(new NoOpEntityResolver());

            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> urls = loader.getResources("META-INF/wsdl.plugin.xml");

            // Create output / merged document
            Document mergedXmlDocument = builder.newDocument();
            Element root = mergedXmlDocument.createElement("properties");
            mergedXmlDocument.appendChild(root);
            for (URL url : Collections.list(urls)) {
                try (InputStreamReader inputStreamReader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
                    Document dDoc = builder.parse(new InputSource(inputStreamReader));
                    NodeList nodeList = (NodeList) xpath.compile("//entry").evaluate(dDoc, XPathConstants.NODESET);
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        Node node = nodeList.item(i);
                        Node copyNode = mergedXmlDocument.importNode(node, true);
                        root.appendChild(copyNode);
                    }
                }
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(mergedXmlDocument),
                    new StreamResult(new OutputStreamWriter(os, StandardCharsets.UTF_8)));

            if (os.size() > 0) {
                generatedResources.produce(
                        new GeneratedResourceBuildItem("META-INF/wsdl.plugin.xml", os.toByteArray()));
            }

        } catch (XPathExpressionException
                | ParserConfigurationException
                | IOException
                | SAXException
                | TransformerException e) {
            LOGGER.warn("cannot merge wsdl.plugin.xml");
        }
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem ssl() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE_CXF);
    }

    @BuildStep
    void runtimeInitializedClasses(
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        // TODO check whether the non-org.apache.cxf classes really need to be here
        Stream.of(
                "io.netty.buffer.PooledByteBufAllocator",
                "io.netty.buffer.UnpooledHeapByteBuf",
                "io.netty.buffer.UnpooledUnsafeHeapByteBuf",
                "io.netty.buffer.UnpooledByteBufAllocator$InstrumentedUnpooledUnsafeHeapByteBuf",
                "io.netty.buffer.AbstractReferenceCountedByteBuf",
                "org.apache.cxf.attachment.AttachmentSerializer",
                "org.apache.cxf.attachment.AttachmentUtil",
                "org.apache.cxf.attachment.ImageDataContentHandler",
                "org.apache.cxf.endpoint.ClientImpl",
                "org.apache.cxf.interceptor.AttachmentOutInterceptor",
                "org.apache.cxf.interceptor.OneWayProcessorInterceptor",
                "org.apache.cxf.interceptor.OneWayProcessorInterceptor$1",
                "org.apache.cxf.phase.PhaseInterceptorChain",
                "org.apache.cxf.service.factory.AbstractServiceFactoryBean",
                "org.apache.cxf.staxutils.validation.W3CMultiSchemaFactory",
                "org.apache.cxf.transport.http.HTTPConduit",
                "org.apache.cxf.transport.http.HTTPConduit$WrappedOutputStream",
                "org.apache.cxf.transport.http.HTTPConduit$WrappedOutputStream$1",
                "org.apache.cxf.ws.addressing.impl.InternalContextUtils",
                "org.apache.cxf.ws.addressing.impl.InternalContextUtils$1")
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClass::produce);

        final IndexView index = combinedIndexBuildItem.getIndex();
        Stream.of(
                /* org.apache.cxf.configuration.blueprint package is not present in some downstream rebuilds of CXF */
                "org.apache.cxf.configuration.blueprint.AbstractBPBeanDefinitionParser")
                .filter(cl -> index.getClassByName(DotName.createSimple(cl)) != null)
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClass::produce);

    }

    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        Stream.of(
                "io.quarkiverse.cxf:quarkus-cxf",
                "org.apache.cxf:cxf-core",
                "org.apache.cxf:cxf-rt-bindings-soap",
                "org.apache.cxf:cxf-rt-bindings-xml",
                "org.apache.cxf:cxf-rt-frontend-jaxws",
                "org.apache.cxf:cxf-rt-databinding-jaxb",
                "org.apache.cxf:cxf-rt-frontend-simple",
                "org.apache.cxf:cxf-rt-transports-http",
                "org.apache.cxf:cxf-rt-wsdl",
                "org.apache.cxf:cxf-rt-ws-addr",
                "org.apache.cxf:cxf-rt-ws-policy")
                .forEach(ga -> {
                    String[] coords = ga.split(":");
                    indexDependency.produce(new IndexDependencyBuildItem(coords[0], coords[1]));
                });
    }

    @BuildStep
    void httpProxies(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxies) {
        IndexView index = combinedIndexBuildItem.getIndex();
        proxies.produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.txw2.TypedXmlWriter"));
        Set<String> proxiesCreated = new HashSet<>();
        DotName typedXmlWriterDN = DotName.createSimple("com.sun.xml.txw2.TypedXmlWriter");
        // getAllKnownDirectImplementors skip interface, so I have to do it myself.
        produceRecursiveProxies(index, typedXmlWriterDN, proxies, proxiesCreated);
    }

    @BuildStep
    void reflectiveClasses(
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<UnremovableBeanBuildItem> unremovables) {
        IndexView index = combinedIndexBuildItem.getIndex();

        Stream.of(
                "org.apache.cxf.databinding.DataBinding",
                "org.apache.cxf.interceptor.Interceptor",
                "org.apache.cxf.binding.soap.interceptor.SoapInterceptor",
                "org.apache.cxf.phase.PhaseInterceptor")
                .map(DotName::createSimple)
                .flatMap(dotName -> index.getAllKnownImplementors(dotName).stream())
                .map(classInfo -> classInfo.name().toString())
                .filter(className -> !className.startsWith("org.apache.cxf.") || !className.contains(".blueprint."))
                .map(className -> new ReflectiveClassBuildItem(false, false, className))
                .forEach(reflectiveClass::produce);

        Stream.of(
                "org.apache.cxf.feature.Feature")
                .map(DotName::createSimple)
                .flatMap(dotName -> index.getAllKnownImplementors(dotName).stream())
                .map(classInfo -> classInfo.name().toString())
                .map(className -> new ReflectiveClassBuildItem(true, false, className))
                .forEach(reflectiveClass::produce);

        for (AnnotationInstance xmlNamespaceInstance : index
                .getAnnotations(DotName.createSimple("com.sun.xml.txw2.annotation.XmlNamespace"))) {
            reflectiveClass.produce(
                    new ReflectiveClassBuildItem(true, true, xmlNamespaceInstance.target().asClass().name().toString()));
        }

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                "org.apache.cxf.common.logging.Slf4jLogger"));

        final Set<String> extensibilities = index.getKnownClasses().stream()
                .map(classInfo -> classInfo.name().toString())
                .filter(className -> className.startsWith("io.quarkiverse.cxf.extensibility")
                        && className.endsWith("Extensibility"))
                .collect(Collectors.toSet());
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, extensibilities.toArray(new String[0])));
        unremovables
                .produce(new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanClassNamesExclusion(extensibilities)));

        /* Referenced from io.quarkiverse.cxf.graal.Target_org_apache_cxf_endpoint_dynamic_ExceptionClassGenerator */
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "io.quarkiverse.cxf.CXFException"));

        /* Referenced from io.quarkiverse.cxf.graal.Target_org_apache_cxf_common_spi_NamespaceClassGenerator */
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "org.apache.cxf.common.jaxb.NamespaceMapper"));
    }

    @BuildStep
    void serviceProviders(BuildProducer<ServiceProviderBuildItem> serviceProvider) {

        serviceProvider.produce(
                new ServiceProviderBuildItem(
                        "org.apache.cxf.bus.factory",
                        "org.apache.cxf.bus.CXFBusFactory"));

    }

    void produceRecursiveProxies(IndexView index,
            DotName interfaceDN,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxies, Set<String> proxiesCreated) {
        index.getKnownDirectImplementors(interfaceDN).stream()
                .filter(classinfo -> Modifier.isInterface(classinfo.flags()))
                .map(ClassInfo::name)
                .forEach(className -> {
                    if (!proxiesCreated.contains(className.toString())) {
                        proxies.produce(new NativeImageProxyDefinitionBuildItem(className.toString()));
                        produceRecursiveProxies(index, className, proxies, proxiesCreated);
                        proxiesCreated.add(className.toString());
                    }
                });

    }

    @BuildStep
    void httpProxies(BuildProducer<NativeImageProxyDefinitionBuildItem> proxies) {
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.jaxb.JAXBContextProxy"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.jaxb.JAXBBeanInfo"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.jaxb.JAXBUtils$BridgeWrapper"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.jaxb.JAXBUtils$SchemaCompiler"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.util.ASMHelper$ClassWriter"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.binding.soap.wsdl.extensions.SoapBinding"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.binding.soap.wsdl.extensions.SoapAddress"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.binding.soap.wsdl.extensions.SoapHeader"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.binding.soap.wsdl.extensions.SoapBody"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.binding.soap.wsdl.extensions.SoapFault"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.binding.soap.wsdl.extensions.SoapOperation"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.binding.soap.wsdl.extensions.SoapHeaderFault"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.jaxb.JAXBUtils$S2JJAXBModel"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.jaxb.JAXBUtils$Options"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.jaxb.JAXBUtils$JCodeModel"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.jaxb.JAXBUtils$Mapping"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.jaxb.JAXBUtils$TypeAndAnnotation"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.jaxb.JAXBUtils$JType"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.jaxb.JAXBUtils$JPackage"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.jaxb.JAXBUtils$JDefinedClass"));
    }

    @BuildStep
    NativeImageResourceBundleBuildItem nativeImageResourceBundleBuildItem() {
        return new NativeImageResourceBundleBuildItem("org.apache.cxf.interceptor.Messages");
    }

    @BuildStep
    NativeImageResourceBuildItem nativeImageResourceBuildItem() {
        // TODO add @HandlerChain (file) and parse it to add class loading
        return new NativeImageResourceBuildItem("com/sun/xml/fastinfoset/resources/ResourceBundle.properties",
                "META-INF/cxf/bus-extensions.txt",
                "META-INF/cxf/cxf.xml",
                "META-INF/cxf/org.apache.cxf.bus.factory",
                "META-INF/blueprint.handlers",
                "META-INF/spring.handlers",
                "META-INF/spring.schemas",
                "META-INF/jax-ws-catalog.xml",
                "OSGI-INF/metatype/workqueue.xml",
                "schemas/core.xsd",
                "schemas/blueprint/core.xsd",
                "schemas/wsdl/XMLSchema.xsd",
                "schemas/wsdl/addressing.xjb",
                "schemas/wsdl/addressing.xsd",
                "schemas/wsdl/addressing200403.xjb",
                "schemas/wsdl/addressing200403.xsd",
                "schemas/wsdl/http.xjb",
                "schemas/wsdl/http.xsd",
                "schemas/wsdl/mime-binding.xsd",
                "schemas/wsdl/soap-binding.xsd",
                "schemas/wsdl/soap-encoding.xsd",
                "schemas/wsdl/soap12-binding.xsd",
                "schemas/wsdl/swaref.xsd",
                "schemas/wsdl/ws-addr-wsdl.xjb",
                "schemas/wsdl/ws-addr-wsdl.xsd",
                "schemas/wsdl/ws-addr.xsd",
                "schemas/wsdl/wsdl.xjb",
                "schemas/wsdl/wsdl.xsd",
                "schemas/wsdl/wsrm.xsd",
                "schemas/wsdl/xmime.xsd",
                "schemas/wsdl/xml.xsd",
                "schemas/configuratio/cxf-beans.xsd",
                "schemas/configuration/extension.xsd",
                "schemas/configuration/parameterized-types.xsd",
                "schemas/configuration/security.xjb",
                "schemas/configuration/security.xsd");
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void shutDown(
            CXFRecorder recorder,
            ShutdownContextBuildItem shutdownContext) {
        recorder.resetDestinationRegistry(shutdownContext);
    }

    private static final class NoOpEntityResolver implements EntityResolver {
        @Override
        public InputSource resolveEntity(String publicId, String systemId) {
            LOGGER.info("Preventing access to " + systemId);
            return new InputSource(new StringReader(""));
        }
    }

    private static class QuarkusCapture implements GeneratedClassClassLoaderCapture {
        private final ClassOutput classOutput;

        QuarkusCapture(ClassOutput classOutput) {
            this.classOutput = classOutput;

        }

        @Override
        public void capture(String name, byte[] bytes) {
            classOutput.getSourceWriter(name);
            LOGGER.trace("capture generation of " + name);
            classOutput.write(name, bytes);
        }
    }

}
