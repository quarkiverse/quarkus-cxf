package io.quarkiverse.cxf.deployment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.jws.WebParam;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.ws.soap.SOAPBinding;

import io.quarkus.gizmo.DescriptorUtils;
import org.apache.cxf.bus.extension.ExtensionManagerImpl;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.util.StringUtils;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.CXFQuarkusServlet;
import io.quarkiverse.cxf.CXFRecorder;
import io.quarkiverse.cxf.CXFServletInfos;
import io.quarkiverse.cxf.CxfClientProducer;
import io.quarkiverse.cxf.CxfConfig;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.GizmoClassVisitor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.jaxb.deployment.JaxbFileRootBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.undertow.deployment.FilterBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;
import io.quarkus.undertow.deployment.ServletDeploymentManagerBuildItem;
import io.quarkus.undertow.deployment.ServletInitParamBuildItem;

class QuarkusCxfProcessor {

    private static final String JAX_WS_SERVLET_NAME = "org.apache.cxf.transport.servlet.CXFNonSpringServlet";
    private static final String JAX_WS_FILTER_NAME = JAX_WS_SERVLET_NAME;
    private static final String FEATURE_CXF = "cxf";
    private static final DotName WEBSERVICE_ANNOTATION = DotName.createSimple("javax.jws.WebService");
    private static final DotName WEBMETHOD_ANNOTATION = DotName.createSimple("javax.jws.WebMethod");
    private static final DotName WEBPARAM_ANNOTATION = DotName.createSimple("javax.jws.WebParam");
    private static final DotName WEBRESULT_ANNOTATION = DotName.createSimple("javax.jws.WebResult");
    private static final DotName REQUEST_WRAPPER_ANNOTATION = DotName.createSimple("javax.xml.ws.RequestWrapper");
    private static final DotName RESPONSE_WRAPPER_ANNOTATION = DotName.createSimple("javax.xml.ws.ResponseWrapper");
    private static final DotName SOAPBINDING_ANNOTATION = DotName.createSimple("javax.jws.soap.SOAPBinding");
    private static final DotName WEBFAULT_ANNOTATION = DotName.createSimple("javax.jws.WebFault");
    private static final DotName ABSTRACT_FEATURE = DotName.createSimple("org.apache.cxf.feature.AbstractFeature");
    private static final DotName ABSTRACT_INTERCEPTOR = DotName.createSimple("org.apache.cxf.phase.AbstractPhaseInterceptor");
    private static final DotName DATABINDING = DotName.createSimple("org.apache.cxf.databinding");
    private static final DotName BINDING_TYPE_ANNOTATION = DotName.createSimple("javax.xml.ws.BindingType");
    private static final DotName XML_NAMESPACE = DotName.createSimple("com.sun.xml.txw2.annotation.XmlNamespace");
    private static final DotName XML_SEE_ALSO = DotName.createSimple("javax.xml.bind.annotation.XmlSeeAlso");
    private static final Logger LOGGER = Logger.getLogger(QuarkusCxfProcessor.class);
    private static final List<Class<? extends Annotation>> JAXB_ANNOTATIONS = Arrays.asList(
            XmlList.class,
            XmlAttachmentRef.class,
            XmlJavaTypeAdapter.class,
            XmlMimeType.class,
            XmlElement.class,
            XmlElementWrapper.class);

    @BuildStep
    public void generateWSDL(BuildProducer<NativeImageResourceBuildItem> ressources,
            CxfBuildTimeConfig cxfBuildTimeConfig) {
        if (cxfBuildTimeConfig.wsdlPath.isPresent()) {
            for (String wsdlPath : cxfBuildTimeConfig.wsdlPath.get()) {
                ressources.produce(new NativeImageResourceBuildItem(wsdlPath));
            }
        }
    }

    private static final String RESPONSE_CLASS_POSTFIX = "Response";

    //TODO check if better to reuse the cxf parsing system to generate only asm from their.

    private MethodDescriptor createWrapper(boolean isRequest, String operationName, String namespace, String resultNamespace,
            String resultName,
            String resultType,
            List<WrapperParameter> params, ClassOutput classOutput, String pkg, String className,
            List<MethodDescriptor> getters, List<MethodDescriptor> setters) {
        MethodDescriptor ctorDescriptor;
        String wrapperClassName = pkg + "." + className + (isRequest ? "" : RESPONSE_CLASS_POSTFIX);
        //WrapperClassGenerator
        try (ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .className(wrapperClassName)
                .build()) {

            classCreator.addAnnotation(AnnotationInstance.create(
                    DotName.createSimple(XmlRootElement.class.getName()), null,
                    new AnnotationValue[] {
                            AnnotationValue.createStringValue("name",
                                    operationName + (isRequest ? "" : RESPONSE_CLASS_POSTFIX)),
                            AnnotationValue.createStringValue("namespace", namespace) }));
            //TODO remove this attribute because gizmo do not support enum
            // wait release of gizmo with https://github.com/quarkusio/gizmo/pull/59
            // to enable it back
            /*
             * classCreator.addAnnotation(AnnotationInstance.create(
             * DotName.createSimple(XmlAccessorType.class.getName()), null,
             * new AnnotationValue[]{AnnotationValue.createEnumValue("value",
             * DotName.createSimple( "javax.xml.bind.annotation.XmlAccessType"),
             * "FIELD")}));
             */

            //if (!anonymous)
            classCreator.addAnnotation(AnnotationInstance.create(
                    DotName.createSimple(XmlType.class.getName()), null,
                    new AnnotationValue[] {
                            AnnotationValue.createStringValue("name",
                                    operationName + (isRequest ? "" : RESPONSE_CLASS_POSTFIX)),
                            AnnotationValue.createStringValue("namespace", namespace) }));

            // else
            //classCreator.addAnnotation(AnnotationInstance.create(
            //        DotName.createSimple(XmlType.class.getName()), null,
            //        new AnnotationValue[] { AnnotationValue.createStringValue("name", "")}));
            try (MethodCreator ctor = classCreator.getMethodCreator(MethodDescriptor.ofConstructor(wrapperClassName))) {
                ctor.setModifiers(Modifier.PUBLIC);
                ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), ctor.getThis());
                ctor.returnValue(null);
                ctorDescriptor = ctor.getMethodDescriptor();
            }
            if (!isRequest && resultName != null && resultType != null && !resultType.equals("void")) {
                //TODO check if method annotation must been forwarded
                try {
                    createWrapperClassField(getters, setters, classCreator, resultType, resultNamespace, resultName,
                            new ArrayList<AnnotationInstance>());
                } catch (Exception e) {
                    throw new RuntimeException("failed to create fields:" + resultType);
                }
            }
            int i = 0;
            for (WrapperParameter param : params) {

                AnnotationInstance webparamAnnotation = param.getAnnotation(WEBPARAM_ANNOTATION);
                String webParamName = "arg" + i;
                String webParamTargetNamespace = "";//namespace;
                WebParam.Mode webParamMode = WebParam.Mode.IN;
                boolean webParamHeader = false;
                //not used
                //String webParamPartName = webparamAnnotation.value("partName").asString();
                if (webparamAnnotation != null) {
                    AnnotationValue val = webparamAnnotation.value("name");
                    if (val != null) {
                        webParamName = val.asString();
                    }
                    val = webparamAnnotation.value("targetNamespace");
                    if (val != null) {
                        webParamTargetNamespace = val.asString();
                    }
                    val = webparamAnnotation.value("mode");
                    if (val != null) {
                        webParamMode = WebParam.Mode.valueOf(val.asEnum());
                    }
                    val = webparamAnnotation.value("header");
                    if (val != null) {
                        webParamHeader = val.asBoolean();
                    }
                }

                if ((webParamMode == WebParam.Mode.OUT && isRequest)
                        || (webParamMode == WebParam.Mode.IN && !isRequest))
                    continue;

                createWrapperClassField(getters, setters, classCreator, param.getParameterType().name().toString(),
                        webParamTargetNamespace, webParamName,
                        param.getAnnotations());
                i++;
            }
        }
        return ctorDescriptor;
    }

    private void createWrapperClassField(List<MethodDescriptor> getters, List<MethodDescriptor> setters,
            ClassCreator classCreator, String identifier, String webParamTargetNamespace, String webParamName,
            List<AnnotationInstance> paramAnnotations) {
        String fieldName = JAXBUtils.nameToIdentifier(webParamName, JAXBUtils.IdentifierType.VARIABLE);

        FieldCreator field = classCreator.getFieldCreator(fieldName, identifier)
                .setModifiers(Modifier.PRIVATE);
        List<DotName> jaxbAnnotationDotNames = JAXB_ANNOTATIONS.stream()
                .map(Class::getName)
                .map(DotName::createSimple)
                .collect(Collectors.toList());
        boolean annotationAdded = false;
        //wait fix of XmlAccessorType
        // before this fix witch annotation to getter
        /*
         * for (AnnotationInstance ann : paramAnnotations) {
         * if (jaxbAnnotationDotNames.contains(ann.name())) {
         * // copy jaxb annotation from param to
         * field.addAnnotation(AnnotationInstance.create(ann.name(), null, ann.values()));
         * annotationAdded = true;
         * }
         * }
         * if (!annotationAdded) {
         * List<AnnotationValue> annotationValues = new ArrayList<>();
         * annotationValues.add(AnnotationValue.createStringValue("name", webParamName));
         * //TODO handle a config for factory.isWrapperPartQualified, factory.isWrapperPartNillable,
         * factory.getWrapperPartMinOccurs
         * // and add annotation value here for it
         * field.addAnnotation(AnnotationInstance.create(DotName.createSimple(XmlElement.class.getName()),
         * null, annotationValues));
         * }
         * 
         */
        try (MethodCreator getter = classCreator.getMethodCreator(
                JAXBUtils.nameToIdentifier(webParamName, JAXBUtils.IdentifierType.GETTER),
                identifier)) {
            // start quick fix : switch annotation from field to getter
            for (AnnotationInstance ann : paramAnnotations) {
                if (jaxbAnnotationDotNames.contains(ann.name())) {
                    // copy jaxb annotation from param to getter
                    getter.addAnnotation(AnnotationInstance.create(ann.name(), null, ann.values()));
                    annotationAdded = true;
                }
            }
            if (!annotationAdded) {
                List<AnnotationValue> annotationValues = new ArrayList<>();
                annotationValues.add(AnnotationValue.createStringValue("name", webParamName));
                annotationValues.add(AnnotationValue.createStringValue("namespace", webParamTargetNamespace));
                //TODO handle a config for factory.isWrapperPartQualified, factory.isWrapperPartNillable, factory.getWrapperPartMinOccurs
                // and add annotation value here for it
                getter.addAnnotation(AnnotationInstance.create(DotName.createSimple(XmlElement.class.getName()),
                        null, annotationValues));
            }
            // end quick fix
            getter.setModifiers(Modifier.PUBLIC);
            getter.returnValue(getter.readInstanceField(field.getFieldDescriptor(), getter.getThis()));
            getters.add(getter.getMethodDescriptor());
        }
        try (MethodCreator setter = classCreator.getMethodCreator(
                JAXBUtils.nameToIdentifier(webParamName, JAXBUtils.IdentifierType.SETTER), void.class,
                identifier)) {
            setter.setModifiers(Modifier.PUBLIC);
            setter.writeInstanceField(field.getFieldDescriptor(), setter.getThis(), setter.getMethodParam(0));
            setter.returnValue(null);
            setters.add(setter.getMethodDescriptor());
        }

    }

    private String getNamespaceFromPackage(String pkg) {
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

    private static Set<String> classHelpers = new HashSet<>();
    static final MethodDescriptor LIST_GET = MethodDescriptor.ofMethod(List.class, "get", Object.class, int.class);
    static final MethodDescriptor LIST_ADDALL = MethodDescriptor.ofMethod(List.class, "addAll", Collection.class,
            boolean.class);

    static final MethodDescriptor ARRAYLIST_CTOR = MethodDescriptor.ofConstructor(ArrayList.class, int.class);
    static final MethodDescriptor JAXBELEMENT_GETVALUE = MethodDescriptor.ofMethod(JAXBElement.class, "getValue", Object.class);

    static final MethodDescriptor LIST_ADD = MethodDescriptor.ofMethod(List.class, "add", boolean.class, Object.class);
    private static final String WRAPPER_HELPER_POSTFIX = "_WrapperTypeHelper";
    private static final String WRAPPER_FACTORY_POSTFIX = "Factory";

    private String computeSignature(List<MethodDescriptor> getters, List<MethodDescriptor> setters) {
        StringBuilder b = new StringBuilder();
        b.append(setters.size()).append(':');
        for (int x = 0; x < setters.size(); x++) {
            MethodDescriptor getMethodDesc = getters.get(x);
            if (getMethodDesc == null) {
                b.append("null,");
            } else {
                //convert MethodDescriptor.getReturnType() format to Method.getReturnType().getName() format
                b.append(getMethodDesc.getName()).append('/');
                b.append(formatType(getMethodDesc.getReturnType())).append(',');
            }
        }
        return b.toString();
    }

    private String formatType(String str) {
        // overt jvn descriptor to class name
        if (str == null) {
            return null;
        }
        if (str.length() == 1) {
            switch (str) {
                case "V":
                    return "void";
                case "B":
                    return "byte";
                case "C":
                    return "character";
                case "D":
                    return "double";
                case "Z":
                    return "boolean";
                case "S":
                    return "short";
                case "J":
                    return "long";
                case "I":
                    return "integer";
                case "F":
                    return "float";
            }
        } else if (str.startsWith("[")) {
            return formatType(str.substring(1));
        } else if (str.startsWith("L") && str.endsWith(";")) {
            return str.substring(1, str.length() - 1).replace('/', '.');
        }
        return str.replace('/', '.').replaceAll(";", "");
    }

    public boolean isAssignableFrom(String interfaceOrSuper, ClassInfo toAssign) {
        if (toAssign == null) {
            return false;
        }
        if (toAssign.name().toString().equals(interfaceOrSuper)) {
            return true;
        }
        if (toAssign.superClassType().name().toString().equals(interfaceOrSuper)) {
            return true;
        }

        for (Type intfc : toAssign.interfaceTypes()) {
            if (intfc.name().toString().equals(interfaceOrSuper)) {
                return true;
            }
        }
        return false;
    }
    public boolean isArray(String str) {
        return (str != null && str.startsWith("["));
    }

    private String createWrapperHelper(ClassOutput classOutput, String pkg, String className,
            MethodDescriptor ctorDescriptor, List<MethodDescriptor> getters,
            List<MethodDescriptor> setters, IndexView index) {
        //WrapperClassGenerator
        int count = 1;
        String newClassName = pkg + "." + className + WRAPPER_HELPER_POSTFIX + count;

        while (classHelpers.contains(newClassName)) {
            count++;
            newClassName = pkg + "." + className + WRAPPER_HELPER_POSTFIX + count;
        }
        classHelpers.add(newClassName);
        try (ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .className(newClassName)
                .interfaces("org.apache.cxf.databinding.WrapperHelper")
                .build()) {
            ClassInfo objectFactoryCls = index.getClassByName(DotName.createSimple(pkg + ".ObjectFactory"));
            //TODO object factory creator
            // but must be always null for generated class so not sure if we keep that.

            FieldCreator factoryField = null;
            if (objectFactoryCls != null) {
                factoryField = classCreator.getFieldCreator("factory", objectFactoryCls.name().toString());
            }

            try (MethodCreator ctor = classCreator.getMethodCreator("<init>", "V")) {
                ctor.setModifiers(Modifier.PUBLIC);
                ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), ctor.getThis());
                if (objectFactoryCls != null && factoryField != null) {
                    ResultHandle factoryRH = ctor.newInstance(
                            MethodDescriptor.ofConstructor(objectFactoryCls.name().toString()));
                    ctor.writeInstanceField(factoryField.getFieldDescriptor(), ctor.getThis(), factoryRH);
                }
                ctor.returnValue(null);
            }

            try (MethodCreator getSignature = classCreator.getMethodCreator("getSignature", String.class)) {
                getSignature.setModifiers(Modifier.PUBLIC);
                ResultHandle signatureRH = getSignature.load(computeSignature(getters, setters));
                getSignature.returnValue(signatureRH);
            }
            try (MethodCreator createWrapperObject = classCreator.getMethodCreator("createWrapperObject", Object.class,
                    List.class)) {

                createWrapperObject.setModifiers(Modifier.PUBLIC);
                ResultHandle wrapperRH = createWrapperObject.newInstance(ctorDescriptor);

                for (int i = 0; i < setters.size(); i++) {
                    MethodDescriptor setter = setters.get(i);
                    MethodDescriptor getter = getters.get(i);
                    if (getter == null) {
                        if (setter == null) {
                            // null placeholder
                            continue;
                        }
                        //TODO throw missing setter exception
                    }
                    ClassInfo tp = index.getClassByName(DotName.createSimple(getter.getReturnType()));

                    boolean isList = isAssignableFrom(List.class.getName(), tp);

                    if (isList) {
                        // List aVal = obj.getA();
                        // List newA = (List)lst.get(99);
                        // if (aVal == null) {
                        // obj.setA(newA);
                        // } else if (newA != null) {
                        // aVal.addAll(newA);
                        // }
                        ResultHandle listRH = createWrapperObject.getMethodParam(0);
                        ResultHandle getterListRH = createWrapperObject.invokeVirtualMethod(getter, wrapperRH);
                        ResultHandle listValRH = createWrapperObject.invokeInterfaceMethod(LIST_GET, listRH,
                                createWrapperObject.load(i));
                        createWrapperObject.checkCast(listValRH, List.class);
                        BranchResult isNullBranch = createWrapperObject.ifNull(getterListRH);
                        try (BytecodeCreator getterValIsNull = isNullBranch.trueBranch()) {
                            getterValIsNull.checkCast(listValRH, getter.getReturnType());
                            getterValIsNull.invokeVirtualMethod(setter, wrapperRH, listValRH);

                        }
                        try (BytecodeCreator getterValIsNotNull = isNullBranch.falseBranch()) {
                            getterValIsNotNull.invokeInterfaceMethod(LIST_ADDALL, getterListRH, listValRH);
                        }
                    } else {
                        boolean isJaxbElement = isAssignableFrom(JAXBElement.class.getName(), tp);
                        ResultHandle listRH = createWrapperObject.getMethodParam(0);
                        ResultHandle listValRH = createWrapperObject.invokeInterfaceMethod(LIST_GET, listRH,
                                createWrapperObject.load(i));
                        if (DescriptorUtils.isPrimitive(getter.getReturnType())) {
                            if (tp != null) {
                                createWrapperObject.invokeVirtualMethod(setter, wrapperRH, listValRH);
                            }
                        } else if (isJaxbElement) {
                            ResultHandle factoryRH = createWrapperObject.readInstanceField(factoryField.getFieldDescriptor(),
                                    createWrapperObject.getThis());
                            String jaxbMethodName = "create" + className + setter.getName().substring(3);

                            ResultHandle jaxbSetRH = createWrapperObject.invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(objectFactoryCls.name().toString(),
                                    jaxbMethodName, setter.getParameterTypes()[0], getter.getReturnType()),
                                    factoryRH, listValRH);

                            createWrapperObject.invokeVirtualMethod(setter, wrapperRH, jaxbSetRH);
                        } else if (isArray(getter.getReturnType())) {
                            createWrapperObject.checkCast(listValRH, getter.getReturnType());
                            createWrapperObject.invokeVirtualMethod(setter, wrapperRH, listValRH);
                        } else {
                            createWrapperObject.checkCast(listValRH, getter.getReturnType());
                            createWrapperObject.invokeVirtualMethod(setter, wrapperRH, listValRH);
                        }
                    }
                }
                createWrapperObject.returnValue(wrapperRH);
            }
            try (MethodCreator getWrapperParts = classCreator.getMethodCreator("getWrapperParts", List.class, Object.class)) {
                getWrapperParts.setModifiers(Modifier.PUBLIC);
                ResultHandle arraylistRH = getWrapperParts.newInstance(ARRAYLIST_CTOR, getWrapperParts.load(getters.size()));
                ResultHandle objRH = getWrapperParts.getMethodParam(0);
                ResultHandle wrapperRH = getWrapperParts.checkCast(objRH, ctorDescriptor.getDeclaringClass());
                for (MethodDescriptor getter : getters) {
                    ResultHandle wrapperValRH = getWrapperParts.invokeVirtualMethod(getter, wrapperRH);
                    ClassInfo firstParam = index.getClassByName(DotName.createSimple(getter.getReturnType()));
                    boolean isJaxbElement = isAssignableFrom(JAXBElement.class.getName(), firstParam);
                    if (isJaxbElement) {
                        wrapperValRH = getWrapperParts.ifNull(wrapperValRH).falseBranch().invokeVirtualMethod(
                                JAXBELEMENT_GETVALUE,
                                wrapperValRH);
                    }

                    getWrapperParts.invokeInterfaceMethod(LIST_ADD, arraylistRH, wrapperValRH);
                }
                getWrapperParts.returnValue(arraylistRH);
            }

        }
        return newClassName;
    }

    private void createWrapperFactory(ClassOutput classOutput, String pkg, String className,
            MethodDescriptor ctorDescriptor) {
        String factoryClassName = pkg + "." + className + WRAPPER_FACTORY_POSTFIX;
        try (ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .className(factoryClassName)
                .build()) {
            String fullClassName = ctorDescriptor.getDeclaringClass();
            try (MethodCreator createWrapper = classCreator.getMethodCreator("create" + className, fullClassName)) {
                createWrapper.setModifiers(Modifier.PUBLIC);
                ResultHandle[] argsRH = new ResultHandle[ctorDescriptor.getParameterTypes().length];
                for (int i = 0; i < ctorDescriptor.getParameterTypes().length; i++) {
                    argsRH[i] = createWrapper.loadNull();
                }
                ResultHandle wrapperInstanceRH = createWrapper.newInstance(ctorDescriptor, argsRH);

                createWrapper.returnValue(wrapperInstanceRH);
            }
        }
    }

    private void createException(ClassOutput classOutput, String exceptionClassName, DotName name) {
        try (ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput).superClass(Exception.class)
                .className(exceptionClassName)
                .build()) {

            String FaultClassName = name.toString();

            FieldCreator field = classCreator.getFieldCreator("faultInfo", FaultClassName).setModifiers(Modifier.PRIVATE);
            //constructor
            try (MethodCreator ctor = classCreator.getMethodCreator("<init>", "V", String.class, FaultClassName)) {
                ctor.setModifiers(Modifier.PUBLIC);
                ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Exception.class, String.class), ctor.getThis(),
                        ctor.getMethodParam(0));
                ctor.writeInstanceField(field.getFieldDescriptor(), ctor.getThis(), ctor.getMethodParam(1));
                ctor.returnValue(null);
            }
            try (MethodCreator getter = classCreator.getMethodCreator("getFaultInfo", FaultClassName)) {
                getter.setModifiers(Modifier.PUBLIC);
                getter.returnValue(getter.readInstanceField(field.getFieldDescriptor(), getter.getThis()));
            }
        }
    }

    private static void createEclipseNamespaceMapper(ClassOutput classOutput) {

        String className = "org.apache.cxf.jaxb.EclipseNamespaceMapper";
        String slashedName = "org/apache/cxf/jaxb/EclipseNamespaceMapper";
        String superName = "org/eclipse/persistence/internal/oxm/record/namespaces/MapNamespacePrefixMapper";
        ClassWriter file = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        final GizmoClassVisitor cw = new GizmoClassVisitor(Gizmo.ASM_API_VERSION, file,
                classOutput.getSourceWriter(slashedName));
        FieldVisitor fv;
        MethodVisitor mv;
        cw.visit(Opcodes.V1_6,
                Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_SUPER,
                slashedName, null,
                superName, null);

        cw.visitSource("EclipseNamespaceMapper.java", null);

        fv = cw.visitField(Opcodes.ACC_PRIVATE, "nsctxt", "[Ljava/lang/String;", null, null);
        fv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/util/Map;)V",
                "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;)V", null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                superName, "<init>", "(Ljava/util/Map;)V", false);
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitInsn(Opcodes.RETURN);
        Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "setContextualNamespaceDecls", "([Ljava/lang/String;)V",
                null, null);
        mv.visitCode();
        l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(47, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitFieldInsn(Opcodes.PUTFIELD, slashedName, "nsctxt", "[Ljava/lang/String;");
        l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLineNumber(48, l1);
        mv.visitInsn(Opcodes.RETURN);
        l2 = new Label();
        mv.visitLabel(l2);
        mv.visitLocalVariable("this", "L" + slashedName + ";", null, l0, l2, 0);
        mv.visitLocalVariable("contextualNamespaceDecls", "[Ljava/lang/String;", null, l0, l2, 1);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getContextualNamespaceDecls", "()[Ljava/lang/String;", null, null);
        mv.visitCode();
        l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(51, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, slashedName, "nsctxt", "[Ljava/lang/String;");
        mv.visitInsn(Opcodes.ARETURN);
        l1 = new Label();

        mv.visitLabel(l1);
        mv.visitLocalVariable("this", "L" + slashedName + ";", null, l0, l1, 0);

        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getPreDeclaredNamespaceUris", "()[Ljava/lang/String;", null, null);
        mv.visitCode();
        l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(1036, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                superName,
                "getPreDeclaredNamespaceUris", "()[Ljava/lang/String;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 1);
        l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLineNumber(1037, l1);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, slashedName, "nsctxt", "[Ljava/lang/String;");
        l2 = new Label();
        mv.visitJumpInsn(Opcodes.IFNONNULL, l2);
        Label l3 = new Label();
        mv.visitLabel(l3);
        mv.visitLineNumber(1038, l3);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitLabel(l2);
        mv.visitLineNumber(1040, l2);
        mv.visitFrame(Opcodes.F_APPEND, 1, new Object[] { "[Ljava/lang/String;" }, 0, null);
        mv.visitTypeInsn(Opcodes.NEW, "java/util/ArrayList");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "asList",
                "([Ljava/lang/Object;)Ljava/util/List;", false);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>",
                "(Ljava/util/Collection;)V", false);
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        Label l4 = new Label();
        mv.visitLabel(l4);
        mv.visitLineNumber(1041, l4);
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitVarInsn(Opcodes.ISTORE, 3);
        Label l5 = new Label();
        mv.visitLabel(l5);
        Label l6 = new Label();
        mv.visitJumpInsn(Opcodes.GOTO, l6);
        Label l7 = new Label();
        mv.visitLabel(l7);
        mv.visitLineNumber(1042, l7);
        mv.visitFrame(Opcodes.F_APPEND, 2, new Object[] { "java/util/List", Opcodes.INTEGER }, 0, null);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, slashedName, "nsctxt", "[Ljava/lang/String;");
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitInsn(Opcodes.AALOAD);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "remove", "(Ljava/lang/Object;)Z", true);
        mv.visitInsn(Opcodes.POP);
        Label l8 = new Label();
        mv.visitLabel(l8);
        mv.visitLineNumber(1041, l8);
        mv.visitIincInsn(3, 2);
        mv.visitLabel(l6);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD,
                slashedName,
                "nsctxt", "[Ljava/lang/String;");
        mv.visitInsn(Opcodes.ARRAYLENGTH);
        mv.visitJumpInsn(Opcodes.IF_ICMPLT, l7);
        Label l9 = new Label();
        mv.visitLabel(l9);
        mv.visitLineNumber(1044, l9);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "size", "()I", true);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List",
                "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;", true);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "[Ljava/lang/String;");
        mv.visitInsn(Opcodes.ARETURN);
        Label l10 = new Label();
        mv.visitLabel(l10);
        mv.visitLocalVariable("this", "L" + slashedName + ";",
                null, l0, l10, 0);
        mv.visitLocalVariable("sup", "[Ljava/lang/String;", null, l1, l10, 1);
        mv.visitLocalVariable("s", "Ljava/util/List;", "Ljava/util/List<Ljava/lang/String;>;", l4, l10, 2);
        mv.visitLocalVariable("x", "I", null, l5, l9, 3);
        mv.visitMaxs(3, 4);
        mv.visitEnd();

        cw.visitEnd();

        classOutput.write(className, file.toByteArray());
    }

    private static void createNamespaceWrapper(ClassOutput classOutput,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            IndexView index) {
        String postFix = "";

        ClassInfo cls = index.getClassByName(
                DotName.createSimple("org.eclipse.persistence.internal.oxm.record.namespaces.MapNamespacePrefixMapper"));
        if (cls != null) {
            QuarkusCxfProcessor.createEclipseNamespaceMapper(classOutput);
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, "org.apache.cxf.jaxb.EclipseNamespaceMapper"));
            unremovableBeans.produce(new UnremovableBeanBuildItem(
                    new UnremovableBeanBuildItem.BeanClassNameExclusion("org.apache.cxf.jaxb.EclipseNamespaceMapper")));
            return;
        }

        cls = index.getClassByName(
                DotName.createSimple("com.sun.xml.internal.bind.marshaller.NamespacePrefixMapper"));
        if (cls != null) {
            postFix = "Internal";
        } else {
            cls = index.getClassByName(
                    DotName.createSimple("com.sun.xml.bind.marshaller.NamespacePrefixMapper"));
            if (cls != null) {
                postFix = "Internal";
            }
        }

        String superName = "com/sun/xml/"
                + ("RI".equals(postFix) ? "" : "internal/")
                + "bind/marshaller/NamespacePrefixMapper";
        String postFixedName = "org/apache/cxf/jaxb/NamespaceMapper" + postFix;
        //TODO translate to gizmo
        /*
         * String className = "org.apache.cxf.jaxb.NamespaceMapperRI";
         * try (ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
         * .superClass(com.sun.xml.bind.marshaller.NamespacePrefixMapper.class)
         * .className(className)
         * .setFinal(true)
         * .build()) {
         * FieldCreator nspref = classCreator.getFieldCreator("nspref", Map.class).setModifiers(Modifier.PRIVATE +
         * Modifier.FINAL);
         * FieldCreator nsctxt = classCreator.getFieldCreator("nsctxt", String.class).setModifiers(Modifier.PRIVATE);
         * FieldCreator EMPTY_STRING = classCreator.getFieldCreator("EMPTY_STRING", String.class).setModifiers(Modifier.PRIVATE
         * + Modifier.FINAL+ Modifier.STATIC);
         * try (MethodCreator staticCtor =
         * classCreator.getMethodCreator(MethodDescriptor.ofConstructor(className)).setModifiers(Modifier.STATIC)) {
         * ResultHandle newArrayInstance = staticCtor.newArray(String.class, 0);
         * staticCtor.writeStaticField(EMPTY_STRING.getFieldDescriptor(), newArrayInstance);
         * staticCtor.returnValue(null);
         * }
         * try (MethodCreator ctor = classCreator.getMethodCreator(MethodDescriptor.ofConstructor(className,
         * Map.class)).setModifiers(Modifier.PUBLIC)) {
         * ctor.
         * }
         * 
         * }
         */
        ClassWriter file = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        final GizmoClassVisitor cw = new GizmoClassVisitor(Gizmo.ASM_API_VERSION, file,
                classOutput.getSourceWriter(postFixedName));

        FieldVisitor fv;
        MethodVisitor mv;

        cw.visit(Opcodes.V1_6,
                Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_SUPER,
                postFixedName, null,
                superName, null);

        cw.visitSource("NamespaceMapper.java", null);

        fv = cw.visitField(Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL,
                "nspref", "Ljava/util/Map;",
                "Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;", null);
        fv.visitEnd();

        fv = cw.visitField(Opcodes.ACC_PRIVATE, "nsctxt", "[Ljava/lang/String;", null, null);
        fv.visitEnd();

        fv = cw.visitField(Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL + Opcodes.ACC_STATIC,
                "EMPTY_STRING", "[Ljava/lang/String;", null, null);
        fv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(30, l0);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");
        mv.visitFieldInsn(Opcodes.PUTSTATIC, postFixedName, "EMPTY_STRING", "[Ljava/lang/String;");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 0);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>",
                "(Ljava/util/Map;)V",
                "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;)V", null);
        mv.visitCode();
        l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(32, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>", "()V", false);
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLineNumber(29, l1);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETSTATIC, postFixedName, "EMPTY_STRING", "[Ljava/lang/String;");
        mv.visitFieldInsn(Opcodes.PUTFIELD, postFixedName, "nsctxt", "[Ljava/lang/String;");
        Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitLineNumber(33, l2);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitFieldInsn(Opcodes.PUTFIELD, postFixedName, "nspref", "Ljava/util/Map;");
        Label l3 = new Label();
        mv.visitLabel(l3);
        mv.visitLineNumber(34, l3);
        mv.visitInsn(Opcodes.RETURN);
        Label l4 = new Label();
        mv.visitLabel(l4);
        mv.visitLocalVariable("this", "L" + postFixedName + ";", null, l0, l4, 0);
        mv.visitLocalVariable("nspref",
                "Ljava/util/Map;", "Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;",
                l0, l4, 1);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getPreferredPrefix",
                "(Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/String;",
                null, null);
        mv.visitCode();
        l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(39, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, postFixedName, "nspref", "Ljava/util/Map;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map",
                "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String");
        mv.visitVarInsn(Opcodes.ASTORE, 4);
        l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLineNumber(40, l1);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        l2 = new Label();
        mv.visitJumpInsn(Opcodes.IFNULL, l2);
        l3 = new Label();
        mv.visitLabel(l3);
        mv.visitLineNumber(41, l3);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitLabel(l2);
        mv.visitLineNumber(43, l2);
        mv.visitFrame(Opcodes.F_APPEND, 1, new Object[] { "java/lang/String" }, 0, null);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitInsn(Opcodes.ARETURN);
        l4 = new Label();
        mv.visitLabel(l4);
        mv.visitLocalVariable("this", "L" + postFixedName + ";", null, l0, l4, 0);
        mv.visitLocalVariable("namespaceUri", "Ljava/lang/String;", null, l0, l4, 1);
        mv.visitLocalVariable("suggestion", "Ljava/lang/String;", null, l0, l4, 2);
        mv.visitLocalVariable("requirePrefix", "Z", null, l0, l4, 3);
        mv.visitLocalVariable("prefix", "Ljava/lang/String;", null, l1, l4, 4);
        mv.visitMaxs(2, 5);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "setContextualNamespaceDecls", "([Ljava/lang/String;)V", null, null);
        mv.visitCode();
        l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(47, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitFieldInsn(Opcodes.PUTFIELD, postFixedName, "nsctxt", "[Ljava/lang/String;");
        l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLineNumber(48, l1);
        mv.visitInsn(Opcodes.RETURN);
        l2 = new Label();
        mv.visitLabel(l2);
        mv.visitLocalVariable("this", "L" + postFixedName + ";", null, l0, l2, 0);
        mv.visitLocalVariable("contextualNamespaceDecls", "[Ljava/lang/String;", null, l0, l2, 1);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getContextualNamespaceDecls", "()[Ljava/lang/String;", null, null);
        mv.visitCode();
        l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(51, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, postFixedName, "nsctxt", "[Ljava/lang/String;");
        mv.visitInsn(Opcodes.ARETURN);
        l1 = new Label();

        mv.visitLabel(l1);
        mv.visitLocalVariable("this", "L" + postFixedName + ";", null, l0, l1, 0);

        mv.visitMaxs(1, 1);
        mv.visitEnd();

        cw.visitEnd();

        classOutput.write(postFixedName, file.toByteArray());
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, "org.apache.cxf.jaxb.NamespaceMapper" + postFix));

        unremovableBeans.produce(new UnremovableBeanBuildItem(
                new UnremovableBeanBuildItem.BeanClassNameExclusion("org.apache.cxf.jaxb.NamespaceMapper" + postFix)));
    }

    @BuildStep
    void markBeansAsUnremovable(BuildProducer<UnremovableBeanBuildItem> unremovables) {
        unremovables.produce(new UnremovableBeanBuildItem(new Predicate<BeanInfo>() {
            @Override
            public boolean test(BeanInfo beanInfo) {
                String nameWithPackage = beanInfo.getBeanClass().local();
                return nameWithPackage.contains(".jaxws_asm") || nameWithPackage.endsWith("ObjectFactory");
            }
        }));
        Set<String> extensibilities = new HashSet<>(Arrays.asList(
                "io.quarkiverse.cxf.AddressTypeExtensibility",
                "io.quarkiverse.cxf.HTTPClientPolicyExtensibility",
                "io.quarkiverse.cxf.HTTPServerPolicyExtensibility",
                "io.quarkiverse.cxf.XMLBindingMessageFormatExtensibility",
                "io.quarkiverse.cxf.XMLFormatBindingExtensibility"));
        unremovables
                .produce(new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanClassNamesExclusion(extensibilities)));
    }

    private static final String ANNOTATION_VALUE_INTERCEPTORS = "interceptors";

    class WrapperParameter {
        private Type parameterType;
        private List<AnnotationInstance> annotations;
        private String Name;

        WrapperParameter(Type parameterType, List<AnnotationInstance> annotations, String name) {
            this.parameterType = parameterType;
            this.annotations = annotations;
            Name = name;
        }

        public String getName() {
            return Name;
        }

        public List<AnnotationInstance> getAnnotations() {
            return annotations;
        }

        public AnnotationInstance getAnnotation(DotName dotname) {
            for (AnnotationInstance ai : annotations) {
                if (ai.name().equals(dotname))
                    return ai;
            }
            return null;
        }

        public Type getParameterType() {
            return parameterType;
        }
    }

    @BuildStep
    public void build(
            Capabilities capabilities,
            CombinedIndexBuildItem combinedIndexBuildItem,
            CxfBuildTimeConfig cxfBuildTimeConfig,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ServletBuildItem> servlet,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<FilterBuildItem> filters,
            BuildProducer<ServletInitParamBuildItem> servletInitParameters,
            BuildProducer<JaxbFileRootBuildItem> forceJaxb,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxies,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<CxfWebServiceBuildItem> cxfWebServices,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        IndexView index = combinedIndexBuildItem.getIndex();
        if (!capabilities.isPresent(Capability.SERVLET)) {
            LOGGER.info("CXF running without servlet container.");
            LOGGER.info("- Add quarkus-undertow to run CXF within a servlet container");
            return;
        }
        // Register package-infos for reflection
        for (AnnotationInstance xmlNamespaceInstance : index.getAnnotations(XML_NAMESPACE)) {
            reflectiveClass.produce(
                    new ReflectiveClassBuildItem(true, true, xmlNamespaceInstance.target().asClass().name().toString()));
        }

        forceJaxb.produce(new JaxbFileRootBuildItem("."));
        //TODO bad code it is set in loop but use outside...
        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);
        createNamespaceWrapper(classOutput, reflectiveClass, unremovableBeans, index);

        Set<String> generatedClass = new HashSet<>();

        boolean StartServlet = false;
        for (AnnotationInstance annotation : index.getAnnotations(WEBSERVICE_ANNOTATION)) {
            if (annotation.target().kind() != AnnotationTarget.Kind.CLASS) {
                continue;
            }

            ClassInfo wsClassInfo = annotation.target().asClass();
            reflectiveClass
                    .produce(new ReflectiveClassBuildItem(true, true, wsClassInfo.name().toString()));
            unremovableBeans.produce(new UnremovableBeanBuildItem(
                    new UnremovableBeanBuildItem.BeanClassNameExclusion(wsClassInfo.name().toString())));

            if (!Modifier.isInterface(wsClassInfo.flags())) {
                continue;
            }
            List<String> wrapperClassNames = new ArrayList<>();
            String sei = wsClassInfo.name().toString();
            Collection<ClassInfo> implementors = index.getAllKnownImplementors(DotName.createSimple(sei));
            String implementor = "";
            //TODO add soap1.2 in config file
            String soapBinding = SOAPBinding.SOAP11HTTP_BINDING;
            //if no implementor, it mean it is client
            if (implementors == null || implementors.isEmpty()) {
                String seiClientproducerClassName = sei + "CxfClientProducer";
                generateCxfClientProducer(generatedBeans, seiClientproducerClassName, sei);
                unremovableBeans.produce(new UnremovableBeanBuildItem(
                        new UnremovableBeanBuildItem.BeanClassNameExclusion(seiClientproducerClassName)));
            } else {
                StartServlet = true;
                for (ClassInfo wsClass : implementors) {
                    implementor = wsClass.name().toString();
                    AnnotationInstance bindingType = wsClass.classAnnotation(BINDING_TYPE_ANNOTATION);
                    if (bindingType != null) {
                        soapBinding = bindingType.value().asString();
                        break;
                    }
                }
            }
            //ClientProxyFactoryBean
            //proxies.produce(new NativeImageProxyDefinitionBuildItem("java.io.Closeable",
            //        "org.apache.cxf.endpoint.Client", wsClassInfo.name().toString()));
            proxies.produce(new NativeImageProxyDefinitionBuildItem(wsClassInfo.name().toString(),
                    "javax.xml.ws.BindingProvider", "java.io.Closeable", "org.apache.cxf.endpoint.Client"));

            String pkg = wsClassInfo.name().toString();
            int idx = pkg.lastIndexOf('.');
            if (idx != -1 && idx < pkg.length() - 1) {
                pkg = pkg.substring(0, idx);
            }
            AnnotationValue namespaceVal = annotation.value("targetNamespace");
            String wsNamespace = namespaceVal != null ? namespaceVal.asString() : getNamespaceFromPackage(pkg);
            String wsName = annotation.value("name") != null ? annotation.value("name").asString() : "";

            pkg = pkg + ".jaxws_asm";
            //TODO config for boolean anonymous = factory.getAnonymousWrapperTypes();
            //if (getAnonymousWrapperTypes) pkg += "_an";
            //currently package-info is not supported by gizmo so done the whole generation
            String packageName = pkg + ".package-info";

            if (!generatedClass.contains(packageName)) {
                String packagefileName = packageName.replace('.', '/');
                //https://github.com/apache/cxf/blob/master/rt/frontend/jaxws/src/main/java/org/apache/cxf/jaxws/WrapperClassGenerator.java#L234
                ClassWriter file = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                final GizmoClassVisitor cv = new GizmoClassVisitor(Gizmo.ASM_API_VERSION, file,
                        classOutput.getSourceWriter(packagefileName));
                cv.visit(Opcodes.V1_5, Opcodes.ACC_ABSTRACT + Opcodes.ACC_INTERFACE, packagefileName, null,
                        "java/lang/Object", null);

                AnnotationVisitor av = cv.visitAnnotation("Ljavax/xml/bind/annotation/XmlSchema;", true);
                av.visit("namespace", wsNamespace);
                av.visitEnum("elementFormDefault", "Ljavax/xml/bind/annotation/XmlNsForm;", "QUALIFIED");
                av.visitEnd();

                // TODO find package annotation with yandex (AnnotationTarget.Kind.package do not exists...
                // then forward value and type of XmlJavaTypeAdapter
                //            PackageInfoCreator.addAnnotation(AnnotationInstance.create(DotName.createSimple(XmlJavaTypeAdapters.class.getName()),
                //                    null, annotationValues));
                //            PackageInfoCreator.addAnnotation(AnnotationInstance.create(DotName.createSimple(XmlJavaTypeAdapter.class.getName()),
                //                    null, annotationValues));

                cv.visitEnd();
                classOutput.write(packagefileName, file.toByteArray());
                /*
                 * try (ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                 * .className(packageName)
                 * .build()) {
                 * 
                 * classCreator.addAnnotation(AnnotationInstance.create(
                 * DotName.createSimple(XmlSchema.class.getName()), null,
                 * new AnnotationValue[]{AnnotationValue.createStringValue("namespace", namespace),
                 * AnnotationValue.createStringValue("namespace", namespace)}));
                 * }
                 */
                generatedClass.add(packageName);
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, packageName));

            }
            //TODO get SOAPBINDING_ANNOTATION to get isRPC
            //@SOAPBinding(style=Style.RPC, use=Use.LITERAL, parameterStyle=ParameterStyle.BARE)
            List<MethodDescriptor> setters = new ArrayList<>();
            List<MethodDescriptor> getters = new ArrayList<>();

            for (MethodInfo mi : wsClassInfo.methods()) {
                for (Type exceptionType : mi.exceptions()) {
                    String exceptionName = exceptionType.name().withoutPackagePrefix() + "_Exception";
                    if (exceptionType.annotation(WEBFAULT_ANNOTATION) != null) {
                        exceptionName = exceptionType.annotation(WEBFAULT_ANNOTATION).value("name").asString();

                    }
                    if (!generatedClass.contains(exceptionName)) {
                        createException(classOutput, exceptionName, exceptionType.name());
                        generatedClass.add(exceptionName);
                    }
                }
                String className = StringUtils.capitalize(mi.name());
                String operationName = mi.name();
                AnnotationInstance webMethodAnnotation = mi.annotation(WEBMETHOD_ANNOTATION);
                if (webMethodAnnotation != null) {
                    AnnotationValue nameVal = webMethodAnnotation.value("operationName");
                    if (nameVal != null) {
                        operationName = nameVal.asString();
                    }
                }

                AnnotationInstance webResultAnnotation = mi.annotation(WEBRESULT_ANNOTATION);
                String resultName = "return";
                String resultNamespace = "";
                if (webResultAnnotation != null) {
                    AnnotationValue resultNameVal = webResultAnnotation.value("name");
                    AnnotationValue resultNamespaceVal = webResultAnnotation.value("targetNamespace");
                    if (resultNameVal != null) {
                        resultName = resultNameVal.asString();
                    }
                    if (resultNamespaceVal != null) {
                        resultNamespace = resultNamespaceVal.asString();
                    }
                }
                List<WrapperParameter> wrapperParams = new ArrayList<>();
                for (int i = 0; i < mi.parameters().size(); i++) {
                    Type paramType = mi.parameters().get(i);
                    String paramName = mi.parameterName(i);
                    List<AnnotationInstance> paramAnnotations = new ArrayList<>();
                    for (AnnotationInstance methodAnnotation : mi.annotations()) {
                        if (methodAnnotation.target().kind() != AnnotationTarget.Kind.METHOD_PARAMETER)
                            continue;
                        MethodParameterInfo paramInfo = methodAnnotation.target().asMethodParameter();
                        if (paramInfo != null && paramName.equals(paramInfo.name())) {
                            paramAnnotations.add(methodAnnotation);
                        }

                    }

                    wrapperParams.add(new WrapperParameter(paramType, paramAnnotations, paramName));
                }

                if (!generatedClass.contains(pkg + className)) {
                    String fullClassName = null;
                    AnnotationInstance requestWrapperAnnotation = mi.annotation(REQUEST_WRAPPER_ANNOTATION);
                    if (requestWrapperAnnotation != null) {
                        AnnotationValue classNameValue = requestWrapperAnnotation.value("className");
                        fullClassName = classNameValue.asString();
                    }
                    MethodDescriptor requestCtor;
                    String pkgWH;
                    if (fullClassName == null) {
                        requestCtor = createWrapper(true, operationName, wsNamespace, resultNamespace, resultName,
                                mi.returnType().toString(), wrapperParams,
                                classOutput, pkg, className, getters, setters);
                        fullClassName = pkg + "." + className;
                        pkgWH = pkg;
                        wrapperClassNames.add(fullClassName);
                    } else {
                        DotName fullClassDotName = DotName.createSimple(fullClassName);
                        className = fullClassDotName.withoutPackagePrefix();
                        requestCtor = MethodDescriptor.ofConstructor(fullClassName);
                        ClassInfo wrapperClass = index.getClassByName(DotName.createSimple(fullClassName));
                        pkgWH = fullClassName.substring(0, fullClassName.lastIndexOf(46));
                        if (wrapperClass == null) {
                            LOGGER.warn("wrapper class not found : " + fullClassName);
                        } else {
                            List<FieldInfo> fields = wrapperClass.fields();
                            for (FieldInfo f : fields) {
                                String fieldName = f.name();
                                getters.add(MethodDescriptor.ofMethod(fullClassName,
                                        JAXBUtils.nameToIdentifier(fieldName, JAXBUtils.IdentifierType.GETTER),
                                        f.type().name().toString()));
                                setters.add(MethodDescriptor.ofMethod(fullClassName,
                                        JAXBUtils.nameToIdentifier(fieldName, JAXBUtils.IdentifierType.SETTER),
                                        "V",
                                        f.type().name().toString()));
                            }
                        }
                    }

                    reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, fullClassName));
                    String wrapperHelperClassName = createWrapperHelper(classOutput, pkgWH, className, requestCtor, getters,
                            setters, index);
                    reflectiveClass
                            .produce(new ReflectiveClassBuildItem(true, true, wrapperHelperClassName));

                    createWrapperFactory(classOutput, pkgWH, className, requestCtor);
                    reflectiveClass
                            .produce(new ReflectiveClassBuildItem(true, true,
                                    pkgWH + "." + className + WRAPPER_FACTORY_POSTFIX));
                    getters.clear();
                    setters.clear();

                    fullClassName = null;
                    AnnotationInstance responseWrapperAnnotation = mi.annotation(RESPONSE_WRAPPER_ANNOTATION);
                    if (responseWrapperAnnotation != null) {
                        AnnotationValue classNameValue = responseWrapperAnnotation.value("className");
                        fullClassName = classNameValue.asString();
                    }
                    MethodDescriptor responseCtor;
                    String responseClassName;
                    if (fullClassName == null) {
                        responseCtor = createWrapper(false, operationName, wsNamespace, resultNamespace, resultName,
                                mi.returnType().toString(), wrapperParams,
                                classOutput, pkg, className, getters, setters);
                        responseClassName = className + RESPONSE_CLASS_POSTFIX;
                        fullClassName = pkg + "." + responseClassName;
                        pkgWH = pkg;
                        wrapperClassNames.add(fullClassName);
                    } else {
                        DotName fullClassDotName = DotName.createSimple(fullClassName);
                        responseClassName = fullClassDotName.withoutPackagePrefix();
                        responseCtor = MethodDescriptor.ofConstructor(fullClassName);
                        ClassInfo wrapperClass = index.getClassByName(DotName.createSimple(fullClassName));
                        pkgWH = fullClassName.substring(0, fullClassName.lastIndexOf(46));
                        if (wrapperClass == null) {
                            LOGGER.warn("wrapper class not found : " + fullClassName);
                        } else {
                            List<FieldInfo> fields = wrapperClass.fields();
                            for (FieldInfo f : fields) {
                                String fieldName = f.name();
                                getters.add(MethodDescriptor.ofMethod(fullClassName,
                                        JAXBUtils.nameToIdentifier(fieldName, JAXBUtils.IdentifierType.GETTER),
                                        f.type().name().toString()));
                                setters.add(MethodDescriptor.ofMethod(fullClassName,
                                        JAXBUtils.nameToIdentifier(fieldName, JAXBUtils.IdentifierType.SETTER),
                                        "V",
                                        f.type().name().toString()));
                            }
                        }
                    }
                    reflectiveClass
                            .produce(new ReflectiveClassBuildItem(true, true, fullClassName));

                    String wrapperHelperResponseClassName = createWrapperHelper(classOutput, pkgWH,
                            responseClassName, responseCtor, getters, setters, index);
                    reflectiveClass
                            .produce(new ReflectiveClassBuildItem(true, true, wrapperHelperResponseClassName));
                    createWrapperFactory(classOutput, pkgWH, responseClassName, responseCtor);

                    reflectiveClass
                            .produce(new ReflectiveClassBuildItem(true, true,
                                    pkgWH + "." + responseClassName + WRAPPER_FACTORY_POSTFIX));

                    getters.clear();
                    setters.clear();

                    reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, pkgWH + ".ObjectFactory"));

                    generatedClass.add(pkg + className);

                }

            }
            cxfWebServices
                    .produce(new CxfWebServiceBuildItem(sei, soapBinding, wsNamespace, wsName, wrapperClassNames, implementor));
            //MethodDescriptor requestCtor = createWrapper("parameters", namespace,mi.typeParameters(), classOutput, pkg, pkg+"Parameters", getters, setters);
            //createWrapperHelper(classOutput, pkg, className, requestCtor, getters, setters);
            //getters.clear();
            //setters.clear();
        }

        feature.produce(new FeatureBuildItem(FEATURE_CXF));

        //TODO ServletInitParamBuildItem
        /*
         * AbstractHTTPServlet.STATIC_RESOURCES_PARAMETER
         * AbstractHTTPServlet.STATIC_WELCOME_FILE_PARAMETER
         * AbstractHTTPServlet.STATIC_CACHE_CONTROL
         * AbstractHTTPServlet.REDIRECTS_PARAMETER
         * AbstractHTTPServlet.REDIRECT_SERVLET_NAME_PARAMETER
         * AbstractHTTPServlet.REDIRECT_SERVLET_PATH_PARAMETER
         * AbstractHTTPServlet.REDIRECT_ATTRIBUTES_PARAMETER
         * AbstractHTTPServlet.REDIRECT_QUERY_CHECK_PARAMETER
         * AbstractHTTPServlet.REDIRECT_WITH_INCLUDE_PARAMETER
         * AbstractHTTPServlet.USE_X_FORWARDED_HEADERS_PARAMETER
         * CXFNonSpringJaxrsServlet.USER_MODEL_PARAM;
         * CXFNonSpringJaxrsServlet.SERVICE_ADDRESS_PARAM;
         * CXFNonSpringJaxrsServlet.IGNORE_APP_PATH_PARAM;
         * CXFNonSpringJaxrsServlet.SERVICE_CLASSES_PARAM;
         * CXFNonSpringJaxrsServlet.PROVIDERS_PARAM;
         * CXFNonSpringJaxrsServlet.FEATURES_PARAM;
         * CXFNonSpringJaxrsServlet.OUT_INTERCEPTORS_PARAM;
         * CXFNonSpringJaxrsServlet.OUT_FAULT_INTERCEPTORS_PARAM;
         * CXFNonSpringJaxrsServlet.IN_INTERCEPTORS_PARAM;
         * CXFNonSpringJaxrsServlet.INVOKER_PARAM;
         * CXFNonSpringJaxrsServlet.SERVICE_SCOPE_PARAM;
         * CXFNonSpringJaxrsServlet.EXTENSIONS_PARAM;
         * CXFNonSpringJaxrsServlet.LANGUAGES_PARAM;
         * CXFNonSpringJaxrsServlet.PROPERTIES_PARAM;
         * CXFNonSpringJaxrsServlet.SCHEMAS_PARAM;
         * CXFNonSpringJaxrsServlet.DOC_LOCATION_PARAM;
         * CXFNonSpringJaxrsServlet.STATIC_SUB_RESOLUTION_PARAM;
         */

        if (StartServlet) {
            //if JAX-WS is installed at the root location we use a filter, otherwise we use a Servlet and take over the whole mapped path
            //            if (cxfBuildTimeConfig.path.equals("/") || cxfBuildTimeConfig.path.isEmpty()) {
            //                filters.produce(
            //                        FilterBuildItem.builder(JAX_WS_FILTER_NAME, CXFQuarkusServlet.class.getName()).setLoadOnStartup(1)
            //                                .addFilterServletNameMapping("default", DispatcherType.REQUEST).setAsyncSupported(true)
            //                                .build());
            //            } else {
            String mappingPath = getMappingPath(cxfBuildTimeConfig.path);
            servlet.produce(ServletBuildItem.builder(JAX_WS_SERVLET_NAME, CXFQuarkusServlet.class.getName())
                    .setLoadOnStartup(1).addMapping(mappingPath).setAsyncSupported(true).build());
            //            }
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, CXFQuarkusServlet.class.getName()));
        }

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

    //    @BuildStep
    //    @Record(ExecutionTime.RUNTIME_INIT)
    //    RouteBuildItem handler(LaunchModeBuildItem launch, CXFRecorder recorder, CxfBuildTimeConfig cfg, CxfConfig cxfConfig) {
    //        return new RouteBuildItem(cfg.path, new CxfHandler(), HandlerType.BLOCKING);
    //    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void runtimeConfig(CXFRecorder recorder, List<CxfWebServiceBuildItem> cxfWebServices,
            CxfConfig cxfConfig, ServletDeploymentManagerBuildItem servletDeploymentManagerBuildItem,
            //force after servlet boot
            List<ServiceStartBuildItem> servicesStated
    //to force that it is done before undertow boot
    //, BuildProducer<HttpHandlerWrapperBuildItem> wrappers
    ) {
        RuntimeValue<CXFServletInfos> infos = recorder.createInfos();
        for (CxfWebServiceBuildItem cxfWebService : cxfWebServices) {
            recorder.registerCXFServlet(infos, cxfWebService.getSei(),
                    cxfConfig, cxfWebService.getSoapBinding(), cxfWebService.getClassNames(), cxfWebService.getImplementor());
        }
        recorder.initServlet(servletDeploymentManagerBuildItem.getDeploymentManager(), infos);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void runtimeConfig(CXFRecorder recorder, CxfConfig cxfConfig, List<CxfWebServiceBuildItem> cxfWebServices,
            BuildProducer<SyntheticBeanBuildItem> synthetics) {
        for (CxfWebServiceBuildItem cxfWebService : cxfWebServices) {
            synthetics.produce(SyntheticBeanBuildItem.configure(CXFClientInfo.class).named(cxfWebService.getSei())
                    .supplier(recorder.cxfClientInfoSupplier(cxfWebService.getSei(),
                            cxfConfig,
                            cxfWebService.getSoapBinding(),
                            cxfWebService.getWsNamespace(),
                            cxfWebService.getWsName(),
                            cxfWebService.getClassNames()))
                    .unremovable()
                    .setRuntimeInit()
                    .done());
        }
    }

    @BuildStep
    BeanDefiningAnnotationBuildItem additionalBeanDefiningAnnotation() {
        return new BeanDefiningAnnotationBuildItem(WEBSERVICE_ANNOTATION);
    }

    @BuildStep
    void buildResources(BuildProducer<NativeImageResourceBuildItem> resources,
            BuildProducer<ReflectiveClassBuildItem> reflectiveItems) {
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
                    while (line != null) {
                        String[] cols = line.split(":");
                        //org.apache.cxf.bus.managers.PhaseManagerImpl:org.apache.cxf.phase.PhaseManager:true
                        if (cols.length > 1) {
                            if (cols[0] != "") {
                                reflectiveItems.produce(new ReflectiveClassBuildItem(true, true, cols[0]));
                            }
                            if (cols[1] != "") {
                                reflectiveItems.produce(new ReflectiveClassBuildItem(true, true, cols[1]));
                            }
                        }
                        line = reader.readLine();
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warn("can not open bus-extensions.txt");
        }

    }

    /**
     * Create Producer bean managing webservice client
     * <p>
     * The generated class will look like
     *
     * <pre>
     * public class FruitWebserviceCxfClientProducer extends AbstractCxfClientProducer {
     * &#64;ApplicationScoped
     * &#64;Produces
     * &#64;Default
     * public FruitWebService createService() {
     * return (FruitWebService) loadCxfClient ();
     * }
     *
     */
    private void generateCxfClientProducer(BuildProducer<GeneratedBeanBuildItem> generatedBean,
            String cxfClientProducerClassName, String sei) {
        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(generatedBean);

        try (ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .className(cxfClientProducerClassName)
                .superClass(CxfClientProducer.class)
                .build()) {
            classCreator.addAnnotation(ApplicationScoped.class);

            FieldCreator fieldCreator = classCreator.getFieldCreator("info", "io.quarkiverse.cxf.CXFClientInfo")
                    .setModifiers(Modifier.PUBLIC);

            fieldCreator.addAnnotation(AnnotationInstance.create(DotNames.NAMED, null, new AnnotationValue[] {
                    AnnotationValue.createStringValue("value", sei)
            }));

            fieldCreator.addAnnotation(
                    AnnotationInstance.create(DotName.createSimple(Inject.class.getName()), null, new AnnotationValue[] {}));
            try (MethodCreator getInfoMethodCreator = classCreator.getMethodCreator("getInfo",
                    "io.quarkiverse.cxf.CXFClientInfo")) {
                getInfoMethodCreator.setModifiers(Modifier.PUBLIC);
                getInfoMethodCreator.returnValue(getInfoMethodCreator.readInstanceField(fieldCreator.getFieldDescriptor(),
                        getInfoMethodCreator.getThis()));
            }
            try (MethodCreator cxfClientMethodCreator = classCreator.getMethodCreator("createService", sei)) {
                cxfClientMethodCreator.addAnnotation(ApplicationScoped.class);
                cxfClientMethodCreator.addAnnotation(Produces.class);
                cxfClientMethodCreator.addAnnotation(Default.class);

                // New configuration
                ResultHandle cxfClient = cxfClientMethodCreator.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(CxfClientProducer.class,
                                "loadCxfClient",
                                Object.class),
                        cxfClientMethodCreator.getThis());
                ResultHandle cxfClientCasted = cxfClientMethodCreator.checkCast(cxfClient, sei);
                cxfClientMethodCreator.returnValue(cxfClientCasted);
            }
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
                new RuntimeInitializedClassBuildItem("org.apache.cxf.staxutils.validation.W3CMultiSchemaFactory"));
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
                "io.quarkiverse.cxf.QuarkusJAXBBeanInfo",
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
                "io.quarkiverse.cxf.AddressTypeExtensibility",
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
        reflectiveItems.produce(new ReflectiveClassBuildItem(false, false,
                //manually added
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
                "com.oracle.xmlns.webservices.jaxws_databinding.ObjectFactory",
                "com.sun.org.apache.xerces.internal.utils.XMLSecurityManager",
                "com.sun.org.apache.xerces.internal.utils.XMLSecurityPropertyManager",
                "com.sun.xml.bind.api.TypeReference",
                "com.sun.xml.bind.DatatypeConverterImpl",
                "com.sun.xml.internal.bind.api.TypeReference",
                "com.sun.xml.internal.bind.DatatypeConverterImpl",
                "com.sun.xml.ws.runtime.config.ObjectFactory",
                "ibm.wsdl.DefinitionImpl",
                "io.swagger.jaxrs.DefaultParameterExtension",
                "io.undertow.server.HttpServerExchange",
                "io.undertow.UndertowOptions",
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
                "javax.wsdl.Binding",
                "javax.wsdl.BindingFault",
                "javax.wsdl.BindingFault",
                "javax.wsdl.BindingInput",
                "javax.wsdl.BindingOperation",
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
                "org.apache.cxf.bindings.xformat.ObjectFactory",
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
                "org.apache.cxf.transports.http.configuration.ObjectFactory",
                "org.apache.cxf.ws.addressing.wsdl.AttributedQNameType",
                "org.apache.cxf.ws.addressing.wsdl.ObjectFactory",
                "org.apache.cxf.ws.addressing.wsdl.ServiceNameType",
                "org.apache.cxf.wsdl.http.AddressType",
                "org.apache.cxf.wsdl.http.ObjectFactory",
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

}
