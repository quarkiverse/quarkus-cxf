package io.quarkiverse.cxf.deployment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.xml.ws.WebServiceContext;

import org.apache.cxf.common.util.StringUtils;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.logging.Logger;

import io.quarkiverse.cxf.WebServiceContextProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;

public class WebServiceContextProcessor {
    private static final Logger log = Logger.getLogger(WebServiceContextProcessor.class);
    private static final Type WEBSERVICE_CONTEXT_TYPE = Type.create(CxfDotNames.WEBSERVICE_CONTEXT, Kind.CLASS);

    @BuildStep
    void webServiceContextProducer(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(WebServiceContextProducer.class));
    }

    /**
     * Quarkus CDI container does not handle {@link Resource} annotations and therefore we add {@link Inject} wherever
     * necessary, so that the {@link WebServiceContext} instances produced by our {@link WebServiceContextProducer}
     * can get injected.
     * <p>
     * Note that CXF does the injection of {@link Resource} annotated fields and methods using reflection at application
     * boot time. We disable that by setting {@link org.apache.cxf.jaxws.JaxWsServerFactoryBean#setBlockInjection(boolean)}
     * to {@code false} in {@link io.quarkiverse.cxf.QuarkusJaxWsServerFactoryBean}.
     *
     * @param combinedIndexBuildItem
     * @param annotationTransformer
     */
    @BuildStep
    void addInjectForResource(
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<AnnotationsTransformerBuildItem> annotationTransformer) {
        final IndexView index = combinedIndexBuildItem.getIndex();

        final ClassLevelResourceAnnotations classLevelResourceAnnotations = new ClassLevelResourceAnnotations(index);
        index.getAnnotations(CxfDotNames.JAKARTA_ANNOTATION_RESOURCE).stream()
                .filter(a -> a.target().kind() == AnnotationTarget.Kind.CLASS)
                .forEach(a -> classLevelResourceAnnotations.handleResource(a.target().asClass(), a));

        index.getAnnotations(CxfDotNames.JAKARTA_ANNOTATION_RESOURCES).stream()
                .filter(a -> a.target().kind() == AnnotationTarget.Kind.CLASS)
                .forEach(a -> {
                    final AnnotationValue value = a.value();
                    if (value != null) {
                        final List<AnnotationValue> values = value.asArrayList();
                        if (values.size() > 0) {
                            for (AnnotationValue res : values) {
                                classLevelResourceAnnotations.handleResource(a.target().asClass(), res.asNested());
                            }
                        }
                    }
                });
        /*
         * Add @Inject to fields having type jakarta.annotation.Resource so that Arc injects the WebServiceContext
         * instance produced by WebServiceContextProducer
         */
        annotationTransformer.produce(new AnnotationsTransformerBuildItem(AnnotationTransformation
                .forFields()
                .when(ctx -> {
                    final FieldInfo fld = ctx.declaration().asField();
                    return isWebServiceContext(fld.type())
                            && !ctx.hasAnnotation(CxfDotNames.INJECT)
                            && (ctx.hasAnnotation(CxfDotNames.JAKARTA_ANNOTATION_RESOURCE)
                                    || classLevelResourceAnnotations.containsField(fld.declaringClass().name(), fld.name()));
                })
                .transform(ctx -> {
                    final FieldInfo fld = ctx.declaration().asField();
                    if (log.isDebugEnabled()) {
                        log.debugf("Adding %s to %s.%s", jakarta.inject.Inject.class.getName(), fld.declaringClass().name(),
                                fld.name());
                    }
                    ctx.add(AnnotationInstance.create(CxfDotNames.INJECT, fld, new AnnotationValue[0]));
                })));

        /*
         * The same for methods
         */
        annotationTransformer.produce(new AnnotationsTransformerBuildItem(AnnotationTransformation
                .forMethods()
                .when(ctx -> {
                    final MethodInfo method = ctx.declaration().asMethod();
                    return setsWebServiceContext(method)
                            && !ctx.hasAnnotation(CxfDotNames.INJECT)
                            && (ctx.hasAnnotation(CxfDotNames.JAKARTA_ANNOTATION_RESOURCE)
                                    || classLevelResourceAnnotations.containsSetter(method.declaringClass().name(),
                                            method.name()));
                })
                .transform(ctx -> {
                    final MethodInfo method = ctx.declaration().asMethod();
                    if (log.isDebugEnabled()) {
                        log.debugf("Adding %s to %s.%s(%s)", jakarta.inject.Inject.class.getName(),
                                method.declaringClass().name(),
                                method.name(), method.parameterType(0).name());
                    }
                    ctx.add(AnnotationInstance.create(CxfDotNames.INJECT, method, new AnnotationValue[0]));
                })));
    }

    static class ClassLevelResourceAnnotations {
        private final Map<DotName, Set<String>> fields = new HashMap<>();
        private final Map<DotName, Set<String>> setters = new HashMap<>();
        private final IndexView index;

        public ClassLevelResourceAnnotations(IndexView index) {
            this.index = index;
        }

        void handleResource(ClassInfo annnotatedClass, AnnotationInstance resourceAnnotation) {
            final AnnotationValue name = resourceAnnotation.value("name");
            if (name == null || "".equals(name.asString())) {
                throw new IllegalStateException(
                        "@Resource annotation on " + annnotatedClass.name() + " must specify a value for 'name'");
            }
            final String setterName = resourceNameToSetter(name.asString());
            if (!findSetterForResource(annnotatedClass, setterName)
                    && !findFieldForResource(annnotatedClass, name.asString())) {
                throw new IllegalStateException("Could not find neither a setter or a field for @Resource(name = \"" + name
                        + "\") in " + annnotatedClass.name() + " or in any of its superclasses.");
            }
        }

        boolean findSetterForResource(ClassInfo clInfo, String setterName) {
            final MethodInfo method = clInfo.method(setterName, WEBSERVICE_CONTEXT_TYPE);
            if (method != null) {
                if (method.hasAnnotation(CxfDotNames.JAKARTA_ANNOTATION_RESOURCE)
                        || method.hasAnnotation(CxfDotNames.INJECT)) {
                    /* nothing to do the method transformer will handle this method */
                    return true;
                }
                setters.computeIfAbsent(clInfo.name(), k -> new HashSet<>()).add(setterName);
                return true;
            } else {
                final DotName superName = clInfo.superName();
                if (superName != null && !CxfDotNames.JAVA_LANG_OBJECT.equals(superName)) {
                    return findSetterForResource(index.getClassByName(superName), setterName);
                } else {
                    return false;
                }
            }
        }

        boolean findFieldForResource(ClassInfo clInfo, String fieldName) {
            final FieldInfo field = clInfo.field(fieldName);
            if (field != null && field.type().name().equals(CxfDotNames.WEBSERVICE_CONTEXT)) {
                if (field.hasAnnotation(CxfDotNames.JAKARTA_ANNOTATION_RESOURCE)
                        || field.hasAnnotation(CxfDotNames.INJECT)) {
                    /* nothing to do the field transformer will handle this field */
                    return true;
                }
                fields.computeIfAbsent(clInfo.name(), k -> new HashSet<>()).add(fieldName);
                return true;
            } else {
                final DotName superName = clInfo.superName();
                if (superName != null && !CxfDotNames.JAVA_LANG_OBJECT.equals(superName)) {
                    return findSetterForResource(index.getClassByName(superName), fieldName);
                } else {
                    return false;
                }
            }
        }

        public boolean containsField(DotName className, String fieldName) {
            Set<String> fieldNames = fields.get(className);
            return fieldNames != null && fieldNames.contains(fieldName);
        }

        public boolean containsSetter(DotName className, String setterName) {
            Set<String> fieldNames = setters.get(className);
            return fieldNames != null && fieldNames.contains(setterName);
        }

    }

    static boolean setsWebServiceContext(MethodInfo method) {
        return method.parametersCount() == 1 && isWebServiceContext(method.parameterType(0));
    }

    static boolean isWebServiceContext(Type fieldType) {
        return fieldType.kind() == Kind.CLASS && fieldType.asClassType().name().equals(CxfDotNames.WEBSERVICE_CONTEXT);
    }

    static String resourceNameToSetter(String resName) {
        return "set" + StringUtils.capitalize(resName);
    }

}
