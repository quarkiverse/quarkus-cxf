package io.quarkiverse.cxf.deployment;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.xml.ws.soap.SOAPBinding;

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
import io.quarkiverse.cxf.CxfClientProducer;
import io.quarkiverse.cxf.CxfFixedConfig;
import io.quarkiverse.cxf.CxfFixedConfig.ClientFixedConfig;
import io.quarkiverse.cxf.annotation.CXFClient;
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
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
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
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxies,
            BuildProducer<CxfClientBuildItem> clients) {
        IndexView index = combinedIndexBuildItem.getIndex();

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

                        clients.produce(
                                new CxfClientBuildItem(sei, soapBinding, wsNamespace, wsName));
                        proxies.produce(new NativeImageProxyDefinitionBuildItem(wsClassInfo.name().toString(),
                                "jakarta.xml.ws.BindingProvider", "java.io.Closeable", "org.apache.cxf.endpoint.Client"));

                    }
                });

    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void startClient(
            CXFRecorder recorder,
            List<CxfClientBuildItem> clients,
            CxfWrapperClassNamesBuildItem cxfWrapperClassNames,
            BuildProducer<SyntheticBeanBuildItem> synthetics) {

        final Map<String, List<String>> wrapperClassNames = cxfWrapperClassNames.getWrapperClassNames();

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
                        wrapperClassNames.get(client.getSei())))
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

    private static Map<String, ClientFixedConfig> findClientSEIsInUse(IndexView index, CxfFixedConfig config) {
        final Map<String, ClientFixedConfig> seiToClientConfig = new TreeMap<>();
        index.getAnnotations(CxfDotNames.CXFCLIENT_ANNOTATION).forEach(annotationInstance -> {
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
                final ClientFixedConfig clientConfig = findClientConfig(
                        config,
                        Optional.ofNullable(annotationInstance.value()).map(AnnotationValue::asString).orElse(null),
                        typeName);
                seiToClientConfig.put(typeName, clientConfig);
            }
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
            ClientFixedConfig result = config.clients.get(key);
            if (result == null) {
                /*
                 * We cannot tell at build time, whether this is illegal, because there can be some runtime config
                 * for the given key that we do not see here. So we just return a default ClientFixedConfig
                 */
                return ClientFixedConfig.createDefault();
            }
            return result;
        }

        final List<Map.Entry<String, ClientFixedConfig>> configsBySei = config.clients.entrySet().stream()
                .filter(cl -> serviceInterfaceName.equals(cl.getValue().serviceInterface.orElse(null)))
                .filter(cl -> !cl.getValue().alternative)
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
                        + configsBySei.stream().map(k -> "quarkus.cxf.\"" + k + "\".service-interface")
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
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        clients
                .stream()
                .map(CxfClientBuildItem::getSei)
                .forEach(sei -> {
                    generateCxfClientProducer(sei, generatedBeans, unremovableBeans);
                });
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

            // String p0class = InjectionPoint.class.getName();
            // String p1class = CXFClientInfo.class.getName();
            try (MethodCreator createService = classCreator.getMethodCreator("createService", sei, InjectionPoint.class)) {
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
                        CXFClientInfo.class);
                // >> .. {
                // >> Object cxfClient = this.loadCxfClient(ip, this.info);
                // >> return ({SEI})cxfClient;
                // >> }

                final ResultHandle cxfClient = createService.invokeVirtualMethod(loadCxfClient, thisHandle,
                        injectionPointHandle,
                        cxfClientInfoHandle);
                createService.returnValue(createService.checkCast(cxfClient, sei));

                // CatchBlockCreator print = overallCatch.addCatch(Throwable.class);
                // print.invokeVirtualMethod(MethodDescriptor.ofMethod(Throwable.class, "printStackTrace", void.class),
                // print.getCaughtException());

            }

            // try (MethodCreator createInfo = classCreator.getMethodCreator(
            // "createInfo",
            // "io.quarkiverse.cxf.CXFClientInfo",
            // p0class)) {
            // createInfo.addAnnotation(Produces.class);
            // createInfo.addAnnotation(CXFClient.class);
            //
            // // p0 (InjectionPoint);
            // ResultHandle p0;
            // ResultHandle p1;
            // ResultHandle cxfClient;
            //
            // p0 = createInfo.getMethodParam(0);
            //
            // MethodDescriptor loadCxfInfo = MethodDescriptor.ofMethod(
            // CxfClientProducer.class,
            // "loadCxfClientInfo",
            // "java.lang.Object",
            // p0class,
            // p1class);
            // // >> .. {
            // // >> Object cxfInfo = this().loadCxfInfo(ip, this.info);
            // // >> return (CXFClientInfo)cxfInfo;
            // // >> }
            //
            // p1 = createInfo.readInstanceField(info.getFieldDescriptor(), createInfo.getThis());
            // cxfClient = createInfo.invokeVirtualMethod(loadCxfInfo, createInfo.getThis(), p0, p1);
            // createInfo.returnValue(createInfo.checkCast(cxfClient, "io.quarkiverse.cxf
            // .CXFClientInfo"));
            // }

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

}
