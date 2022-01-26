package io.quarkiverse.cxf.deployment;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import org.jboss.jandex.Type;
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
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
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
    private static final DotName WEBSERVICE_CLIENT = DotName.createSimple("javax.xml.ws.WebServiceClient");
    private static final DotName REQUEST_WRAPPER_ANNOTATION = DotName.createSimple("javax.xml.ws.RequestWrapper");
    private static final DotName RESPONSE_WRAPPER_ANNOTATION = DotName.createSimple("javax.xml.ws.ResponseWrapper");
    private static final DotName ABSTRACT_FEATURE = DotName.createSimple("org.apache.cxf.feature.AbstractFeature");
    private static final DotName ABSTRACT_INTERCEPTOR = DotName.createSimple("org.apache.cxf.phase.AbstractPhaseInterceptor");
    private static final DotName DATABINDING = DotName.createSimple("org.apache.cxf.databinding");
    private static final DotName BINDING_TYPE_ANNOTATION = DotName.createSimple("javax.xml.ws.BindingType");
    private static final DotName XML_NAMESPACE = DotName.createSimple("com.sun.xml.txw2.annotation.XmlNamespace");
    private static final DotName XML_SEE_ALSO = DotName.createSimple("javax.xml.bind.annotation.XmlSeeAlso");
    private static final Logger LOGGER = Logger.getLogger(QuarkusCxfProcessor.class);

    @BuildStep
    public void generateSysProps(BuildProducer<SystemPropertyBuildItem> props) {
        props.produce(new SystemPropertyBuildItem("com.sun.xml.bind.v2.bytecode.ClassTailor.noOptimize", "true"));
    }

    @BuildStep
    public void generateWSDL(BuildProducer<NativeImageResourceBuildItem> ressources,
            CxfBuildTimeConfig cxfBuildTimeConfig) {
        if (cxfBuildTimeConfig.wsdlPath.isPresent()) {
            for (String wsdlPath : cxfBuildTimeConfig.wsdlPath.get()) {
                ressources.produce(new NativeImageResourceBuildItem(wsdlPath));
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
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxies,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<CxfWebServiceBuildItem> cxfWebServices,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) throws ClassNotFoundException {
        IndexView index = combinedIndexBuildItem.getIndex();
        // Register package-infos for reflection
        for (AnnotationInstance xmlNamespaceInstance : index.getAnnotations(XML_NAMESPACE)) {
            reflectiveClass.produce(
                    new ReflectiveClassBuildItem(true, true, xmlNamespaceInstance.target().asClass().name().toString()));
        }

        Bus bus = BusFactory.getDefaultBus();
        // setup class capturing
        bus.setExtension(new QuarkusCapture(new GeneratedBeanGizmoAdaptor(generatedBeans)),
                GeneratedClassClassLoaderCapture.class);

        Set<String> clientSEIsInUse = findClientSEIsInUse(index);

        for (AnnotationInstance annotation : index.getAnnotations(WEBSERVICE_ANNOTATION)) {
            if (annotation.target().kind() != AnnotationTarget.Kind.CLASS) {
                continue;
            }

            ClassInfo wsClassInfo = annotation.target().asClass();

            String sei = wsClassInfo.name().toString();

            reflectiveClass
                    .produce(new ReflectiveClassBuildItem(true, true, sei));
            unremovableBeans.produce(new UnremovableBeanBuildItem(
                    new UnremovableBeanBuildItem.BeanClassNameExclusion(sei)));

            if (!Modifier.isInterface(wsClassInfo.flags())) {
                continue;
            }

            // created on-demand if an implementor or client usage is found
            QuarkusJaxWsServiceFactoryBean factoryBean = null;

            String wsNamespace = Optional.ofNullable(annotation.value("targetNamespace"))
                    .map(AnnotationValue::asString)
                    .orElseGet(() -> getNameSpaceFromClassInfo(wsClassInfo));

            final String soapBindingDefault = SOAPBinding.SOAP11HTTP_BINDING;

            Collection<ClassInfo> implementors = index.getAllKnownImplementors(DotName.createSimple(sei));

            if (implementors != null && !implementors.isEmpty()) {
                factoryBean = factoryBean == null ? createQuarkusJaxWsServiceFactoryBean(sei, bus) : factoryBean;

                for (ClassInfo wsClass : implementors) {
                    String impl = wsClass.name().toString();
                    String wsName = impl.contains(".") ? impl.substring(impl.lastIndexOf('.') + 1) : impl;
                    additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(impl));

                    /*
                     * Address issue #346; though this really should be handled in ArC
                     * https://github.com/quarkiverse/quarkus-cxf/issues/346
                     * https://github.com/quarkusio/quarkus/issues/7507
                     */
                    reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, impl + "_Subclass"));

                    String soapBinding = Optional.ofNullable(wsClass.classAnnotation(BINDING_TYPE_ANNOTATION))
                            .map(bindingType -> bindingType.value().asString())
                            .orElse(soapBindingDefault);
                    cxfWebServices.produce(new CxfWebServiceBuildItem(cxfBuildTimeConfig.path, sei, soapBinding,
                            wsNamespace, wsName, factoryBean.getWrappersClassNames(), impl));
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

        feature.produce(new FeatureBuildItem(FEATURE_CXF));

        for (ClassInfo subclass : index.getAllKnownSubclasses(ABSTRACT_FEATURE)) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, subclass.name().toString()));
        }
        for (ClassInfo subclass : index.getAllKnownSubclasses(ABSTRACT_INTERCEPTOR)) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, subclass.name().toString()));
        }
        for (ClassInfo subclass : index.getAllKnownImplementors(DATABINDING)) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, subclass.name().toString()));
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
                            return paramInfo.method().parameters().get(paramInfo.position());
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
                .filter(CxfWebServiceBuildItem::IsClient)
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
                if (cxfWebService.IsClient()) {
                    continue;
                }
                recorder.registerCXFServlet(infos, cxfWebService.getPath(), cxfWebService.getSei(),
                        cxfConfig, cxfWebService.getSoapBinding(), cxfWebService.getClassNames(),
                        cxfWebService.getImplementor());
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
                .filter(CxfWebServiceBuildItem::IsClient)
                .map(QuarkusCxfProcessor::clientData)
                .map(cxf -> {
                    String fmt = "producing dedicated CXFClientInfo bean named '%s' for SEI %s";
                    String msg = format(fmt, cxf.getSei(), cxf.getSei());
                    LOGGER.info(msg);
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

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os));
        try {
            Enumeration<URL> urls = ExtensionManagerImpl.class.getClassLoader().getResources("META-INF/cxf/bus-extensions.txt");
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try (InputStream openStream = url.openStream()) {
                    //todo set directly extension and avoid load of file at runtime
                    //List<Extension> exts = new TextExtensionFragmentParser(loader).getExtensions(is);
                    //factory.getBus().setExtension();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(openStream));
                    String line = reader.readLine();
                    out.write(line);
                    out.newLine();
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
                        line = reader.readLine();
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warn("can not merge bus-extensions.txt");
        }
        // for uber jar merge bus-extensions
        if ((!uberJarRequired.isEmpty() || packageConfig.type.equalsIgnoreCase(PackageConfig.UBER_JAR))
                && (os.size() > 0)) {
            generatedResources.produce(
                    new GeneratedResourceBuildItem("META-INF/cxf/bus-extensions.txt", os.toByteArray()));
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
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
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
                Document dDoc = builder.parse(new InputSource(new InputStreamReader(url.openStream())));
                NodeList nodeList = (NodeList) xpath.compile("//entry").evaluate(dDoc, XPathConstants.NODESET);
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);
                    Node copyNode = mergedXmlDocument.importNode(node, true);
                    root.appendChild(copyNode);
                }
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(mergedXmlDocument),
                    new StreamResult(new OutputStreamWriter(os, "UTF-8")));
        } catch (XPathExpressionException
                | ParserConfigurationException
                | IOException
                | SAXException
                | TransformerException e) {
            LOGGER.warn("can not merge wsdl.plugin.xml");
        }
        if (os.size() > 0) {
            generatedResources.produce(
                    new GeneratedResourceBuildItem("META-INF/wsdl.plugin.xml", os.toByteArray()));
        }
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem ssl() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE_CXF);
    }

    @BuildStep
    List<RuntimeInitializedClassBuildItem> runtimeInitializedClasses() {
        return Arrays.asList(
                new RuntimeInitializedClassBuildItem("io.netty.buffer.PooledByteBufAllocator"),
                new RuntimeInitializedClassBuildItem("io.netty.buffer.UnpooledHeapByteBuf"),
                new RuntimeInitializedClassBuildItem("io.netty.buffer.UnpooledUnsafeHeapByteBuf"),
                new RuntimeInitializedClassBuildItem(
                        "io.netty.buffer.UnpooledByteBufAllocator$InstrumentedUnpooledUnsafeHeapByteBuf"),
                new RuntimeInitializedClassBuildItem("io.netty.buffer.AbstractReferenceCountedByteBuf"),
                new RuntimeInitializedClassBuildItem("org.apache.cxf.staxutils.validation.W3CMultiSchemaFactory"),
                new RuntimeInitializedClassBuildItem("org.apache.cxf.endpoint.ClientImpl"),
                new RuntimeInitializedClassBuildItem("org.apache.cxf.phase.PhaseInterceptorChain"),
                new RuntimeInitializedClassBuildItem("org.apache.cxf.interceptor.AttachmentOutInterceptor"),
                new RuntimeInitializedClassBuildItem("org.apache.cxf.attachment.AttachmentSerializer"),
                new RuntimeInitializedClassBuildItem("org.apache.cxf.attachment.AttachmentUtil"),
                new RuntimeInitializedClassBuildItem("org.apache.cxf.attachment.ImageDataContentHandler"),
                new RuntimeInitializedClassBuildItem("org.apache.cxf.service.factory.AbstractServiceFactoryBean"),
                new RuntimeInitializedClassBuildItem("org.apache.cxf.interceptor.OneWayProcessorInterceptor"),
                new RuntimeInitializedClassBuildItem("org.apache.cxf.interceptor.OneWayProcessorInterceptor$1"),
                new RuntimeInitializedClassBuildItem("org.apache.cxf.ws.addressing.impl.InternalContextUtils"),
                new RuntimeInitializedClassBuildItem("org.apache.cxf.ws.addressing.impl.InternalContextUtils$1"),
                new RuntimeInitializedClassBuildItem("org.apache.cxf.transport.http.HTTPConduit"),
                new RuntimeInitializedClassBuildItem("org.apache.cxf.transport.http.HTTPConduit$WrappedOutputStream"),
                new RuntimeInitializedClassBuildItem("org.apache.cxf.transport.http.HTTPConduit$WrappedOutputStream$1"),
                new RuntimeInitializedClassBuildItem("com.sun.xml.bind.v2.runtime.output.FastInfosetStreamWriterOutput"));
    }

    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("org.glassfish.jaxb", "txw2"));
        indexDependency.produce(new IndexDependencyBuildItem("org.glassfish.jaxb", "jaxb-runtime"));
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
    void seeAlso(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveItems) {
        IndexView index = combinedIndexBuildItem.getIndex();
        for (AnnotationInstance xmlSeeAlsoAnn : index.getAnnotations(XML_SEE_ALSO)) {
            AnnotationValue value = xmlSeeAlsoAnn.value();
            Type[] types = value.asClassArray();
            for (Type t : types) {
                reflectiveItems.produce(new ReflectiveClassBuildItem(false, false, t.name().toString()));
            }
        }
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
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPOperation"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPBody"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPHeader"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPAddress"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPBinding"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPFault"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPHeaderFault"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.binding.soap.wsdl.extensions.SoapBinding"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.binding.soap.wsdl.extensions.SoapAddress"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.binding.soap.wsdl.extensions.SoapHeader"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.binding.soap.wsdl.extensions.SoapBody"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.binding.soap.wsdl.extensions.SoapFault"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.binding.soap.wsdl.extensions.SoapOperation"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.binding.soap.wsdl.extensions.SoapHeaderFault"));
        produceProxyIfExist(proxies, "com.sun.xml.bind.marshaller.CharacterEscapeHandler");
        produceProxyIfExist(proxies, "com.sun.xml.internal.bind.marshaller.CharacterEscapeHandler");
        produceProxyIfExist(proxies, "org.glassfish.jaxb.core.marshaller.CharacterEscapeHandler");
        produceProxyIfExist(proxies, "com.sun.xml.txw2.output.CharacterEscapeHandler");
        produceProxyIfExist(proxies, "org.glassfish.jaxb.characterEscapeHandler");
        produceProxyIfExist(proxies, "org.glassfish.jaxb.marshaller.CharacterEscapeHandler");
        //proxies.produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.model.impl.PropertySeed"));
        //proxies.produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.model.core.TypeInfo"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.jaxb.JAXBUtils$S2JJAXBModel"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.jaxb.JAXBUtils$Options"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.jaxb.JAXBUtils$JCodeModel"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.jaxb.JAXBUtils$Mapping"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.jaxb.JAXBUtils$TypeAndAnnotation"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.jaxb.JAXBUtils$JType"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.jaxb.JAXBUtils$JPackage"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.jaxb.JAXBUtils$JDefinedClass"));
    }

    private void produceProxyIfExist(BuildProducer<NativeImageProxyDefinitionBuildItem> proxies, String s) {
        try {
            Class.forName(s);
            proxies.produce(new NativeImageProxyDefinitionBuildItem(s));
        } catch (ClassNotFoundException e) {
            //silent fail
        }
    }

    @BuildStep
    public void registerReflectionItems(BuildProducer<ReflectiveClassBuildItem> reflectiveItems) {
        //TODO load all bus-extensions.txt file and parse it to generate the reflective class.
        //TODO load all handler from https://github.com/apache/cxf/tree/master/rt/frontend/jaxws/src/main/java/org/apache/cxf/jaxws/handler/types
        reflectiveItems.produce(new ReflectiveClassBuildItem(true, false, "org.apache.cxf.common.jaxb.NamespaceMapper"));

        reflectiveItems.produce(new ReflectiveClassBuildItem(true, true,
                "org.apache.cxf.common.spi.ClassLoaderService",
                "org.apache.cxf.bindings.xformat.ObjectFactory",
                "org.apache.cxf.ws.addressing.wsdl.ObjectFactory",
                "org.apache.cxf.transports.http.configuration.ObjectFactory",
                "org.apache.cxf.wsdl.http.ObjectFactory",
                "com.sun.xml.ws.runtime.config.ObjectFactory",
                "com.oracle.xmlns.webservices.jaxws_databinding.ObjectFactory",
                "org.apache.cxf.common.spi.GeneratedClassClassLoaderCapture",
                "org.apache.cxf.common.spi.ClassGeneratorClassLoader$TypeHelperClassLoader",
                "org.apache.cxf.common.util.ASMHelper",
                "org.apache.cxf.common.util.ASMHelperImpl",
                "org.apache.cxf.common.spi.ClassLoaderProxyService",
                "org.apache.cxf.common.spi.GeneratedNamespaceClassLoader",
                "org.apache.cxf.common.spi.NamespaceClassCreator",
                "org.apache.cxf.common.spi.NamespaceClassGenerator",
                "org.apache.cxf.binding.corba.utils.CorbaFixedAnyImplClassCreatorProxyService",
                "org.apache.cxf.binding.corba.utils.CorbaFixedAnyImplClassCreator",
                "org.apache.cxf.binding.corba.utils.CorbaFixedAnyImplClassLoader",
                "org.apache.cxf.binding.corba.utils.CorbaFixedAnyImplGenerator",
                "org.apache.cxf.jaxb.WrapperHelperProxyService",
                "org.apache.cxf.jaxb.WrapperHelperCreator",
                "org.apache.cxf.jaxb.WrapperHelperClassGenerator",
                "org.apache.cxf.jaxb.WrapperHelperClassLoader",
                "org.apache.cxf.jaxb.FactoryClassProxyService",
                "org.apache.cxf.jaxb.FactoryClassCreator",
                "org.apache.cxf.jaxb.FactoryClassGenerator",
                "org.apache.cxf.jaxb.FactoryClassLoader",
                "org.apache.cxf.jaxws.spi.WrapperClassCreatorProxyService",
                "org.apache.cxf.jaxws.spi.WrapperClassCreator",
                "org.apache.cxf.jaxws.spi.WrapperClassLoader",
                "org.apache.cxf.jaxws.spi.WrapperClassGenerator",
                "org.apache.cxf.endpoint.dynamic.ExceptionClassCreatorProxyService",
                "org.apache.cxf.endpoint.dynamic.ExceptionClassCreator",
                "org.apache.cxf.endpoint.dynamic.ExceptionClassLoader",
                "org.apache.cxf.endpoint.dynamic.ExceptionClassGenerator",
                "org.apache.cxf.ws.addressing.WSAddressingFeature",
                "org.apache.cxf.ws.addressing.impl.AddressingWSDLExtensionLoader",
                "org.apache.cxf.wsdl.ExtensionClassCreatorProxyService",
                "org.apache.cxf.wsdl.ExtensionClassCreator",
                "org.apache.cxf.wsdl.ExtensionClassLoader",
                "org.apache.cxf.wsdl.ExtensionClassGenerator",
                "java.net.HttpURLConnection",
                "com.sun.xml.bind.v2.schemagen.xmlschema.Schema",
                "com.sun.xml.bind.v2.schemagen.xmlschema.package-info",
                "com.sun.org.apache.xerces.internal.dom.DocumentTypeImpl",
                "org.w3c.dom.DocumentType",
                "java.lang.Throwable",
                "java.nio.charset.Charset",
                "com.sun.org.apache.xerces.internal.parsers.StandardParserConfiguration",
                "com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource",
                "com.sun.org.apache.xml.internal.resolver.readers.XCatalogReader",
                "com.sun.org.apache.xml.internal.resolver.readers.ExtendedXMLCatalogReader",
                "com.sun.org.apache.xml.internal.resolver.Catalog",
                "org.apache.xml.resolver.readers.OASISXMLCatalogReader",
                "com.sun.org.apache.xml.internal.resolver.readers.XCatalogReader",
                "com.sun.org.apache.xml.internal.resolver.readers.OASISXMLCatalogReader",
                "com.sun.org.apache.xml.internal.resolver.readers.TR9401CatalogReader",
                "com.sun.org.apache.xml.internal.resolver.readers.SAXCatalogReader",
                //"com.sun.xml.txw2.TypedXmlWriter",
                //"com.sun.codemodel.JAnnotationWriter",
                //"com.sun.xml.txw2.ContainerElement",
                "javax.xml.parsers.DocumentBuilderFactory",
                "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl",
                "com.sun.org.apache.xml.internal.serializer.ToXMLStream",
                "com.sun.org.apache.xerces.internal.dom.EntityImpl",
                "org.apache.cxf.common.jaxb.JAXBUtils$S2JJAXBModel",
                "org.apache.cxf.common.jaxb.JAXBUtils$Options",
                "org.apache.cxf.common.jaxb.JAXBUtils$JCodeModel",
                "org.apache.cxf.common.jaxb.JAXBUtils$Mapping",
                "org.apache.cxf.common.jaxb.JAXBUtils$TypeAndAnnotation",
                "org.apache.cxf.common.jaxb.JAXBUtils$JType",
                "org.apache.cxf.common.jaxb.JAXBUtils$JPackage",
                "org.apache.cxf.common.jaxb.JAXBUtils$JDefinedClass",
                "com.sun.xml.bind.v2.model.nav.ReflectionNavigator",
                "com.sun.xml.bind.v2.runtime.unmarshaller.StAXExConnector",
                "com.sun.xml.bind.v2.runtime.unmarshaller.FastInfosetConnector",
                "com.sun.xml.bind.v2.runtime.output.FastInfosetStreamWriterOutput",
                "org.jvnet.staxex.XMLStreamWriterEx",
                "com.sun.xml.bind.v2.runtime.output.StAXExStreamWriterOutput",
                "org.jvnet.fastinfoset.stax.LowLevelFastInfosetStreamWriter",
                "com.sun.xml.fastinfoset.stax.StAXDocumentSerializer",
                "com.sun.xml.fastinfoset.stax.StAXDocumentParser",
                "org.jvnet.fastinfoset.stax.FastInfosetStreamReader",
                "org.jvnet.staxex.XMLStreamReaderEx",
                // missing from jaxp extension
                //GregorSamsa but which package ???
                "com.sun.org.apache.xalan.internal.xsltc.dom.CollatorFactoryBase",
                //objecttype in jaxp
                "com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaLoader",
                "java.lang.Object",
                "java.lang.String",
                "java.math.BigInteger",
                "java.math.BigDecimal",
                "javax.xml.datatype.XMLGregorianCalendar",
                "javax.xml.datatype.Duration",
                "java.lang.Integer",
                "java.lang.Long",
                "java.lang.Short",
                "java.lang.Float",
                "java.lang.Double",
                "java.lang.Boolean",
                "java.lang.Byte",
                "java.lang.StringBuffer",
                "java.lang.Throwable",
                "java.lang.Character",
                "com.sun.xml.bind.api.CompositeStructure",
                "java.net.URI",
                "javax.xml.bind.JAXBElement",
                "javax.xml.namespace.QName",
                "java.awt.Image",
                "java.io.File",
                "java.lang.Class",
                "java.lang.Void",
                "java.net.URL",
                "java.util.Calendar",
                "java.util.Date",
                "java.util.GregorianCalendar",
                "java.util.UUID",
                "javax.activation.DataHandler",
                "javax.xml.transform.Source",
                "com.sun.org.apache.xml.internal.serializer.ToXMLSAXHandler",
                "com.sun.org.apache.xerces.internal.xni.parser.XMLParserConfiguration",
                "com.sun.org.apache.xerces.internal.parsers.StandardParserConfiguration",
                "com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource",
                "org.xml.sax.helpers.XMLReaderAdapter",
                "org.xml.sax.helpers.XMLFilterImpl",
                "javax.xml.validation.ValidatorHandler",
                "org.xml.sax.ext.DefaultHandler2",
                "org.xml.sax.helpers.DefaultHandler",
                "com.sun.org.apache.xalan.internal.lib.Extensions",
                "com.sun.org.apache.xalan.internal.lib.ExsltCommon",
                "com.sun.org.apache.xalan.internal.lib.ExsltMath",
                "com.sun.org.apache.xalan.internal.lib.ExsltSets",
                "com.sun.org.apache.xalan.internal.lib.ExsltDatetime",
                "com.sun.org.apache.xalan.internal.lib.ExsltStrings",
                "com.sun.org.apache.xerces.internal.dom.DocumentImpl",
                "com.sun.org.apache.xalan.internal.processor.TransformerFactoryImpl",
                "com.sun.org.apache.xerces.internal.dom.CoreDocumentImpl",
                "com.sun.org.apache.xerces.internal.dom.PSVIDocumentImpl",
                "com.sun.org.apache.xpath.internal.domapi.XPathEvaluatorImpl",
                "com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaValidator",
                "com.sun.org.apache.xerces.internal.impl.dtd.XMLDTDValidator",
                "com.sun.org.apache.xml.internal.utils.FastStringBuffer",
                "com.sun.xml.internal.stream.events.XMLEventFactoryImpl",
                "com.sun.xml.internal.stream.XMLOutputFactoryImpl",
                "com.sun.xml.internal.stream.XMLInputFactoryImpl",
                "com.sun.org.apache.xerces.internal.jaxp.datatype.DatatypeFactoryImpl",
                "javax.xml.stream.XMLStreamConstants",
                "com.sun.org.apache.xalan.internal.xslt.XSLProcessorVersion",
                "com.sun.org.apache.xalan.internal.processor.XSLProcessorVersion",
                "com.sun.org.apache.xalan.internal.Version",
                "com.sun.org.apache.xerces.internal.framework.Version",
                "com.sun.org.apache.xerces.internal.impl.Version",
                "org.apache.crimson.parser.Parser2",
                "org.apache.tools.ant.Main",
                "org.w3c.dom.Document",
                "org.w3c.dom.Node",
                "org.xml.sax.Parser",
                "org.xml.sax.XMLReader",
                "org.xml.sax.helpers.AttributesImpl",
                "org.apache.cxf.common.logging.Slf4jLogger",
                "io.quarkiverse.cxf.CxfInfoProducer",
                "io.quarkiverse.cxf.AddressTypeExtensibility",
                "io.quarkiverse.cxf.UsingAddressingExtensibility",
                "io.quarkiverse.cxf.CXFException",
                "io.quarkiverse.cxf.HTTPClientPolicyExtensibility",
                "io.quarkiverse.cxf.HTTPServerPolicyExtensibility",
                "io.quarkiverse.cxf.XMLBindingMessageFormatExtensibility",
                "io.quarkiverse.cxf.XMLFormatBindingExtensibility",
                "org.apache.cxf.common.util.ReflectionInvokationHandler",
                "com.sun.codemodel.internal.writer.FileCodeWriter",
                "com.sun.codemodel.writer.FileCodeWriter",
                "com.sun.xml.internal.bind.marshaller.NoEscapeHandler",
                "com.sun.xml.internal.bind.marshaller.MinimumEscapeHandler",
                "com.sun.xml.internal.bind.marshaller.DumbEscapeHandler",
                "com.sun.xml.internal.bind.marshaller.NioEscapeHandler",
                "com.sun.xml.bind.marshaller.NoEscapeHandler",
                "com.sun.xml.bind.marshaller.MinimumEscapeHandler",
                "com.sun.xml.bind.marshaller.DumbEscapeHandler",
                "com.sun.xml.bind.marshaller.NioEscapeHandler",
                "com.sun.tools.internal.xjc.api.XJC",
                "com.sun.tools.xjc.api.XJC",
                "com.sun.xml.internal.bind.api.JAXBRIContext",
                "com.sun.xml.bind.api.JAXBRIContext",
                "org.apache.cxf.common.util.ReflectionInvokationHandler",
                "javax.xml.ws.wsaddressing.W3CEndpointReference",
                "org.apache.cxf.common.jaxb.JAXBBeanInfo",
                "javax.xml.bind.JAXBContext",
                "com.sun.xml.bind.v2.runtime.LeafBeanInfoImpl",
                "com.sun.xml.bind.v2.runtime.ArrayBeanInfoImpl",
                "com.sun.xml.bind.v2.runtime.ValueListBeanInfoImpl",
                "com.sun.xml.bind.v2.runtime.AnyTypeBeanInfo",
                "com.sun.xml.bind.v2.runtime.JaxBeanInfo",
                "com.sun.xml.bind.v2.runtime.ClassBeanInfoImpl",
                "com.sun.xml.bind.v2.runtime.CompositeStructureBeanInfo",
                "com.sun.xml.bind.v2.runtime.ElementBeanInfoImpl",
                "com.sun.xml.bind.v2.runtime.MarshallerImpl",
                "com.sun.xml.messaging.saaj.soap.SOAPDocumentImpl",
                "com.sun.xml.internal.messaging.saaj.soap.SOAPDocumentImpl",
                "com.sun.org.apache.xerces.internal.dom.DOMXSImplementationSourceImpl",
                "javax.wsdl.Types",
                "javax.wsdl.extensions.mime.MIMEPart",
                "com.sun.xml.bind.v2.runtime.BridgeContextImpl",
                "com.sun.xml.bind.v2.runtime.JAXBContextImpl",
                "com.sun.xml.bind.subclassReplacements",
                "com.sun.xml.bind.defaultNamespaceRemap",
                "com.sun.xml.bind.c14n",
                "com.sun.xml.bind.v2.model.annotation.RuntimeAnnotationReader",
                "com.sun.xml.bind.XmlAccessorFactory",
                "com.sun.xml.bind.treatEverythingNillable",
                "com.sun.xml.bind.retainReferenceToInfo",
                "com.sun.xml.internal.bind.subclassReplacements",
                "com.sun.xml.internal.bind.defaultNamespaceRemap",
                "com.sun.xml.internal.bind.c14n",
                "org.apache.cxf.common.jaxb.SchemaCollectionContextProxy",
                "com.sun.xml.internal.bind.v2.model.annotation.RuntimeAnnotationReader",
                "com.sun.xml.internal.bind.XmlAccessorFactory",
                "com.sun.xml.internal.bind.treatEverythingNillable",
                "com.sun.xml.bind.marshaller.CharacterEscapeHandler",
                "com.sun.xml.internal.bind.marshaller.CharacterEscapeHandler",
                "com.sun.org.apache.xerces.internal.dom.ElementNSImpl",
                "sun.security.ssl.SSLLogger",
                "com.ibm.wsdl.extensions.schema.SchemaImpl",
                //TODO add refection only if soap 1.2
                "com.ibm.wsdl.extensions.soap12.SOAP12AddressImpl",
                "com.ibm.wsdl.extensions.soap12.SOAP12AddressSerializer",
                "com.ibm.wsdl.extensions.soap12.SOAP12BindingImpl",
                "com.ibm.wsdl.extensions.soap12.SOAP12BindingSerializer",
                "com.ibm.wsdl.extensions.soap12.SOAP12BodyImpl",
                "com.ibm.wsdl.extensions.soap12.SOAP12BodySerializer",
                "com.ibm.wsdl.extensions.soap12.SOAP12Constants",
                "com.ibm.wsdl.extensions.soap12.SOAP12FaultImpl",
                "com.ibm.wsdl.extensions.soap12.SOAP12FaultSerializer",
                "com.ibm.wsdl.extensions.soap12.SOAP12HeaderFaultImpl",
                "com.ibm.wsdl.extensions.soap12.SOAP12HeaderImpl",
                "com.ibm.wsdl.extensions.soap12.SOAP12HeaderSerializer",
                "com.ibm.wsdl.extensions.soap12.SOAP12OperationImpl",
                "com.ibm.wsdl.extensions.soap12.SOAP12OperationSerializer",
                "com.sun.xml.internal.bind.retainReferenceToInfo"));
        reflectiveItems.produce(new ReflectiveClassBuildItem(
                false,
                false,
                //manually added
                "org.apache.cxf.jaxws.spi.ProviderImpl",
                "com.sun.xml.bind.v2.JAXBContextFactory",
                "org.apache.cxf.wsdl.interceptors.BareInInterceptor",
                "com.sun.msv.reader.GrammarReaderController",
                "org.apache.cxf.binding.soap.interceptor.RPCInInterceptor",
                "org.apache.cxf.wsdl.interceptors.DocLiteralInInterceptor",
                "StaxSchemaValidationInInterceptor",
                "org.apache.cxf.binding.soap.interceptor.SoapHeaderInterceptor",
                "org.apache.cxf.binding.soap.model.SoapHeaderInfo",
                "javax.xml.stream.XMLStreamReader",
                "java.util.List",
                "org.apache.cxf.service.model.BindingOperationInfo",
                "org.apache.cxf.binding.soap.interceptor.CheckFaultInterceptor",
                "org.apache.cxf.interceptor.ClientFaultConverter",
                "org.apache.cxf.binding.soap.interceptor.EndpointSelectionInterceptor",
                "java.io.InputStream",
                "org.apache.cxf.service.model.MessageInfo",
                "org.apache.cxf.binding.soap.interceptor.MustUnderstandInterceptor",
                "org.apache.cxf.interceptor.OneWayProcessorInterceptor",
                "java.io.OutputStream",
                "org.apache.cxf.binding.soap.interceptor.ReadHeadersInterceptor",
                "org.apache.cxf.binding.soap.interceptor.RPCOutInterceptor",
                "org.apache.cxf.binding.soap.interceptor.Soap11FaultInInterceptor",
                "org.apache.cxf.binding.soap.interceptor.Soap11FaultOutInterceptor",
                "org.apache.cxf.binding.soap.interceptor.Soap12FaultInInterceptor",
                "org.apache.cxf.binding.soap.interceptor.Soap12FaultOutInterceptor",
                "org.apache.cxf.binding.soap.interceptor.SoapActionInInterceptor",
                "org.apache.cxf.binding.soap.wsdl.extensions.SoapBody",
                "javax.wsdl.extensions.soap.SOAPBody",
                "org.apache.cxf.binding.soap.model.SoapOperationInfo",
                "org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor$SoapOutEndingInterceptor",
                "org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor",
                "org.apache.cxf.binding.soap.interceptor.StartBodyInterceptor",
                "java.lang.Exception",
                "org.apache.cxf.staxutils.W3CDOMStreamWriter",
                "javax.xml.stream.XMLStreamReader",
                "javax.xml.stream.XMLStreamWriter",
                "org.apache.cxf.common.jaxb.JAXBContextCache",
                "com.ctc.wstx.sax.WstxSAXParserFactory",
                "com.ibm.wsdl.BindingFaultImpl",
                "com.ibm.wsdl.BindingImpl",
                "com.ibm.wsdl.BindingInputImpl",
                "com.ibm.wsdl.BindingOperationImpl",
                "com.ibm.wsdl.BindingOutputImpl",
                "com.ibm.wsdl.extensions.soap.SOAPAddressImpl",
                "com.ibm.wsdl.extensions.soap.SOAPBindingImpl",
                "com.ibm.wsdl.extensions.soap.SOAPBodyImpl",
                "com.ibm.wsdl.extensions.soap.SOAPFaultImpl",
                "com.ibm.wsdl.extensions.soap.SOAPHeaderImpl",
                "com.ibm.wsdl.extensions.soap.SOAPOperationImpl",
                "com.ibm.wsdl.factory.WSDLFactoryImpl",
                "com.ibm.wsdl.FaultImpl",
                "com.ibm.wsdl.InputImpl",
                "com.ibm.wsdl.MessageImpl",
                "com.ibm.wsdl.OperationImpl",
                "com.ibm.wsdl.OutputImpl",
                "com.ibm.wsdl.PartImpl",
                "com.ibm.wsdl.PortImpl",
                "com.ibm.wsdl.PortTypeImpl",
                "com.ibm.wsdl.ServiceImpl",
                "com.ibm.wsdl.TypesImpl",
                "com.sun.org.apache.xerces.internal.utils.XMLSecurityManager",
                "com.sun.org.apache.xerces.internal.utils.XMLSecurityPropertyManager",
                "com.sun.xml.bind.api.TypeReference",
                "com.sun.xml.bind.DatatypeConverterImpl",
                "com.sun.xml.internal.bind.api.TypeReference",
                "com.sun.xml.internal.bind.DatatypeConverterImpl",
                "ibm.wsdl.DefinitionImpl",
                "io.swagger.jaxrs.DefaultParameterExtension",
                "java.lang.invoke.MethodHandles",
                "java.rmi.RemoteException",
                "java.rmi.ServerException",
                "java.security.acl.Group",
                "javax.enterprise.inject.spi.CDI",
                "javax.jws.Oneway",
                "javax.jws.WebMethod",
                "javax.jws.WebParam",
                "javax.jws.WebResult",
                "javax.jws.WebService",
                "javax.security.auth.login.Configuration",
                "javax.servlet.WriteListener",
                "javax.wsdl.Binding",
                "javax.wsdl.BindingFault",
                "javax.wsdl.BindingInput",
                "javax.wsdl.BindingOperation",
                "javax.wsdl.BindingOutput",
                "javax.wsdl.Definition",
                "javax.wsdl.Fault",
                "javax.wsdl.Import",
                "javax.wsdl.Input",
                "javax.wsdl.Message",
                "javax.wsdl.Operation",
                "javax.wsdl.Output",
                "javax.wsdl.Part",
                "javax.wsdl.Port",
                "javax.wsdl.PortType",
                "javax.wsdl.Service",
                "javax.wsdl.Types",
                "javax.xml.bind.annotation.XmlSeeAlso",
                "javax.xml.soap.SOAPMessage",
                "javax.xml.transform.stax.StAXSource",
                "javax.xml.ws.Action",
                "javax.xml.ws.BindingType",
                "javax.xml.ws.Provider",
                "javax.xml.ws.RespectBinding",
                "javax.xml.ws.Service",
                "javax.xml.ws.ServiceMode",
                "javax.xml.ws.soap.Addressing",
                "javax.xml.ws.soap.MTOM",
                "javax.xml.ws.soap.SOAPBinding",
                "javax.xml.ws.WebFault",
                "javax.xml.ws.WebServiceProvider",
                "net.sf.cglib.proxy.Enhancer",
                "net.sf.cglib.proxy.MethodInterceptor",
                "net.sf.cglib.proxy.MethodProxy",
                "net.sf.ehcache.CacheManager",
                "org.apache.commons.logging.LogFactory",
                "org.apache.cxf.binding.soap.SoapBinding",
                "org.apache.cxf.binding.soap.SoapFault",
                "org.apache.cxf.binding.soap.SoapHeader",
                "org.apache.cxf.binding.soap.SoapMessage",
                "org.apache.cxf.binding.xml.XMLFault",
                "org.apache.cxf.bindings.xformat.XMLBindingMessageFormat",
                "org.apache.cxf.bindings.xformat.XMLFormatBinding",
                "org.apache.cxf.bus.CXFBusFactory",
                "org.apache.cxf.bus.managers.BindingFactoryManagerImpl",
                "org.apache.cxf.interceptor.Fault",
                "org.apache.cxf.jaxb.DatatypeFactory",
                "org.apache.cxf.jaxb.JAXBDataBinding",
                "org.apache.cxf.jaxrs.utils.JAXRSUtils",
                "org.apache.cxf.jaxws.binding.soap.SOAPBindingImpl",
                "org.apache.cxf.metrics.codahale.CodahaleMetricsProvider",
                "org.apache.cxf.message.Exchange",
                "org.apache.cxf.message.ExchangeImpl",
                "org.apache.cxf.message.StringMapImpl",
                "org.apache.cxf.message.StringMap",
                "org.apache.cxf.tools.fortest.cxf523.Database",
                "org.apache.cxf.tools.fortest.cxf523.DBServiceFault",
                "org.apache.cxf.tools.fortest.withannotation.doc.HelloWrapped",
                "org.apache.cxf.transports.http.configuration.HTTPClientPolicy",
                "org.apache.cxf.transports.http.configuration.HTTPServerPolicy",
                "org.apache.cxf.ws.addressing.wsdl.AttributedQNameType",
                "org.apache.cxf.ws.addressing.wsdl.ServiceNameType",
                "org.apache.cxf.wsdl.http.AddressType",
                "org.apache.hello_world.Greeter",
                "org.apache.hello_world_soap_http.types.StringStruct",
                "org.apache.karaf.jaas.boot.principal.Group",
                "org.apache.xerces.impl.Version",
                "org.apache.yoko.orb.OB.BootManager",
                "org.apache.yoko.orb.OB.BootManagerHelper",
                "org.codehaus.stax2.XMLStreamReader2",
                "org.eclipse.jetty.jaas.spi.PropertyFileLoginModule",
                "org.eclipse.jetty.jmx.MBeanContainer",
                "org.eclipse.jetty.plus.jaas.spi.PropertyFileLoginModule",
                "org.hsqldb.jdbcDriver",
                "org.jdom.Document",
                "org.jdom.Element",
                "org.osgi.framework.Bundle",
                "org.osgi.framework.BundleContext",
                "org.osgi.framework.FrameworkUtil",
                "org.slf4j.impl.StaticLoggerBinder",
                "org.slf4j.LoggerFactory",
                "org.springframework.aop.framework.Advised",
                "org.springframework.aop.support.AopUtils",
                "org.springframework.core.io.support.PathMatchingResourcePatternResolver",
                "org.springframework.core.type.classreading.CachingMetadataReaderFactory",
                "org.springframework.osgi.io.OsgiBundleResourcePatternResolver",
                "org.springframework.osgi.util.BundleDelegatingClassLoader"));
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
                "META-INF/services/javax.xml.ws.spi.Provider",
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
