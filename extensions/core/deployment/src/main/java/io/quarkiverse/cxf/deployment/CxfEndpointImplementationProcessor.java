package io.quarkiverse.cxf.deployment;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.xml.ws.soap.SOAPBinding;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.quarkiverse.cxf.CXFRecorder;
import io.quarkiverse.cxf.CXFRecorder.BeanLookupStrategy;
import io.quarkiverse.cxf.CXFRecorder.ServletConfig;
import io.quarkiverse.cxf.CXFServletInfos;
import io.quarkiverse.cxf.CxfConfig;
import io.quarkiverse.cxf.CxfFixedConfig;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HandlerType;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Find WebService implementations and deploy them.
 */
public class CxfEndpointImplementationProcessor {

    private static final Logger LOGGER = Logger.getLogger(CxfEndpointImplementationProcessor.class);

    @BuildStep
    void beanDefiningAnnotation(
            BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotation) {
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(CxfDotNames.CXF_ENDPOINT_ANNOTATION,
                DotName.createSimple(ApplicationScoped.class), false));
    }

    @BuildStep
    void collectEndpoints(
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<CxfEndpointImplementationBuildItem> endpointImplementations,
            BuildProducer<ServiceSeiBuildItem> serviceSeis,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        IndexView index = combinedIndexBuildItem.getIndex();

        Set<String> reflectives = new TreeSet<>();
        CxfDeploymentUtils.webServiceAnnotations(index)
                .forEach(annotation -> {
                    final ClassInfo wsClassInfo = annotation.target().asClass();

                    final boolean hasWebServiceAnnotation = wsClassInfo.annotationsMap()
                            .containsKey(CxfDotNames.WEBSERVICE_ANNOTATION);
                    final boolean hasWebServiceProviderAnnotation = wsClassInfo.annotationsMap()
                            .containsKey(CxfDotNames.WEBSERVICE_PROVIDER_ANNOTATION);

                    if (isJaxwsEndpoint(wsClassInfo, index, true, hasWebServiceAnnotation,
                            hasWebServiceProviderAnnotation)) {
                        final String impl = wsClassInfo.name().toString();

                        final AnnotationInstance cxfEndpointAnnotation = wsClassInfo
                                .declaredAnnotation(CxfDotNames.CXF_ENDPOINT_ANNOTATION);
                        final String relPath = cxfEndpointAnnotation != null ? cxfEndpointAnnotation.value().asString() : null;

                        submitImpl(
                                endpointImplementations,
                                serviceSeis,
                                additionalBeans,
                                reflectives,
                                annotation,
                                wsClassInfo,
                                impl,
                                hasWebServiceProviderAnnotation,
                                relPath,
                                BeanLookupStrategy.TYPE);

                    } else if (Modifier.isInterface(wsClassInfo.flags())) {
                        String cl = wsClassInfo.name().toString();
                        try {
                            CxfDeploymentUtils.walkParents(
                                    Thread.currentThread().getContextClassLoader().loadClass(cl),
                                    reflectives::add);
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException("Could not load " + cl + " at build time", e);
                        }
                    }

                });
        CxfDeploymentUtils.cxfEndpointAnnotations(index)
                .forEach(annotation -> {
                    final MethodInfo methodInfo = annotation.target().asMethod();

                    final String impl = methodInfo.returnType().name().toString();
                    final ClassInfo wsClassInfo = index.getClassByName(methodInfo.returnType().name());
                    AnnotationInstance wsAnnot = wsClassInfo.declaredAnnotation(CxfDotNames.WEBSERVICE_ANNOTATION);
                    final boolean hasWebServiceProviderAnnotation = wsAnnot != null;
                    if (wsAnnot == null) {
                        wsAnnot = wsClassInfo.declaredAnnotation(CxfDotNames.WEBSERVICE_PROVIDER_ANNOTATION);
                        if (wsAnnot == null) {
                            throw new IllegalStateException("The return type '" + impl + "' of method " + methodInfo.toString()
                                    + " must be annotated with either " + CxfDotNames.WEBSERVICE_ANNOTATION + " or "
                                    + CxfDotNames.WEBSERVICE_PROVIDER_ANNOTATION);
                        }
                    }

                    submitImpl(
                            endpointImplementations,
                            serviceSeis,
                            additionalBeans,
                            reflectives,
                            wsAnnot,
                            wsClassInfo,
                            impl,
                            hasWebServiceProviderAnnotation,
                            annotation.value().asString(),
                            BeanLookupStrategy.TYPE_WITH_CXFENDPOINT_ANNOTATION);
                });

        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder(reflectives.toArray(new String[0])).methods().fields().build());
    }

    private void submitImpl(
            BuildProducer<CxfEndpointImplementationBuildItem> endpointImplementations,
            BuildProducer<ServiceSeiBuildItem> serviceSeis,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans, Set<String> reflectives, AnnotationInstance annotation,
            ClassInfo wsClassInfo,
            String impl,
            boolean hasWebServiceProviderAnnotation,
            String relativePathFromCxfEndpointAnnotation,
            BeanLookupStrategy beanLookupStrategy) {

        try {
            CxfDeploymentUtils.walkParents(
                    Thread.currentThread().getContextClassLoader().loadClass(impl),
                    reflectives::add);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load " + impl + " at build time", e);
        }
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(impl));

        final String wsNamespace = Optional.ofNullable(annotation.value("targetNamespace"))
                .map(AnnotationValue::asString)
                .orElseGet(() -> CxfDeploymentUtils.getNameSpaceFromClassInfo(wsClassInfo));

        final String wsName = Optional.ofNullable(annotation.value("serviceName"))
                .map(AnnotationValue::asString)
                .orElse(impl.contains(".") ? impl.substring(impl.lastIndexOf('.') + 1) : impl);

        String soapBinding = Optional
                .ofNullable(wsClassInfo.declaredAnnotation(CxfDotNames.BINDING_TYPE_ANNOTATION))
                .map(bindingType -> bindingType.value().asString())
                .orElse(SOAPBinding.SOAP11HTTP_BINDING);

        endpointImplementations.produce(
                new CxfEndpointImplementationBuildItem(
                        impl,
                        soapBinding,
                        wsNamespace,
                        wsName,
                        hasWebServiceProviderAnnotation,
                        relativePathFromCxfEndpointAnnotation,
                        beanLookupStrategy));
        serviceSeis.produce(new ServiceSeiBuildItem(impl));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    CXFServletInfosBuildItem startRoute(CXFRecorder recorder,
            BuildProducer<RouteBuildItem> routes,
            BeanContainerBuildItem beanContainer,
            List<CxfEndpointImplementationBuildItem> cxfEndpoints,
            List<CxfRouteRegistrationRequestorBuildItem> cxfRouteRegistrationRequestors,
            HttpBuildTimeConfig httpBuildTimeConfig,
            HttpConfiguration httpConfiguration,
            CxfBuildTimeConfig cxfBuildTimeConfig,
            CxfFixedConfig fixedConfig,
            CxfConfig cxfConfig) {
        final RuntimeValue<CXFServletInfos> infos = recorder.createInfos(fixedConfig.path(),
                httpBuildTimeConfig.rootPath);
        final List<String> requestors = cxfRouteRegistrationRequestors.stream()
                .map(CxfRouteRegistrationRequestorBuildItem::getRequestorName)
                .collect(Collectors.toList());
        if (!cxfEndpoints.isEmpty()) {
            RuntimeValue<Map<String, List<ServletConfig>>> implementorToCfgMap = recorder.implementorToCfgMap(cxfConfig);
            for (CxfEndpointImplementationBuildItem cxfWebService : cxfEndpoints) {
                recorder.addCxfServletInfo(
                        infos,
                        implementorToCfgMap,
                        fixedConfig.path(),
                        cxfWebService.getImplementor(),
                        cxfConfig,
                        cxfWebService.getWsName(),
                        cxfWebService.getWsNamespace(),
                        cxfWebService.getSoapBinding(),
                        cxfWebService.getImplementor(),
                        cxfWebService.isProvider(),
                        cxfWebService.getRelativePath(),
                        cxfWebService.getBeanLookupStrategy());
                requestors.add(cxfWebService.getImplementor());
            }
        }
        if (!requestors.isEmpty()) {
            final Handler<RoutingContext> handler = recorder.initServer(infos, beanContainer.getValue(),
                    httpConfiguration, fixedConfig);
            final String mappingPath = getMappingPath(fixedConfig.path());
            LOGGER.infof("Mapping a Vert.x handler for CXF to %s as requested by %s", mappingPath, requestors);
            routes.produce(RouteBuildItem.builder()
                    .route(mappingPath)
                    .handler(handler)
                    .handlerType(HandlerType.BLOCKING)
                    .build());
        } else {
            LOGGER.debug(
                    "Not registering a Vert.x handler for CXF as no WS endpoints were found at build time and no other extension requested it");
        }

        return new CXFServletInfosBuildItem(infos);
    }

    private static String getMappingPath(String path) {
        String mappingPath;
        if (path.endsWith("/")) {
            mappingPath = path + "*";
        } else {
            mappingPath = path + "/*";
        }
        return mappingPath;
    }

    /**
     * Adapted from <a href=
     * "https://github.com/wildfly/wildfly/blob/26.x/webservices/server-integration/src/main/java/org/jboss/as/webservices/util/ASHelper.java#L220-L245">WildFly<a>
     */
    private static boolean isJaxwsEndpoint(final ClassInfo clazz, final IndexView index, boolean log,
            boolean hasWebServiceAnnotation, boolean hasWebServiceProviderAnnotation) {
        // assert JAXWS endpoint class flags
        final short flags = clazz.flags();
        if (Modifier.isInterface(flags))
            return false;
        if (Modifier.isAbstract(flags))
            return false;
        if (!Modifier.isPublic(flags))
            return false;
        if (isJaxwsService(clazz, index))
            return false;
        if (!hasWebServiceAnnotation && !hasWebServiceProviderAnnotation) {
            return false;
        }
        if (hasWebServiceAnnotation && hasWebServiceProviderAnnotation) {
            if (log) {
                LOGGER.warnf(
                        "[JAXWS 2.2 spec, section 7.7] The @WebService and @WebServiceProvider annotations are mutually exclusive - %s won't be considered as a webservice endpoint, since it doesn't meet that requirement",
                        clazz.name().toString());
            }
            return false;
        }
        if (Modifier.isFinal(flags)) {
            if (log) {
                LOGGER.warnf("WebService endpoint class cannot be final - %s won't be considered as a webservice endpoint",
                        clazz.name().toString());
            }
            return false;
        }
        return true;
    }

    private static boolean isJaxwsService(final ClassInfo current, final IndexView index) {
        ClassInfo tmp = current;
        while (tmp != null) {
            final DotName superName = tmp.superName();
            if (CxfDotNames.JAXWS_SERVICE_CLASS.equals(superName)) {
                return true;
            }
            tmp = index.getClassByName(superName);
        }
        return false;
    }

}
