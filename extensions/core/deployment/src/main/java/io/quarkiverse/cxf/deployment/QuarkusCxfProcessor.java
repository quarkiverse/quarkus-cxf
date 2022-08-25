package io.quarkiverse.cxf.deployment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
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
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.extension.ExtensionManagerImpl;
import org.apache.cxf.common.spi.GeneratedClassClassLoaderCapture;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import io.quarkiverse.cxf.CXFClientData;
import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.CXFRecorder;
import io.quarkiverse.cxf.CXFServletInfos;
import io.quarkiverse.cxf.CxfClientProducer;
import io.quarkiverse.cxf.CxfConfig;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.ReflectiveBeanClassBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.builder.item.BuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.UberJarMergedResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.UberJarRequiredBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.http.deployment.DefaultRouteBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HandlerType;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

class QuarkusCxfProcessor {

    private static final String FEATURE_CXF = "cxf";
    private static final DotName CXFCLIENT_ANNOTATION = DotName.createSimple(CXFClient.class.getName());
    private static final DotName INJECT_INSTANCE = DotName.createSimple(Instance.class.getName());
    private static final DotName WEBSERVICE_ANNOTATION = DotName.createSimple("javax.jws.WebService");
    private static final DotName WEBSERVICE_PROVIDER_ANNOTATION = DotName.createSimple("javax.xml.ws.WebServiceProvider");
    private static final DotName WEBSERVICE_PROVIDER_INTERFACE = DotName.createSimple("javax.xml.ws.Provider");
    private static final DotName WEBSERVICE_CLIENT = DotName.createSimple("javax.xml.ws.WebServiceClient");
    private static final DotName REQUEST_WRAPPER_ANNOTATION = DotName.createSimple("javax.xml.ws.RequestWrapper");
    private static final DotName RESPONSE_WRAPPER_ANNOTATION = DotName.createSimple("javax.xml.ws.ResponseWrapper");
    private static final DotName BINDING_TYPE_ANNOTATION = DotName.createSimple("javax.xml.ws.BindingType");
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

    private String getNameSpaceFromClassInfo(ClassInfo wsClassInfo) {
        String pkg = wsClassInfo.name().toString();
        int idx = pkg.lastIndexOf('.');
        if (idx != -1 && idx < pkg.length() - 1) {
            pkg = pkg.substring(0, idx);
        }
        //TODO XRootElement then XmlSchema then derived of package
        String[] strs = pkg.split("\\.");
        StringBuilder b = new StringBuilder("http://");
        for (int i = strs.length - 1; i >= 0; i--) {
            if (i != strs.length - 1) {
                b.append(".");
            }
            b.append(strs[i]);
        }
        b.append("/");
        return b.toString();
    }

    @BuildStep
    void markBeansAsUnremovable(BuildProducer<UnremovableBeanBuildItem> unremovables) {
        unremovables.produce(new UnremovableBeanBuildItem(beanInfo -> {
            String nameWithPackage = beanInfo.getBeanClass().local();
            return nameWithPackage.contains(".jaxws_asm") || nameWithPackage.endsWith("ObjectFactory");
        }));
        Set<String> extensibilities = new HashSet<>(Arrays.asList(
                "io.quarkiverse.cxf.AddressTypeExtensibility",
                "io.quarkiverse.cxf.UsingAddressingExtensibility",
                "io.quarkiverse.cxf.HTTPClientPolicyExtensibility",
                "io.quarkiverse.cxf.HTTPServerPolicyExtensibility",
                "io.quarkiverse.cxf.XMLBindingMessageFormatExtensibility",
                "io.quarkiverse.cxf.XMLFormatBindingExtensibility"));
        unremovables
                .produce(new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanClassNamesExclusion(extensibilities)));
    }

    class QuarkusCapture implements GeneratedClassClassLoaderCapture {
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

    @BuildStep
    public void buildAdditionalBeans(
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        Stream.of(
                "io.quarkiverse.cxf.annotation.CXFClient")
                .map(AdditionalBeanBuildItem::unremovableOf)
                .forEach(additionalBeans::produce);
    }

    @BuildStep
    public void buildUnremovablesBeans(BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        Stream.of(
                "io.quarkiverse.cxf.annotation.CXFClient")
                .map(UnremovableBeanBuildItem.BeanClassNameExclusion::new)
                .map(UnremovableBeanBuildItem::new)
                .forEach(unremovableBeans::produce);
    }

    @BuildStep
    public void build(
            CombinedIndexBuildItem combinedIndexBuildItem,
            CxfBuildTimeConfig cxfBuildTimeConfig,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveBeanClassBuildItem> reflectiveBeanClass,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxies,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<CxfWebServiceBuildItem> cxfWebServices,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) throws ClassNotFoundException {
        IndexView index = combinedIndexBuildItem.getIndex();

        Bus bus = BusFactory.getDefaultBus();
        // setup class capturing
        bus.setExtension(new QuarkusCapture(new GeneratedBeanGizmoAdaptor(generatedBeans)),
                GeneratedClassClassLoaderCapture.class);

        Set<String> clientSEIsInUse = findClientSEIsInUse(index);

        Collection<AnnotationInstance> webserviceAnnotations = new ArrayList<>(index.getAnnotations(WEBSERVICE_ANNOTATION));
        webserviceAnnotations.addAll(index.getAnnotations(WEBSERVICE_PROVIDER_ANNOTATION));

        for (AnnotationInstance annotation : webserviceAnnotations) {
            if (annotation.target().kind() != AnnotationTarget.Kind.CLASS) {
                continue;
            }

            ClassInfo wsClassInfo = annotation.target().asClass();
            boolean isProvider = wsClassInfo.interfaceNames().contains(WEBSERVICE_PROVIDER_INTERFACE);
            boolean isWebService = wsClassInfo.annotationsMap().containsKey(WEBSERVICE_ANNOTATION);
            boolean isInterface = Modifier.isInterface(wsClassInfo.flags());
            boolean isImplementingAnInterface = !wsClassInfo.interfaceTypes().isEmpty();
            boolean isWebServiceWithoutInterface = isWebService && !isInterface && !isImplementingAnInterface;

            String sei = wsClassInfo.name().toString();

            reflectiveClass
                    .produce(new ReflectiveClassBuildItem(true, true, sei));
            unremovableBeans.produce(new UnremovableBeanBuildItem(
                    new UnremovableBeanBuildItem.BeanClassNameExclusion(sei)));

            if (!isProvider && !isInterface && !isWebServiceWithoutInterface) {
                continue;
            }

            // created on-demand if an implementor or client usage is found
            QuarkusJaxWsServiceFactoryBean factoryBean = null;

            String wsNamespace = Optional.ofNullable(annotation.value("targetNamespace"))
                    .map(AnnotationValue::asString)
                    .orElseGet(() -> getNameSpaceFromClassInfo(wsClassInfo));

            final String soapBindingDefault = SOAPBinding.SOAP11HTTP_BINDING;

            Collection<ClassInfo> implementors = new ArrayList<>(index.getAllKnownImplementors(DotName.createSimple(sei)));

            if (isProvider || isWebServiceWithoutInterface) {
                implementors.add(wsClassInfo);
            }

            if (!implementors.isEmpty()) {
                factoryBean = factoryBean == null ? createQuarkusJaxWsServiceFactoryBean(sei, bus) : factoryBean;

                for (ClassInfo wsClass : implementors) {
                    String impl = wsClass.name().toString();
                    String wsName = Optional.ofNullable(wsClass.classAnnotation(WEBSERVICE_ANNOTATION))
                            .filter(classAnno -> Objects.nonNull(classAnno.value("serviceName")))
                            .map(classAnno -> classAnno.value("serviceName").asString())
                            .orElse(impl.contains(".") ? impl.substring(impl.lastIndexOf('.') + 1) : impl);
                    additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(impl));

                    // Registers ArC generated subclasses for native reflection
                    reflectiveBeanClass.produce(new ReflectiveBeanClassBuildItem(impl));

                    String soapBinding = Optional.ofNullable(wsClass.classAnnotation(BINDING_TYPE_ANNOTATION))
                            .map(bindingType -> bindingType.value().asString())
                            .orElse(soapBindingDefault);

                    cxfWebServices.produce(new CxfWebServiceBuildItem(cxfBuildTimeConfig.path, sei, soapBinding,
                            wsNamespace, wsName, factoryBean.getWrappersClassNames(), impl, isProvider));
                }
            }

            if (clientSEIsInUse.contains(sei)) {
                factoryBean = factoryBean == null ? createQuarkusJaxWsServiceFactoryBean(sei, bus) : factoryBean;

                AnnotationInstance webserviceClient = findWebServiceClientAnnotation(index, wsClassInfo.name());
                String wsName;
                if (webserviceClient != null) {
                    wsName = webserviceClient.value("name").asString();
                    wsNamespace = webserviceClient.value("targetNamespace").asString();
                } else {
                    wsName = Optional.ofNullable(annotation.value("serviceName"))
                            .map(AnnotationValue::asString)
                            .orElse("");
                }
                cxfWebServices.produce(new CxfWebServiceBuildItem(cxfBuildTimeConfig.path, sei, soapBindingDefault, wsNamespace,
                        wsName, factoryBean.getWrappersClassNames()));
                proxies.produce(new NativeImageProxyDefinitionBuildItem(wsClassInfo.name().toString(),
                        "javax.xml.ws.BindingProvider", "java.io.Closeable", "org.apache.cxf.endpoint.Client"));
            }

            if (factoryBean == null) {
                // neither implementation nor client usage found, no use processing the methods
                continue;
            }

            for (MethodInfo mi : wsClassInfo.methods()) {

                AnnotationInstance requestWrapperAnnotation = mi.annotation(REQUEST_WRAPPER_ANNOTATION);
                if (requestWrapperAnnotation != null) {
                    AnnotationValue classNameValue = requestWrapperAnnotation.value("className");
                    String fullClassName = classNameValue.asString();
                    reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, fullClassName));
                }
                AnnotationInstance responseWrapperAnnotation = mi.annotation(RESPONSE_WRAPPER_ANNOTATION);
                if (responseWrapperAnnotation != null) {
                    AnnotationValue classNameValue = responseWrapperAnnotation.value("className");
                    String fullClassName = classNameValue.asString();
                    reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, fullClassName));
                }
            }
        }

    }

    private Set<String> findClientSEIsInUse(IndexView index) {
        return index.getAnnotations(CXFCLIENT_ANNOTATION).stream()
                .map(AnnotationInstance::target)
                .map(target -> {
                    switch (target.kind()) {
                        case FIELD:
                            return target.asField().type();
                        case METHOD_PARAMETER:
                            MethodParameterInfo paramInfo = target.asMethodParameter();
                            return paramInfo.method().parameterTypes().get(paramInfo.position());
                        default:
                            return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(type -> type.name().equals(INJECT_INSTANCE) ? type.asParameterizedType().arguments().get(0) : type)
                .map(type -> type.name().toString())
                .collect(Collectors.toSet());
    }

    private AnnotationInstance findWebServiceClientAnnotation(IndexView index, DotName seiName) {
        Collection<AnnotationInstance> annotations = index.getAnnotations(WEBSERVICE_CLIENT);
        for (AnnotationInstance annotation : annotations) {
            ClassInfo targetClass = annotation.target().asClass();

            for (MethodInfo method : targetClass.methods()) {
                if (method.returnType().name().equals(seiName)) {
                    return annotation;
                }
            }
        }

        return null;
    }

    private QuarkusJaxWsServiceFactoryBean createQuarkusJaxWsServiceFactoryBean(String sei, Bus bus)
            throws ClassNotFoundException {
        QuarkusJaxWsServiceFactoryBean jaxwsFac = new QuarkusJaxWsServiceFactoryBean();
        jaxwsFac.setBus(bus);
        //TODO here add all class
        jaxwsFac.setServiceClass(Thread.currentThread().getContextClassLoader().loadClass(sei));
        jaxwsFac.create();
        return jaxwsFac;
    }

    /**
     * Build step to generate Producer beans suitable for injecting @CFXClient
     */
    @BuildStep
    void clientProducerBuildStep(
            List<CxfWebServiceBuildItem> cxfItems,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        cxfItems
                .stream()
                .filter(CxfWebServiceBuildItem::isClient)
                .map(CxfWebServiceBuildItem::getSei)
                .forEach(sei -> {
                    generateCxfClientProducer(sei, generatedBeans, unremovableBeans);
                });
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void startRoute(CXFRecorder recorder,
            BuildProducer<DefaultRouteBuildItem> defaultRoutes,
            BuildProducer<RouteBuildItem> routes,
            BeanContainerBuildItem beanContainer,
            List<CxfWebServiceBuildItem> cxfWebServices,
            HttpBuildTimeConfig httpBuildTimeConfig,
            HttpConfiguration httpConfiguration,
            CxfConfig cxfConfig) {
        String path = null;
        boolean startRoute = false;
        if (!cxfWebServices.isEmpty()) {
            RuntimeValue<CXFServletInfos> infos = recorder.createInfos();
            for (CxfWebServiceBuildItem cxfWebService : cxfWebServices) {
                if (cxfWebService.isClient()) {
                    continue;
                }
                recorder.registerCXFServlet(infos, cxfWebService.getPath(), cxfWebService.getSei(),
                        cxfConfig, cxfWebService.getWsName(), cxfWebService.getWsNamespace(),
                        cxfWebService.getSoapBinding(), cxfWebService.getClassNames(),
                        cxfWebService.getImplementor(), cxfWebService.isProvider());
                if (cxfWebService.getImplementor() != null && !cxfWebService.getImplementor().isEmpty()) {
                    startRoute = true;
                }
                if (path == null) {
                    path = cxfWebService.getPath();
                    recorder.setPath(infos, path, httpBuildTimeConfig.rootPath);
                }
            }
            if (startRoute) {
                Handler<RoutingContext> handler = recorder.initServer(infos, beanContainer.getValue(),
                        httpConfiguration);
                if (path != null) {
                    routes.produce(RouteBuildItem.builder()
                            .route(getMappingPath(path))
                            .handler(handler)
                            .handlerType(HandlerType.BLOCKING)
                            .build());

                }
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void startClient(CXFRecorder recorder, CxfConfig cxfConfig, List<CxfWebServiceBuildItem> cxfWebServices,
            BuildProducer<SyntheticBeanBuildItem> synthetics) {

        //
        // Create injectable CXFClientInfo bean for each SEI-only interface, i.e. for each
        // class annotated as @WebService and without implementation. This bean fuells a
        // producer bean producing CXF proxy clients.
        cxfWebServices
                .stream()
                .filter(CxfWebServiceBuildItem::isClient)
                .map(QuarkusCxfProcessor::clientData)
                .map(cxf -> {
                    LOGGER.debugf("producing dedicated CXFClientInfo bean named '%s' for SEI %s", cxf.getSei(), cxf.getSei());
                    return SyntheticBeanBuildItem
                            .configure(CXFClientInfo.class)
                            .named(cxf.getSei())
                            .runtimeValue(recorder.cxfClientInfoSupplier(cxf))
                            .unremovable()
                            .setRuntimeInit()
                            .done();
                }).forEach(synthetics::produce);
    }

    @BuildStep
    BeanDefiningAnnotationBuildItem additionalBeanDefiningAnnotation() {
        return new BeanDefiningAnnotationBuildItem(WEBSERVICE_ANNOTATION);
    }

    @BuildStep
    List<UberJarMergedResourceBuildItem> uberJarMergedResourceBuildItem() {
        return Arrays.asList(
                new UberJarMergedResourceBuildItem("META-INF/cxf/bus-extensions.txt"),
                new UberJarMergedResourceBuildItem("META-INF/wsdl.plugin.xml"));
    }

    @BuildStep
    void buildResources(BuildProducer<NativeImageResourceBuildItem> resources,
            BuildProducer<ReflectiveClassBuildItem> reflectiveItems,
            List<UberJarRequiredBuildItem> uberJarRequired,
            PackageConfig packageConfig,
            BuildProducer<GeneratedResourceBuildItem> generatedResources) {

        try (ByteArrayOutputStream os = new ByteArrayOutputStream();
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os))) {
            Enumeration<URL> urls = ExtensionManagerImpl.class.getClassLoader().getResources("META-INF/cxf/bus-extensions.txt");
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try (InputStream openStream = url.openStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(openStream))) {
                    //todo set directly extension and avoid load of file at runtime
                    //List<Extension> exts = new TextExtensionFragmentParser(loader).getExtensions(is);
                    //factory.getBus().setExtension();
                    String line = reader.readLine();
                    while (line != null) {
                        String[] cols = line.split(":");
                        //org.apache.cxf.bus.managers.PhaseManagerImpl:org.apache.cxf.phase.PhaseManager:true
                        if (cols.length > 1) {
                            if (!"".equals(cols[0])) {
                                reflectiveItems.produce(new ReflectiveClassBuildItem(true, true, cols[0]));
                            }
                            if (!"".equals(cols[1])) {
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
            LOGGER.warn("can not merge bus-extensions.txt");
        }
    }

    @BuildStep
    void buildXmlResources(BuildProducer<NativeImageResourceBuildItem> resources,
            List<UberJarRequiredBuildItem> uberJarRequired,
            PackageConfig packageConfig,
            BuildProducer<GeneratedResourceBuildItem> generatedResources) {
        // for uber jar only merge xml resource
        if (uberJarRequired.isEmpty() && !packageConfig.type.equalsIgnoreCase(PackageConfig.UBER_JAR)) {
            return;
        }
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            XPath xpath = XPathFactory.newInstance().newXPath();
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();

            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> urls = loader.getResources("META-INF/wsdl.plugin.xml");

            // Create output / merged document
            Document mergedXmlDocument = builder.newDocument();
            Element root = mergedXmlDocument.createElement("properties");
            mergedXmlDocument.appendChild(root);
            for (URL url : Collections.list(urls)) {
                try (InputStreamReader inputStreamReader = new InputStreamReader(url.openStream())) {
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
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(mergedXmlDocument),
                    new StreamResult(new OutputStreamWriter(os, "UTF-8")));

            if (os.size() > 0) {
                generatedResources.produce(
                        new GeneratedResourceBuildItem("META-INF/wsdl.plugin.xml", os.toByteArray()));
            }

        } catch (XPathExpressionException
                | ParserConfigurationException
                | IOException
                | SAXException
                | TransformerException e) {
            LOGGER.warn("can not merge wsdl.plugin.xml");
        }
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem ssl() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE_CXF);
    }

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
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
                "org.apache.cxf.configuration.blueprint.AbstractBPBeanDefinitionParser",
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
    }

    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        Stream.of(
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
    void reflectiveClasses(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
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

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false,
                "org.apache.cxf.catalog.OASISCatalogManager",
                "org.apache.cxf.common.util.ASMHelperImpl$2",
                "org.apache.cxf.common.util.OpcodesProxy",
                "org.apache.cxf.headers.HeaderManager",
                "org.apache.cxf.policy.PolicyDataEngine"));

    }

    void produceRecursiveProxies(IndexView index,
            DotName interfaceDN,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxies, Set<String> proxiesCreated) {
        index.getKnownDirectImplementors(interfaceDN).stream()
                .filter(classinfo -> Modifier.isInterface(classinfo.flags()))
                .map(ClassInfo::name)
                .forEach((className) -> {
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
    public void registerReflectionItems(BuildProducer<ReflectiveClassBuildItem> reflectiveItems) {
        //TODO load all bus-extensions.txt file and parse it to generate the reflective class.
        //TODO load all handler from https://github.com/apache/cxf/tree/master/rt/frontend/jaxws/src/main/java/org/apache/cxf/jaxws/handler/types
        reflectiveItems.produce(new ReflectiveClassBuildItem(true, false, "org.apache.cxf.common.jaxb.NamespaceMapper"));

        reflectiveItems.produce(new ReflectiveClassBuildItem(true, true,
                "io.quarkiverse.cxf.AddressTypeExtensibility",
                "io.quarkiverse.cxf.CXFException",
                "io.quarkiverse.cxf.CxfInfoProducer",
                "io.quarkiverse.cxf.HTTPClientPolicyExtensibility",
                "io.quarkiverse.cxf.HTTPServerPolicyExtensibility",
                "io.quarkiverse.cxf.UsingAddressingExtensibility",
                "io.quarkiverse.cxf.XMLBindingMessageFormatExtensibility",
                "io.quarkiverse.cxf.XMLFormatBindingExtensibility",
                "org.apache.cxf.binding.corba.utils.CorbaFixedAnyImplClassCreator",
                "org.apache.cxf.binding.corba.utils.CorbaFixedAnyImplClassCreatorProxyService",
                "org.apache.cxf.binding.corba.utils.CorbaFixedAnyImplClassLoader",
                "org.apache.cxf.binding.corba.utils.CorbaFixedAnyImplGenerator",
                "org.apache.cxf.bindings.xformat.ObjectFactory",
                "org.apache.cxf.common.jaxb.JAXBBeanInfo",
                "org.apache.cxf.common.jaxb.JAXBUtils$JCodeModel",
                "org.apache.cxf.common.jaxb.JAXBUtils$JDefinedClass",
                "org.apache.cxf.common.jaxb.JAXBUtils$JPackage",
                "org.apache.cxf.common.jaxb.JAXBUtils$JType",
                "org.apache.cxf.common.jaxb.JAXBUtils$Mapping",
                "org.apache.cxf.common.jaxb.JAXBUtils$Options",
                "org.apache.cxf.common.jaxb.JAXBUtils$S2JJAXBModel",
                "org.apache.cxf.common.jaxb.JAXBUtils$TypeAndAnnotation",
                "org.apache.cxf.common.jaxb.SchemaCollectionContextProxy",
                "org.apache.cxf.common.logging.Slf4jLogger",
                "org.apache.cxf.common.spi.ClassGeneratorClassLoader$TypeHelperClassLoader",
                "org.apache.cxf.common.spi.ClassLoaderProxyService",
                "org.apache.cxf.common.spi.ClassLoaderService",
                "org.apache.cxf.common.spi.GeneratedClassClassLoaderCapture",
                "org.apache.cxf.common.spi.GeneratedNamespaceClassLoader",
                "org.apache.cxf.common.spi.NamespaceClassCreator",
                "org.apache.cxf.common.spi.NamespaceClassGenerator",
                "org.apache.cxf.common.util.ASMHelper",
                "org.apache.cxf.common.util.ASMHelperImpl",
                "org.apache.cxf.common.util.ReflectionInvokationHandler",
                "org.apache.cxf.common.util.ReflectionInvokationHandler",
                "org.apache.cxf.endpoint.dynamic.ExceptionClassCreator",
                "org.apache.cxf.endpoint.dynamic.ExceptionClassCreatorProxyService",
                "org.apache.cxf.endpoint.dynamic.ExceptionClassGenerator",
                "org.apache.cxf.endpoint.dynamic.ExceptionClassLoader",
                "org.apache.cxf.jaxb.FactoryClassCreator",
                "org.apache.cxf.jaxb.FactoryClassGenerator",
                "org.apache.cxf.jaxb.FactoryClassLoader",
                "org.apache.cxf.jaxb.FactoryClassProxyService",
                "org.apache.cxf.jaxb.WrapperHelperClassGenerator",
                "org.apache.cxf.jaxb.WrapperHelperClassLoader",
                "org.apache.cxf.jaxb.WrapperHelperCreator",
                "org.apache.cxf.jaxb.WrapperHelperProxyService",
                "org.apache.cxf.jaxws.spi.WrapperClassCreator",
                "org.apache.cxf.jaxws.spi.WrapperClassCreatorProxyService",
                "org.apache.cxf.jaxws.spi.WrapperClassGenerator",
                "org.apache.cxf.jaxws.spi.WrapperClassLoader",
                "org.apache.cxf.transports.http.configuration.ObjectFactory",
                "org.apache.cxf.ws.addressing.WSAddressingFeature",
                "org.apache.cxf.ws.addressing.impl.AddressingWSDLExtensionLoader",
                "org.apache.cxf.ws.addressing.wsdl.ObjectFactory",
                "org.apache.cxf.wsdl.ExtensionClassCreator",
                "org.apache.cxf.wsdl.ExtensionClassCreatorProxyService",
                "org.apache.cxf.wsdl.ExtensionClassGenerator",
                "org.apache.cxf.wsdl.ExtensionClassLoader",
                "org.apache.cxf.wsdl.http.ObjectFactory"));
        reflectiveItems.produce(new ReflectiveClassBuildItem(
                false,
                false,
                //manually added
                "org.apache.cxf.binding.soap.SoapBinding",
                "org.apache.cxf.binding.soap.SoapFault",
                "org.apache.cxf.binding.soap.SoapHeader",
                "org.apache.cxf.binding.soap.SoapMessage",
                "org.apache.cxf.binding.soap.interceptor.CheckFaultInterceptor",
                "org.apache.cxf.binding.soap.interceptor.EndpointSelectionInterceptor",
                "org.apache.cxf.binding.soap.interceptor.MustUnderstandInterceptor",
                "org.apache.cxf.binding.soap.interceptor.RPCInInterceptor",
                "org.apache.cxf.binding.soap.interceptor.RPCOutInterceptor",
                "org.apache.cxf.binding.soap.interceptor.ReadHeadersInterceptor",
                "org.apache.cxf.binding.soap.interceptor.Soap11FaultInInterceptor",
                "org.apache.cxf.binding.soap.interceptor.Soap11FaultOutInterceptor",
                "org.apache.cxf.binding.soap.interceptor.Soap12FaultInInterceptor",
                "org.apache.cxf.binding.soap.interceptor.Soap12FaultOutInterceptor",
                "org.apache.cxf.binding.soap.interceptor.SoapActionInInterceptor",
                "org.apache.cxf.binding.soap.interceptor.SoapHeaderInterceptor",
                "org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor",
                "org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor$SoapOutEndingInterceptor",
                "org.apache.cxf.binding.soap.interceptor.StartBodyInterceptor",
                "org.apache.cxf.binding.soap.model.SoapHeaderInfo",
                "org.apache.cxf.binding.soap.model.SoapOperationInfo",
                "org.apache.cxf.binding.soap.wsdl.extensions.SoapBody",
                "org.apache.cxf.binding.xml.XMLFault",
                "org.apache.cxf.bindings.xformat.XMLBindingMessageFormat",
                "org.apache.cxf.bindings.xformat.XMLFormatBinding",
                "org.apache.cxf.bus.CXFBusFactory",
                "org.apache.cxf.bus.managers.BindingFactoryManagerImpl",
                "org.apache.cxf.common.jaxb.JAXBContextCache",
                "org.apache.cxf.interceptor.ClientFaultConverter",
                "org.apache.cxf.interceptor.Fault",
                "org.apache.cxf.interceptor.OneWayProcessorInterceptor",
                "org.apache.cxf.jaxb.DatatypeFactory",
                "org.apache.cxf.jaxb.JAXBDataBinding",
                "org.apache.cxf.jaxrs.utils.JAXRSUtils",
                "org.apache.cxf.jaxws.binding.soap.SOAPBindingImpl",
                "org.apache.cxf.jaxws.spi.ProviderImpl",
                "org.apache.cxf.message.Exchange",
                "org.apache.cxf.message.ExchangeImpl",
                "org.apache.cxf.message.StringMap",
                "org.apache.cxf.message.StringMapImpl",
                "org.apache.cxf.metrics.codahale.CodahaleMetricsProvider",
                "org.apache.cxf.service.model.BindingOperationInfo",
                "org.apache.cxf.service.model.MessageInfo",
                "org.apache.cxf.staxutils.W3CDOMStreamWriter",
                "org.apache.cxf.tools.fortest.cxf523.DBServiceFault",
                "org.apache.cxf.tools.fortest.cxf523.Database",
                "org.apache.cxf.tools.fortest.withannotation.doc.HelloWrapped",
                "org.apache.cxf.transports.http.configuration.HTTPClientPolicy",
                "org.apache.cxf.transports.http.configuration.HTTPServerPolicy",
                "org.apache.cxf.ws.addressing.wsdl.AttributedQNameType",
                "org.apache.cxf.ws.addressing.wsdl.ServiceNameType",
                "org.apache.cxf.wsdl.http.AddressType",
                "org.apache.cxf.wsdl.interceptors.BareInInterceptor",
                "org.apache.cxf.wsdl.interceptors.DocLiteralInInterceptor"));
    }

    @BuildStep
    NativeImageResourceBundleBuildItem nativeImageResourceBundleBuildItem() {
        return new NativeImageResourceBundleBuildItem("org.apache.cxf.interceptor.Messages");
    }

    @BuildStep
    NativeImageResourceBuildItem nativeImageResourceBuildItem() {
        //TODO add @HandlerChain (file) and parse it to add class loading
        return new NativeImageResourceBuildItem("com/sun/xml/fastinfoset/resources/ResourceBundle.properties",
                "META-INF/cxf/bus-extensions.txt",
                "META-INF/cxf/cxf.xml",
                "META-INF/cxf/org.apache.cxf.bus.factory",
                "META-INF/services/org.apache.cxf.bus.factory",
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

    private String getMappingPath(String path) {
        String mappingPath;
        if (path.endsWith("/")) {
            mappingPath = path + "*";
        } else {
            mappingPath = path + "/*";
        }
        return mappingPath;
    }

    private void produceUnremovableBean(
            BuildProducer<UnremovableBeanBuildItem> unremovables,
            String... args) {
        Arrays.stream(args)
                .map(UnremovableBeanBuildItem.BeanClassNameExclusion::new)
                .map(UnremovableBeanBuildItem::new)
                .forEach(unremovables::produce);
    }

    private void generateCxfClientProducer(
            String sei,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        // For a given SEI we create a dedicated client producer class, i.e.
        //
        // >> @ApplicationScoped
        // >> [public] {SEI}CxfClientProducer implements CxfClientProducer {
        // >>   @Inject
        // >>   @Named(value="{SEI}")
        // >>   public CXFClientInfo info;
        // >>
        // >>   @Produces
        // >>   @CXFClient
        // >>   {SEI} createService(InjectionPoint ip) {
        // >>     return ({SEI}) super().loadCxfClient(ip, this.info);
        // >>   }
        // >>
        // >>   @Produces
        // >>   @CXFClient
        // >>   CXFClientInfo createInfo(InjectionPoint ip) {
        // >>     return ({SEI}) super().loadCxfClientInfo(ip, this.info);
        // >>   }
        // >> }
        String cxfClientProducerClassName = sei + "CxfClientProducer";

        ClassOutput classoutput = new GeneratedBeanGizmoAdaptor(generatedBeans);

        try (ClassCreator classCreator = ClassCreator.builder()
                .classOutput(classoutput)
                .className(cxfClientProducerClassName)
                .superClass(CxfClientProducer.class)
                .build()) {

            FieldCreator info;

            classCreator.addAnnotation(ApplicationScoped.class);

            // generates:
            // >> public CXFClientInfo info;

            info = classCreator
                    .getFieldCreator("info", "io.quarkiverse.cxf.CXFClientInfo")
                    .setModifiers(Modifier.PUBLIC);

            // add @Named to info, i.e.
            // >> @Named(value="{SEI}")
            // >> public CXFClientInfo info;

            info.addAnnotation(
                    AnnotationInstance.create(DotNames.NAMED, null, new AnnotationValue[] {
                            AnnotationValue.createStringValue("value", sei)
                    }));

            // add @Inject annotation to info, i.e.
            // >> @Inject
            // >> @Named(value="{SEI}")
            // >> public CXFClientInfo info;
            info.addAnnotation(
                    AnnotationInstance
                            .create(DotName.createSimple(Inject.class.getName()), null, new AnnotationValue[] {}));

            // create method
            // >> @Produces
            // >> @CXFClient
            // >> {SEI} createService(InjectionPoint ip) { .. }

            //String p0class = InjectionPoint.class.getName();
            //String p1class = CXFClientInfo.class.getName();
            try (MethodCreator createService = classCreator.getMethodCreator("createService", sei, InjectionPoint.class)) {
                createService.addAnnotation(Produces.class);
                createService.addAnnotation(CXFClient.class);

                // p0 (InjectionPoint);
                ResultHandle p0, p1, p2;
                ResultHandle cxfClient;

                p0 = createService.getThis();
                p1 = createService.getMethodParam(0);
                p2 = createService.readInstanceField(info.getFieldDescriptor(), p0);

                MethodDescriptor loadCxfClient = MethodDescriptor.ofMethod(
                        CxfClientProducer.class,
                        "loadCxfClient",
                        "java.lang.Object",
                        InjectionPoint.class,
                        CXFClientInfo.class);
                // >> .. {
                // >>       Object cxfClient = this.loadCxfClient(ip, this.info);
                // >>       return ({SEI})cxfClient;
                // >>    }

                cxfClient = createService.invokeVirtualMethod(loadCxfClient, p0, p1, p2);
                createService.returnValue(createService.checkCast(cxfClient, sei));
            }

            //            try (MethodCreator createInfo = classCreator.getMethodCreator(
            //                    "createInfo",
            //                    "io.quarkiverse.cxf.CXFClientInfo",
            //                    p0class)) {
            //                createInfo.addAnnotation(Produces.class);
            //                createInfo.addAnnotation(CXFClient.class);
            //
            //                // p0 (InjectionPoint);
            //                ResultHandle p0;
            //                ResultHandle p1;
            //                ResultHandle cxfClient;
            //
            //                p0 = createInfo.getMethodParam(0);
            //
            //                MethodDescriptor loadCxfInfo = MethodDescriptor.ofMethod(
            //                        CxfClientProducer.class,
            //                        "loadCxfClientInfo",
            //                        "java.lang.Object",
            //                        p0class,
            //                        p1class);
            //                // >> .. {
            //                // >>       Object cxfInfo = this().loadCxfInfo(ip, this.info);
            //                // >>       return (CXFClientInfo)cxfInfo;
            //                // >>    }
            //
            //                p1 = createInfo.readInstanceField(info.getFieldDescriptor(), createInfo.getThis());
            //                cxfClient = createInfo.invokeVirtualMethod(loadCxfInfo, createInfo.getThis(), p0, p1);
            //                createInfo.returnValue(createInfo.checkCast(cxfClient, "io.quarkiverse.cxf
            //                .CXFClientInfo"));
            //            }

        }

        // Eventually let's produce
        produceUnremovableBean(unremovableBeans, cxfClientProducerClassName);
    }

    @SafeVarargs
    private static <T> Set<T> asSet(T... items) {
        return Arrays.stream(items).collect(Collectors.toSet());
    }

    @SafeVarargs
    private static <T extends BuildItem> void produce(
            BuildProducer<T> p,
            T... beans) {
        Arrays.stream(beans).forEach(p::produce);
    }

    private static <T extends BuildItem> void produce(
            BuildProducer<T> p,
            Collection<T> beans) {
        beans.forEach(p::produce);
    }

    private static CXFClientData clientData(CxfWebServiceBuildItem cxfWebServiceBuildItem) {
        return new CXFClientData(
                cxfWebServiceBuildItem.getSoapBinding(),
                cxfWebServiceBuildItem.getSei(),
                cxfWebServiceBuildItem.getWsName(),
                cxfWebServiceBuildItem.getWsNamespace(),
                cxfWebServiceBuildItem.getClassNames());
    }
}
