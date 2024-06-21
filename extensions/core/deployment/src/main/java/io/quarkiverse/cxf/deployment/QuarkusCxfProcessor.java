package io.quarkiverse.cxf.deployment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.HostnameVerifier;
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

import jakarta.xml.ws.WebFault;
import jakarta.xml.ws.handler.Handler;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.soap.interceptor.SoapInterceptor;
import org.apache.cxf.bus.extension.ExtensionManagerImpl;
import org.apache.cxf.common.spi.GeneratedClassClassLoaderCapture;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.phase.PhaseInterceptor;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import io.quarkiverse.cxf.CXFRecorder;
import io.quarkiverse.cxf.QuarkusBusFactory;
import io.quarkiverse.cxf.deployment.CxfBuildTimeConfig.Wsdl2JavaParameterSet;
import io.quarkiverse.cxf.deployment.codegen.Wsdl2JavaCodeGen;
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
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.PackageConfig.JarConfig.JarType;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.UberJarMergedResourceBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.gizmo.ClassOutput;

class QuarkusCxfProcessor {
    private static final Logger LOGGER = Logger.getLogger(QuarkusCxfProcessor.class);

    @BuildStep
    FeatureBuildItem feature() {
        return QuarkusCxfFeature.CXF.asFeature();
    }

    @BuildStep
    public void filterLogging(BuildProducer<LogCleanupFilterBuildItem> logCleanupProducer) {
        logCleanupProducer.produce(
                new LogCleanupFilterBuildItem(
                        "org.apache.cxf.wsdl.service.factory.ReflectionServiceFactoryBean", Level.FINE, "Creating Service"));
    }

    @BuildStep
    public void generateWSDL(
            OutputTargetBuildItem target,
            BuildProducer<NativeImageResourceBuildItem> resources,
            CxfBuildTimeConfig cxfBuildTimeConfig) {
        if (cxfBuildTimeConfig.wsdlPath().isPresent()) {
            for (String wsdlPath : cxfBuildTimeConfig.wsdlPath().get()) {
                resources.produce(new NativeImageResourceBuildItem(wsdlPath));
            }
        }

        /* Add all WSDLs configured for wsdl2java processing */
        final Path classesDir = target.getOutputDirectory().resolve("classes");
        if (Files.isDirectory(classesDir)) {
            final Set<String> wsdlResourcePaths = new LinkedHashSet<>();
            scanWsdls(
                    classesDir,
                    cxfBuildTimeConfig.codegen().wsdl2java().rootParameterSet(),
                    Wsdl2JavaCodeGen.WSDL2JAVA_CONFIG_KEY_PREFIX,
                    wsdlResourcePaths::add);

            for (Entry<String, Wsdl2JavaParameterSet> en : cxfBuildTimeConfig.codegen().wsdl2java().namedParameterSets()
                    .entrySet()) {
                scanWsdls(
                        target.getOutputDirectory(),
                        en.getValue(),
                        Wsdl2JavaCodeGen.WSDL2JAVA_NAMED_CONFIG_KEY_PREFIX + en.getKey(),
                        wsdlResourcePaths::add);
            }

            if (!wsdlResourcePaths.isEmpty()) {
                resources.produce(new NativeImageResourceBuildItem(new ArrayList<>(wsdlResourcePaths)));
            }
        }

    }

    static void scanWsdls(
            Path inputDir,
            Wsdl2JavaParameterSet params,
            String prefix,
            Consumer<String> resourcePathConsumer) {
        Wsdl2JavaCodeGen.scan(
                inputDir,
                params.includes().isPresent() ? params.includes()
                        : Optional.of(Collections.emptyList()),
                params.excludes(),
                prefix,
                new HashMap<>(),
                path -> resourcePathConsumer.accept(inputDir.relativize(path).toString().replace('\\', '/')));
    }

    @BuildStep
    void markBeansAsUnremovable(BuildProducer<UnremovableBeanBuildItem> unremovables) {
        unremovables.produce(new UnremovableBeanBuildItem(beanInfo -> {
            String nameWithPackage = beanInfo.getBeanClass().local();
            return nameWithPackage.contains(".jaxws_asm") || nameWithPackage.endsWith("ObjectFactory");
        }));
    }

    @BuildStep
    BuildTimeBusBuildItem bus() {
        final Bus bus = BusFactory.getDefaultBus();
        // setup class capturing
        return new BuildTimeBusBuildItem(bus);
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void systemProperty(BuildProducer<SystemPropertyBuildItem> systemProperty) {
        /* Workaround for https://github.com/quarkiverse/quarkus-cxf/issues/1078 */
        systemProperty
                .produce(new SystemPropertyBuildItem(BusFactory.BUS_FACTORY_PROPERTY_NAME, QuarkusBusFactory.class.getName()));
    }

    @BuildStep
    void generateRuntimeBusServiceFile(BuildProducer<GeneratedResourceBuildItem> generatedResources) {
        /*
         * If we simply stored io.quarkiverse.cxf.deployment.QuarkusBusFactory
         * to META-INF/services/org.apache.cxf.bus.factory in the runtime module
         * then it would be also visible for the deployment module, where we want to configure
         * the bus extensions differently
         */
        byte[] serviceFileContent = QuarkusBusFactory.class.getName().getBytes(StandardCharsets.UTF_8);
        generatedResources.produce(
                new GeneratedResourceBuildItem(
                        "META-INF/services/" + BusFactory.BUS_FACTORY_PROPERTY_NAME,
                        serviceFileContent));
    }

    @BuildStep
    void generateClasses(
            BuildTimeBusBuildItem busBuildItem,
            List<ClientSeiBuildItem> clients,
            List<ServiceSeiBuildItem> endpointImplementations,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        QuarkusCapture capture = new QuarkusCapture(new GeneratedBeanGizmoAdaptor(generatedBeans));
        final Bus bus = busBuildItem.getBus();
        final GeneratedClassClassLoaderCapture oldCapture = bus.getExtension(GeneratedClassClassLoaderCapture.class);
        bus.setExtension(capture, GeneratedClassClassLoaderCapture.class);
        try {
            final Random rnd = new Random(System.currentTimeMillis());
            endpointImplementations.stream()
                    .map(ServiceSeiBuildItem::getSei)
                    .distinct()
                    .forEach(sei -> {
                        LOGGER.debugf("Generating ancillary classes for service %s", sei);
                        /*
                         * This is a fake build time server start, so it does not matter much that we
                         * use a fake path
                         */
                        final String path = "/QuarkusCxfProcessor/dummy-" + rnd.nextLong();
                        final int oldCnt = capture.getGeneratedClassesCount();
                        CxfDeploymentUtils.createServer(sei, path, bus);
                        LOGGER.infof("Generated %d ancillary classes for service %s",
                                (capture.getGeneratedClassesCount() - oldCnt), sei);
                    });

            clients.stream()
                    .map(ClientSeiBuildItem::getSei)
                    .distinct()
                    .forEach(sei -> {
                        LOGGER.debugf("Generating ancillary classes for client %s", sei);
                        final int oldCnt = capture.getGeneratedClassesCount();
                        CxfDeploymentUtils.createClient(sei, bus);
                        LOGGER.infof("Generated %d ancillary classes for client %s",
                                (capture.getGeneratedClassesCount() - oldCnt), sei);
                    });

            reflectiveClasses.produce(
                    ReflectiveClassBuildItem
                            .builder(capture.getGeneratedClasses())
                            .fields()
                            .build());
        } finally {
            /*
             * The capture is only valid only while the supplied GeneratedBeanBuildItem producer is alive anyway
             * so let's better reset the extension back to the original value
             */
            bus.setExtension(oldCapture, GeneratedClassClassLoaderCapture.class);
        }

    }

    @BuildStep
    List<UberJarMergedResourceBuildItem> uberJarMergedResourceBuildItem() {
        return Arrays.asList(
                new UberJarMergedResourceBuildItem("META-INF/cxf/bus-extensions.txt"),
                new UberJarMergedResourceBuildItem("META-INF/wsdl.plugin.xml"));
    }

    @BuildStep
    void buildResources(BuildProducer<ReflectiveClassBuildItem> reflectiveItems,
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
                                reflectiveItems.produce(ReflectiveClassBuildItem.builder(cols[0]).methods().fields().build());
                            }
                            if (cols[1].length() > 0) {
                                reflectiveItems.produce(ReflectiveClassBuildItem.builder(cols[1]).methods().fields().build());
                            }
                        }
                        out.write(line);
                        out.newLine();
                        line = reader.readLine();
                    }
                }
            }

            // for uber jar merge bus-extensions
            if (packageConfig.jar().type() == JarType.UBER_JAR && os.size() > 0) {
                generatedResources.produce(
                        new GeneratedResourceBuildItem("META-INF/cxf/bus-extensions.txt", os.toByteArray()));
            }
        } catch (IOException e) {
            LOGGER.warn("cannot merge bus-extensions.txt");
        }
    }

    @BuildStep
    void buildXmlResources(
            PackageConfig packageConfig,
            BuildProducer<GeneratedResourceBuildItem> generatedResources) {
        // for uber jar only merge xml resource
        if (packageConfig.jar().type() != JarType.UBER_JAR) {
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
        return new ExtensionSslNativeSupportBuildItem(QuarkusCxfFeature.CXF.getKey());
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
                DataBinding.class,
                Interceptor.class,
                SoapInterceptor.class,
                PhaseInterceptor.class,
                HostnameVerifier.class)
                .map(DotName::createSimple)
                .flatMap(dotName -> index.getAllKnownImplementors(dotName).stream())
                .map(classInfo -> classInfo.name().toString())
                .filter(className -> !className.startsWith("org.apache.cxf.") || !className.contains(".blueprint."))
                .map(className -> ReflectiveClassBuildItem.builder(className).build())
                .forEach(reflectiveClass::produce);

        Stream.of(
                "org.apache.cxf.feature.Feature")
                .map(DotName::createSimple)
                .flatMap(dotName -> index.getAllKnownImplementors(dotName).stream())
                .map(classInfo -> classInfo.name().toString())
                .map(className -> ReflectiveClassBuildItem.builder(className).methods().build())
                .forEach(reflectiveClass::produce);

        for (AnnotationInstance xmlNamespaceInstance : index
                .getAnnotations(DotName.createSimple("com.sun.xml.txw2.annotation.XmlNamespace"))) {
            reflectiveClass.produce(
                    ReflectiveClassBuildItem.builder(xmlNamespaceInstance.target().asClass().name().toString()).methods()
                            .fields().build());
        }

        {
            String[] classes = index.getAnnotations(DotName.createSimple(WebFault.class))
                    .stream()
                    .map(annot -> annot.target().asClass().name().toString())
                    .toArray(String[]::new);
            reflectiveClass.produce(
                    ReflectiveClassBuildItem.builder(classes)
                            .methods()
                            .build());
        }

        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(
                        org.apache.cxf.wsdl.WSDLConstants.class,
                        org.apache.cxf.ws.addressing.JAXWSAConstants.class)
                        .fields()
                        .build());

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "org.apache.cxf.common.logging.Slf4jLogger",
                QuarkusBusFactory.class.getName())
                .build());

        final Set<String> extensibilities = index.getKnownClasses().stream()
                .map(classInfo -> classInfo.name().toString())
                .filter(className -> className.startsWith("io.quarkiverse.cxf.extensibility")
                        && className.endsWith("Extensibility"))
                .collect(Collectors.toSet());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(extensibilities.toArray(new String[0])).build());
        unremovables
                .produce(new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanClassNamesExclusion(extensibilities)));

        /* Referenced from io.quarkiverse.cxf.graal.Target_org_apache_cxf_endpoint_dynamic_ExceptionClassGenerator */
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("io.quarkiverse.cxf.CXFException").build());

        /* Referenced from io.quarkiverse.cxf.graal.Target_org_apache_cxf_common_spi_NamespaceClassGenerator */
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("org.apache.cxf.common.jaxb.NamespaceMapper").build());

        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder("org.apache.cxf.ws.addressing.AddressingProperties").methods().build());

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
        return new NativeImageResourceBuildItem(
                "META-INF/services/" + BusFactory.BUS_FACTORY_PROPERTY_NAME,
                "com/sun/xml/fastinfoset/resources/ResourceBundle.properties",
                "META-INF/cxf/bus-extensions.txt",
                "META-INF/cxf/cxf.xml",
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
    @Record(ExecutionTime.STATIC_INIT)
    void setupRuntimeBusCustomizers(
            CXFRecorder recorder,
            List<RuntimeBusCustomizerBuildItem> customizers) {
        for (RuntimeBusCustomizerBuildItem customizer : customizers) {
            recorder.addRuntimeBusCustomizer(customizer.getCustomizer());
        }
    }

    @BuildStep
    void unremovables(BuildProducer<UnremovableBeanBuildItem> unremovables) {

        /*
         * Mark the beans that can be referenced in application.properties as unremovable.
         * Unfortunately, we cannot be more specific here (so that we'd only register the types
         * really referenced in application.properties) because the *interceptors options
         * are runtime only
         */
        final Set<DotName> unremovableTypes = Set.of(
                DotName.createSimple(Interceptor.class),
                DotName.createSimple(Handler.class),
                DotName.createSimple(Feature.class),
                DotName.createSimple(HostnameVerifier.class));

        unremovables
                .produce(new UnremovableBeanBuildItem(beanInfo -> {
                    return beanInfo.getTypes().stream()
                            .map(Type::name)
                            .anyMatch(unremovableTypes::contains);
                }));

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

        private final Map<String, byte[]> generatedClasses = new LinkedHashMap<>();

        QuarkusCapture(ClassOutput classOutput) {
            this.classOutput = classOutput;
        }

        @Override
        public void capture(String name, byte[] bytes) {
            final String dotName = name.indexOf('.') >= 0 ? name : name.replace('/', '.');
            final String slashName = name.indexOf('/') >= 0 ? name : name.replace('.', '/');
            final byte[] oldVal = generatedClasses.get(dotName);
            if (oldVal != null && !Arrays.equals(oldVal, bytes)) {
                throw new IllegalStateException("Cannot overwrite an existing generated class file " + slashName
                        + " with a different content. Is there perhaps a naming clash among the methods of your service interfaces?");
            } else {
                classOutput.getSourceWriter(slashName);
                LOGGER.debugf("Generated class %s", dotName);
                classOutput.write(slashName, bytes);
                generatedClasses.put(dotName, bytes);
            }
        }

        public int getGeneratedClassesCount() {
            return generatedClasses.size();
        }

        public String[] getGeneratedClasses() {
            return generatedClasses.keySet().toArray(new String[0]);
        }
    }

}
