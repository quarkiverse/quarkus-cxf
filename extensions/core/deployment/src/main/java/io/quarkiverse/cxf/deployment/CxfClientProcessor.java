package io.quarkiverse.cxf.deployment;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.soap.SOAPBinding;

import org.apache.cxf.endpoint.Client;
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

import io.quarkiverse.cxf.CXFClientData;
import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.CXFRecorder;
import io.quarkiverse.cxf.ClientInjectionPoint;
import io.quarkiverse.cxf.CxfClientConfig.HTTPConduitImpl;
import io.quarkiverse.cxf.CxfClientProducer;
import io.quarkiverse.cxf.CxfFixedConfig;
import io.quarkiverse.cxf.CxfFixedConfig.ClientFixedConfig;
import io.quarkiverse.cxf.HttpClientHTTPConduitFactory;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.graal.QuarkusCxfFeature;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.gizmo.AnnotatedElement;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

/**
 * Find WebService implementations and deploy them.
 */
public class CxfClientProcessor {

    private static final Logger LOGGER = Logger.getLogger(CxfClientProcessor.class);

    @BuildStep
    void collectClients(
            CxfFixedConfig config,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<NativeImageFeatureBuildItem> nativeImageFeatures,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxies,
            BuildProducer<CxfClientBuildItem> clients,
            BuildProducer<ClientSeiBuildItem> clientSeis) {
        IndexView index = combinedIndexBuildItem.getIndex();

        final AtomicBoolean hasRuntimeInitializedProxy = new AtomicBoolean(false);
        final Map<String, ClientFixedConfig> clientSEIsInUse = findClientSEIsInUse(index, config);
        CxfDeploymentUtils.webServiceAnnotations(index)
                .forEach(annotation -> {
                    final ClassInfo wsClassInfo = annotation.target().asClass();
                    ClientFixedConfig clientConfig = clientSEIsInUse.get(wsClassInfo.name().toString());
                    if (clientConfig != null) {
                        final String sei = wsClassInfo.name().toString();
                        AnnotationInstance webserviceClient = findWebServiceClientAnnotation(index, wsClassInfo.name());
                        final String wsName;
                        final String wsNamespace;
                        if (webserviceClient != null) {
                            wsName = webserviceClient.value("name").asString();
                            wsNamespace = webserviceClient.value("targetNamespace").asString();
                        } else {
                            wsName = Optional.ofNullable(annotation.value("serviceName"))
                                    .map(AnnotationValue::asString)
                                    .orElse("");
                            wsNamespace = Optional.ofNullable(annotation.value("targetNamespace"))
                                    .map(AnnotationValue::asString)
                                    .orElseGet(() -> CxfDeploymentUtils.getNameSpaceFromClassInfo(wsClassInfo));
                        }
                        final String soapBinding = Optional
                                .ofNullable(wsClassInfo.declaredAnnotation(CxfDotNames.BINDING_TYPE_ANNOTATION))
                                .map(bindingType -> bindingType.value().asString())
                                .orElse(SOAPBinding.SOAP11HTTP_BINDING);

                        final ProxyInfo proxyInfo = ProxyInfo.of(
                                Optional.ofNullable(clientConfig.native_()).map(native_ -> native_.runtimeInitialized())
                                        .orElse(false),
                                wsClassInfo,
                                index);
                        proxies.produce(new NativeImageProxyDefinitionBuildItem(proxyInfo.interfaces));

                        clients.produce(
                                new CxfClientBuildItem(sei, soapBinding, wsNamespace, wsName, proxyInfo.isRuntimeInitialized));
                        clientSeis.produce(new ClientSeiBuildItem(sei));

                        hasRuntimeInitializedProxy.set(hasRuntimeInitializedProxy.get() || proxyInfo.isRuntimeInitialized);

                    }
                });

        if (hasRuntimeInitializedProxy.get()) {
            nativeImageFeatures.produce(new NativeImageFeatureBuildItem(QuarkusCxfFeature.class));
        }

    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void startClient(
            CXFRecorder recorder,
            List<CxfClientBuildItem> clients,
            BuildProducer<SyntheticBeanBuildItem> synthetics) {

        //
        // Create injectable CXFClientInfo bean for each SEI-only interface, i.e. for each
        // class annotated as @WebService and without implementation. This bean fuells a
        // producer bean producing CXF proxy clients.
        clients
                .stream()
                .map(client -> new CXFClientData(
                        client.getSoapBinding(),
                        client.getSei(),
                        client.getWsName(),
                        client.getWsNamespace(),
                        client.isProxyClassRuntimeInitialized()))
                .map(cxf -> {
                    LOGGER.debugf("producing dedicated CXFClientInfo bean named '%s' for SEI %s", cxf.getSei(), cxf.getSei());
                    return SyntheticBeanBuildItem
                            .configure(CXFClientData.class)
                            .named(cxf.getSei())
                            .runtimeValue(recorder.cxfClientData(cxf))
                            .unremovable()
                            .setRuntimeInit()
                            .done();
                }).forEach(synthetics::produce);
    }

    private static AnnotationInstance findWebServiceClientAnnotation(IndexView index, DotName seiName) {
        Collection<AnnotationInstance> annotations = index.getAnnotations(CxfDotNames.WEBSERVICE_CLIENT);
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

    public static Stream<ClientInjectionPoint> findClientInjectionPoints(IndexView index) {
        return index.getAnnotations(CxfDotNames.CXFCLIENT_ANNOTATION).stream()
                .map(annotationInstance -> {
                    final AnnotationTarget target = annotationInstance.target();
                    Type type;
                    switch (target.kind()) {
                        case FIELD:
                            type = target.asField().type();
                            break;
                        case METHOD_PARAMETER:
                            MethodParameterInfo paramInfo = target.asMethodParameter();
                            MethodInfo method = paramInfo.method();
                            type = method.parameterTypes().get(paramInfo.position());
                            break;
                        default:
                            type = null;
                            break;
                    }
                    if (type != null) {
                        type = type.name().equals(CxfDotNames.INJECT_INSTANCE) ? type.asParameterizedType().arguments().get(0)
                                : type;
                        final String typeName = type.name().toString();
                        try {
                            ClassLoader cl = Thread.currentThread().getContextClassLoader();
                            final Class<?> sei = Class.forName(typeName, true, cl);
                            final AnnotationValue value = annotationInstance.value();
                            return new ClientInjectionPoint(value != null ? value.asString() : "", sei);
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException("Could not load Service Endpoint Interface " + typeName);
                        }
                    } else {
                        return null;
                    }
                })
                .filter(ip -> ip != null)
                .distinct();
    }

    private static Map<String, ClientFixedConfig> findClientSEIsInUse(IndexView index, CxfFixedConfig config) {
        final Map<String, ClientFixedConfig> seiToClientConfig = new TreeMap<>();
        findClientInjectionPoints(index).forEach(clientInjectionPoint -> {
            String sei = clientInjectionPoint.getSei().getName();
            final ClientFixedConfig clientConfig = findClientConfig(
                    config,
                    clientInjectionPoint.getConfigKey(),
                    sei);
            seiToClientConfig.put(sei, clientConfig);
        });
        return seiToClientConfig;
    }

    /**
     * Find a {@link ClientFixedConfig} by the given client configuration {@code key} or by the given
     * {@code serviceInterfaceName}.
     * Note that there is a similar algorithm implemented in
     * {@code io.quarkiverse.cxf.CxfClientProducer.selectorCXFClientInfo(CxfConfig, CxfFixedConfig, InjectionPoint, CXFClientInfo)}
     *
     * @param config the {@link CxfFixedConfig} to search in
     * @param key the key to lookup in the {@link CxfFixedConfig#clients} map
     * @param serviceInterfaceName {@link ClientFixedConfig#serviceInterface} to look for
     * @return a matching {@link ClientFixedConfig}, possibly a default one produced by
     *         {@link ClientFixedConfig#createDefault()}
     *
     * @throws IllegalStateException if there are too many {@link ClientFixedConfig}s available for the given
     *         {@code serviceInterfaceName}
     */
    static ClientFixedConfig findClientConfig(CxfFixedConfig config, String key, String serviceInterfaceName) {
        if (key != null && !key.isEmpty()) {
            ClientFixedConfig result = config.clients().get(key);
            if (result == null) {
                /*
                 * We cannot tell at build time, whether this is illegal, because there can be some runtime config
                 * for the given key that we do not see here. So we just return a default ClientFixedConfig
                 */
                return ClientFixedConfig.createDefault();
            }
            return result;
        }

        final List<Map.Entry<String, ClientFixedConfig>> configsBySei = config.clients().entrySet().stream()
                .filter(cl -> serviceInterfaceName.equals(cl.getValue().serviceInterface().orElse(null)))
                .filter(cl -> !cl.getValue().alternative())
                .collect(Collectors.toList());

        switch (configsBySei.size()) {
            case 0:
                /*
                 * We cannot tell at build time, whether this is illegal, because there can be some runtime config
                 * for the given key that we do not see here. So we just return a default ClientFixedConfig
                 */
                return ClientFixedConfig.createDefault();
            case 1:
                return configsBySei.get(0).getValue();
            default:
                throw new IllegalStateException("quarkus.cxf.*.service-interface = " + serviceInterfaceName
                        + " with alternative = false expected once, but found " + configsBySei.size() + " times in "
                        + configsBySei.stream().map(k -> "quarkus.cxf.\"" + k.getKey() + "\".service-interface")
                                .collect(Collectors.joining(", ")));
        }

    }

    /**
     * Build step to generate Producer beans suitable for injecting @CFXClient
     */
    @BuildStep
    void generateClientProducers(
            List<CxfClientBuildItem> clients,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        clients
                .stream()
                .map(CxfClientBuildItem::getSei)
                .forEach(sei -> {
                    generateCxfClientProducer(sei, generatedBeans, unremovableBeans);
                });

        if (clients.stream().anyMatch(CxfClientBuildItem::isProxyClassRuntimeInitialized)) {
            reflectiveClasses
                    .produce(ReflectiveClassBuildItem.builder(CxfClientProducer.RUNTIME_INITIALIZED_PROXY_MARKER_INTERFACE_NAME)
                            .build());
            copyMarkerInterfaceToApplication(generatedBeans);
        }
    }

    /**
     * Copies the {@value CxfClientProducer#RUNTIME_INITIALIZED_PROXY_MARKER_INTERFACE_NAME} from the current
     * classloader
     * to the user application. Why we have do that: First, the interface is package-visible so that adding it to
     * the client proxy definition forces GraalVM to generate the proxy class in
     * {@value CxfClientProducer#RUNTIME_INITIALIZED_PROXY_MARKER_INTERFACE_PACKAGE} package rather than under a random
     * package/class name. Thanks to that we can request the postponed initialization of the generated proxy class by
     * package
     * name.
     * More details in <a href="https://github.com/quarkiverse/quarkus-cxf/issues/580">#580</a>.
     *
     * @param generatedBeans
     */
    private void copyMarkerInterfaceToApplication(BuildProducer<GeneratedBeanBuildItem> generatedBeans) {
        byte[] bytes;
        try {
            bytes = IoUtil.readClassAsBytes(getClass().getClassLoader(),
                    CxfClientProducer.RUNTIME_INITIALIZED_PROXY_MARKER_INTERFACE_NAME);
        } catch (IOException e) {
            throw new RuntimeException("Could not read " + CxfClientProducer.RUNTIME_INITIALIZED_PROXY_MARKER_INTERFACE_NAME
                    + ".class from quarkus-cxf-deployment jar");
        }
        String className = CxfClientProducer.RUNTIME_INITIALIZED_PROXY_MARKER_INTERFACE_NAME.replace('.', '/');
        generatedBeans.produce(new GeneratedBeanBuildItem(className, bytes));
    }

    private void generateCxfClientProducer(
            String sei,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        // For a given SEI we create a dedicated client producer class, i.e.
        //
        // >> @ApplicationScoped
        // >> [public] {SEI}CxfClientProducer implements CxfClientProducer {
        // >> @Inject
        // >> @Named(value="{SEI}")
        // >> public CXFClientInfo info;
        // >>
        // >> @Produces
        // >> @CXFClient
        // >> {SEI} createService(InjectionPoint ip) {
        // >> return ({SEI}) super().loadCxfClient(ip, this.info);
        // >> }
        // >>
        // >> @Produces
        // >> @CXFClient
        // >> CXFClientInfo createInfo(InjectionPoint ip) {
        // >> return ({SEI}) super().loadCxfClientInfo(ip, this.info);
        // >> }
        // >> }
        String cxfClientProducerClassName = sei + "CxfClientProducer";

        ClassOutput classoutput = new GeneratedBeanGizmoAdaptor(generatedBeans);

        try (ClassCreator classCreator = ClassCreator.builder()
                .classOutput(classoutput)
                .className(cxfClientProducerClassName)
                .superClass(CxfClientProducer.class)
                .build()) {

            classCreator.addAnnotation(ApplicationScoped.class);

            // generates:
            // >> public CXFClientData info;
            final FieldCreator info = classCreator
                    .getFieldCreator("info", CXFClientData.class.getName())
                    .setModifiers(Modifier.PUBLIC);

            // add @Named to info, i.e.
            // >> @Named(value="{SEI}")
            // >> public CXFClientData info;

            info.addAnnotation(
                    AnnotationInstance.create(DotNames.NAMED, null, new AnnotationValue[] {
                            AnnotationValue.createStringValue("value", sei)
                    }));

            // add @Inject annotation to info, i.e.
            // >> @Inject
            // >> @Named(value="{SEI}")
            // >> public CXFClientData info;
            info.addAnnotation(
                    AnnotationInstance
                            .create(DotName.createSimple(Inject.class.getName()), null, new AnnotationValue[] {}));

            // create method
            // >> @Produces
            // >> @CXFClient
            // >> {SEI} createService(InjectionPoint ip) { .. }

            try (MethodCreator createService = classCreator.getMethodCreator(
                    "createService",
                    sei,
                    InjectionPoint.class)) {
                createService.addAnnotation(Produces.class);
                createService.addAnnotation(CXFClient.class);

                final ResultHandle thisHandle = createService.getThis();
                final ResultHandle injectionPointHandle = createService.getMethodParam(0);
                final ResultHandle cxfClientInfoHandle = createService.readInstanceField(info.getFieldDescriptor(), thisHandle);

                MethodDescriptor loadCxfClient = MethodDescriptor.ofMethod(
                        CxfClientProducer.class,
                        "loadCxfClient",
                        "java.lang.Object",
                        InjectionPoint.class,
                        CXFClientData.class);
                // >> .. {
                // >> Object cxfClient = this.loadCxfClient(ip, this.info);
                // >> return ({SEI})cxfClient;
                // >> }

                final ResultHandle cxfClient = createService.invokeVirtualMethod(
                        loadCxfClient,
                        thisHandle,
                        injectionPointHandle,
                        cxfClientInfoHandle);
                createService.returnValue(createService.checkCast(cxfClient, sei));

            }

            /*
             * The client proxy implements Closeable so we need to generate a disposer method
             * that calls proxy.close(). This is important because e.g. java.net.http.HttpClient based CXF clients
             * have some associated threads that is better to stop immediately.
             */
            // create method
            // >> @Produces
            // >> @CXFClient
            // >> void closeClient(@Disposes @CXFClient {SEI} client) { .. }
            try (MethodCreator createService = classCreator.getMethodCreator(
                    "closeClient",
                    void.class,
                    sei)) {
                AnnotatedElement clientParamAnnotations = createService.getParameterAnnotations(0);
                clientParamAnnotations.addAnnotation(Disposes.class);
                clientParamAnnotations.addAnnotation(CXFClient.class);

                final ResultHandle thisHandle = createService.getThis();
                final ResultHandle clientHandle = createService.getMethodParam(0);

                MethodDescriptor closeClient = MethodDescriptor.ofMethod(
                        CxfClientProducer.class,
                        "closeCxfClient",
                        void.class,
                        Object.class);
                // >> .. {
                // >> this.closeCxfClient(client);
                // >> }

                createService.invokeVirtualMethod(
                        closeClient,
                        thisHandle,
                        clientHandle);
                createService.returnVoid();

            }
        }

        // Eventually let's produce
        produceUnremovableBean(unremovableBeans, cxfClientProducerClassName);
    }

    private void produceUnremovableBean(
            BuildProducer<UnremovableBeanBuildItem> unremovables,
            String... args) {
        Arrays.stream(args)
                .map(UnremovableBeanBuildItem.BeanClassNameExclusion::new)
                .map(UnremovableBeanBuildItem::new)
                .forEach(unremovables::produce);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void customizers(
            CXFRecorder recorder,
            CxfFixedConfig config,
            List<FeatureBuildItem> features,
            BuildProducer<RuntimeBusCustomizerBuildItem> customizers) {
        final HTTPConduitImpl factory = config.httpConduitFactory()
                .orElse(
                        io.quarkiverse.cxf.deployment.QuarkusCxfFeature.hc5Present(features)
                                ? HTTPConduitImpl.CXFDefault
                                : HTTPConduitImpl.QuarkusCXFDefault);
        switch (factory) {
            case CXFDefault:
                // nothing to do
                break;
            case QuarkusCXFDefault:
            case VertxHttpClientHTTPConduitFactory:
                customizers.produce(new RuntimeBusCustomizerBuildItem(recorder.setVertxHttpClientHTTPConduitFactory()));
                break;
            case URLConnectionHTTPConduitFactory:
                customizers.produce(new RuntimeBusCustomizerBuildItem(recorder.setURLConnectionHTTPConduitFactory()));
                break;
            case HttpClientHTTPConduitFactory: {
                customizers.produce(new RuntimeBusCustomizerBuildItem(recorder.setHttpClientHTTPConduitFactory()));
                break;
            }
            default:
                throw new IllegalStateException("Unexpected " + HTTPConduitImpl.class.getSimpleName() + " value: "
                        + config.httpConduitFactory());
        }
    }

    private static class ProxyInfo {

        public static ProxyInfo of(
                boolean refersToRuntimeInitializedClasses,
                ClassInfo wsClassInfo,
                IndexView index) {
            final List<String> result = new ArrayList<>();
            result.add(wsClassInfo.name().toString());
            result.add(BindingProvider.class.getName());
            result.add("java.io.Closeable");
            result.add(Client.class.getName());

            if (refersToRuntimeInitializedClasses) {
                result.add(io.quarkiverse.cxf.CxfClientProducer.RUNTIME_INITIALIZED_PROXY_MARKER_INTERFACE_NAME);
            }
            return new ProxyInfo(result, refersToRuntimeInitializedClasses);
        }

        private ProxyInfo(List<String> interfaces, boolean isRuntimeInitialized) {
            super();
            this.interfaces = interfaces;
            this.isRuntimeInitialized = isRuntimeInitialized;
        }

        private final List<String> interfaces;
        private final boolean isRuntimeInitialized;

    }

}
