package io.quarkiverse.cxf.deployment;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import jakarta.jws.WebService;

import org.apache.cxf.tools.java2ws.JavaToWS;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;

class Java2WsdlProcessor {

    private static final Logger log = Logger.getLogger(Java2WsdlProcessor.class);
    public static final String JAVA2WSDL_CONFIG_KEY_PREFIX = "quarkus.cxf.codegen.java2wsdl";

    @BuildStep
    void java2wsdl(CxfBuildTimeConfig cxfBuildTimeConfig, CombinedIndexBuildItem combinedIndex,
            OutputTargetBuildItem outputTargetBuildItem,
            //todo if reflectiveClassBuildItem producer is not here, step is ignored because it doesn't produce anything
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        String defaultOutputDir = outputTargetBuildItem.getOutputDirectory().resolve("classes").resolve("wsdl").toString();
        IndexView index = combinedIndex.getIndex();

        if (!cxfBuildTimeConfig.java2ws.enabled) {
            log.info("Skipping " + this.getClass() + " invocation on user's request");
            return;
        }

        String[] services = index.getAnnotations(DotName.createSimple(WebService.class.getName()))
                .stream()
                .map(AnnotationInstance::target)
                .map(annotationTarget -> {
                    if (annotationTarget.kind().equals(AnnotationTarget.Kind.CLASS)) {
                        return annotationTarget.asClass();
                    }
                    return null;
                })
                .filter(ci -> ci != null)
                .map(classInfo -> classInfo.name().toString())
                .toArray(String[]::new);

        final Map<String, String> processedClasses = new HashMap<>();
        boolean result = false;
        //        //root is applied only if there is no namedParameters or if it contains at least one property
        if (cxfBuildTimeConfig.java2ws.namedParameterSets.isEmpty()) {
            result |= java2wsdl(services, cxfBuildTimeConfig.java2ws.rootParameterSet,
                    JAVA2WSDL_CONFIG_KEY_PREFIX, processedClasses, defaultOutputDir, true);
        }

        //named
        final Set<String> names = cxfBuildTimeConfig.java2ws.namedParameterSets.keySet();
        for (String name : names) {
            CxfBuildTimeConfig.Java2WsParameterSet params = cxfBuildTimeConfig.java2ws.namedParameterSets.get(name);
            result |= java2wsdl(services, params, JAVA2WSDL_CONFIG_KEY_PREFIX + "." + name, processedClasses, defaultOutputDir,
                    false);
        }

        if (!result) {
            log.warn("java2wsdl processed 0 classes");
        }
    }

    static boolean java2wsdl(String[] serviceClasses, CxfBuildTimeConfig.Java2WsParameterSet params,
            String prefix,
            Map<String, String> processedClasses,
            String defaultLOutputDir,
            boolean useDefaultIncludes) {

        return scan(serviceClasses,
                params.include.orElse(useDefaultIncludes ? CxfBuildTimeConfig.Java2WsParameterSet.DEFAULT_INCLUDES : null),
                params.exclude, prefix, processedClasses, (String serviceClass) -> {
                    final Java2WsdlParams java2WsdlParams = new Java2WsdlParams(serviceClass,
                            params.outputDir.orElse(defaultLOutputDir),
                            params.additionalParams.orElse(Collections.emptyList()),
                            params.wsdlNameTemplate.orElse(CxfBuildTimeConfig.Java2WsParameterSet.DEFAULT_WSDL_NAME_TEMPLATE));
                    if (log.isInfoEnabled()) {
                        log.info(java2WsdlParams.appendLog(new StringBuilder("Running wsdl2java")).toString());
                    }
                    try {
                        new JavaToWS(java2WsdlParams.toParameterArray()).run();
                    } catch (Exception e) {
                        throw new RuntimeException(
                                java2WsdlParams.appendLog(new StringBuilder("Could not run wsdl2Java")).toString(),
                                e);
                    }
                });
    }

    static boolean scan(
            String[] classes,
            String includes,
            Optional<String> excludes,
            String prefix,
            Map<String, String> processedClasses,
            Consumer<String> serviceClassConsumer) {

        final String selectors = "    " + prefix + ".includes = " + includes +
                (excludes.isPresent()
                        ? "\n    " + prefix + ".excludes = " + excludes.get()
                        : "");

        final Consumer<String> chainedConsumer = serviceClass -> {
            final String oldSelectors = processedClasses.get(serviceClass);
            if (oldSelectors != null) {
                throw new IllegalStateException("Service class " + serviceClass + " was already selected by\n\n"
                        + oldSelectors
                        + "\n\nand therefore it cannot once again be selected by\n\n" + selectors
                        + "\n\nPlease make sure that the individual include/exclude sets are mutually exclusive.");
            }
            processedClasses.put(serviceClass, selectors);
            serviceClassConsumer.accept(serviceClass);
        };

        Pattern includePatter = Pattern.compile(includes, Pattern.CASE_INSENSITIVE);
        Optional<Pattern> excludePatter = excludes.map(p -> Pattern.compile(p, Pattern.CASE_INSENSITIVE));

        Arrays.stream(classes)
                .filter(cl -> includePatter.matcher(cl).matches())
                .filter(cl -> excludePatter.isEmpty() || !excludePatter.get().matcher(cl).matches())
                .forEach(chainedConsumer::accept);

        return !processedClasses.isEmpty();
    }

    static class Java2WsdlParams {
        private final String inputClass;
        private final String outDir;
        private final List<String> additionalParams;
        private final String wsdlNameTemplate;

        Java2WsdlParams(String inputClass, String outDir, List<String> additionalParams, String wsdlNameTemplate) {
            super();
            this.inputClass = inputClass;
            this.outDir = outDir;
            this.additionalParams = additionalParams;
            this.wsdlNameTemplate = wsdlNameTemplate;
        }

        StringBuilder appendLog(StringBuilder sb) {
            render(value -> sb.append(' ').append(value));
            return sb;
        }

        String[] toParameterArray() {
            final String[] result = new String[additionalParams.size() + 6];
            final AtomicInteger i = new AtomicInteger(0);
            render(value -> result[i.getAndIncrement()] = value);
            return result;
        }

        void render(Consumer<String> paramConsumer) {
            paramConsumer.accept("-wsdl");
            paramConsumer.accept("-o");
            paramConsumer.accept(generateWsdlName());
            paramConsumer.accept("-d");
            paramConsumer.accept(outDir.toString());
            additionalParams.forEach(paramConsumer);
            paramConsumer.accept(inputClass);
        }

        String generateWsdlName() {
            try {
                return wsdlNameTemplate
                        .replaceAll("<CLASS_NAME>", Class.forName(inputClass).getSimpleName())
                        .replaceAll("<FULLY_QUALIFIED_CLASS_NAME>", Class.forName(inputClass).getName().replace('.', '_'));
            } catch (ClassNotFoundException e) {
                //can not happen,because class is loaded from the index
                throw new RuntimeException(String.format("Class '%s' can not be found. Should not happen.", inputClass));
            }

        }

    }

}
