package io.quarkiverse.cxf.deployment;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.jws.WebService;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.spi.GeneratedClassClassLoaderCapture;
import org.apache.cxf.tools.java2ws.JavaToWS;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkiverse.cxf.deployment.CxfBuildTimeConfig.Java2WsParameterSet;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.paths.PathFilter;

class Java2WsdlProcessor {

    private static final Logger log = Logger.getLogger(Java2WsdlProcessor.class);

    /*
     * We consume GeneratedBeanBuildItem to ensure that this method is executed after
     * QuarkusCxfProcessor.generateClasses(). Otherwise the wrapper classes might be generated earlier here in this
     * method without capturing to a GeneratedBeanBuildItem. This is because QuarkusCxfProcessor.generateClasses()
     * does not generate classes that are loaded already.
     */
    @Consume(GeneratedBeanBuildItem.class)
    @BuildStep
    void java2wsdl(
            CxfBuildTimeConfig cxfBuildTimeConfig,
            CombinedIndexBuildItem combinedIndex,
            OutputTargetBuildItem outputTargetBuildItem,
            BuildProducer<GeneratedResourceBuildItem> dummy /*
                                                             * Force Quarkus build time container to call this method
                                                             * even though it does not produce any BuildItems
                                                             */) {

        final String targetDir = outputTargetBuildItem.getOutputDirectory().toString();
        final String classesDir = outputTargetBuildItem.getOutputDirectory().resolve("classes").toString();

        final IndexView index = combinedIndex.getIndex();
        if (!cxfBuildTimeConfig.java2ws().enabled()) {
            log.info("Skipping " + this.getClass() + " invocation on user's request");
            return;
        }

        final Set<DotName> services = index.getAnnotations(DotName.createSimple(WebService.class.getName()))
                .stream()
                .map(AnnotationInstance::target)
                .filter(annotationTarget -> annotationTarget.kind() == AnnotationTarget.Kind.CLASS)
                .map(AnnotationTarget::asClass)
                .map(classInfo -> classInfo.name())
                .collect(Collectors.toCollection(TreeSet::new));

        final Bus oldBus = BusFactory.getThreadDefaultBus();
        final Bus java2wsdlBus = BusFactory.newInstance().createBus();
        java2wsdlBus.setExtension(null, GeneratedClassClassLoaderCapture.class);
        BusFactory.setThreadDefaultBus(java2wsdlBus);
        try {
            final Map<String, String> processedClasses = new LinkedHashMap<>();
            java2wsdl(
                    services,
                    cxfBuildTimeConfig.java2ws().rootParameterSet(),
                    Java2WsParameterSet.JAVA2WS_CONFIG_KEY_PREFIX,
                    targetDir,
                    classesDir,
                    processedClasses);

            // named
            for (Entry<String, Java2WsParameterSet> en : cxfBuildTimeConfig.java2ws().namedParameterSets().entrySet()) {
                java2wsdl(
                        services,
                        en.getValue(),
                        Java2WsParameterSet.JAVA2WS_CONFIG_KEY_PREFIX + "." + en.getKey(),
                        targetDir,
                        classesDir,
                        processedClasses);
            }
            log.infof("java2ws processed %d classes", processedClasses.size());
        } finally {
            BusFactory.setThreadDefaultBus(oldBus);
        }
    }

    static void java2wsdl(
            Set<DotName> serviceClasses,
            CxfBuildTimeConfig.Java2WsParameterSet params,
            String prefix,
            String targetDir,
            String classesDir,
            Map<String, String> processedClasses) {
        params.validate(prefix);
        if (params.includes().isEmpty()) {
            /* Nothing to do */
            return;
        }
        final String selectors = "    " + prefix + ".includes = " + params.includes().get() +
                (params.excludes().isPresent()
                        ? "\n    " + prefix + ".excludes = " + params.excludes().get()
                        : "");

        final PathFilter pathFilter = new PathFilter(toSlashNames(params.includes()), toSlashNames(params.excludes()));

        for (DotName serviceClass : serviceClasses) {
            if (pathFilter.isVisible(serviceClass.toString('/'))) {
                final String oldSelectors = processedClasses.get(serviceClass.toString());
                if (oldSelectors != null) {
                    throw new IllegalStateException("Service class " + serviceClass + " was already selected by\n\n"
                            + oldSelectors
                            + "\n\nand therefore it cannot once again be selected by\n\n" + selectors
                            + "\n\nEnsure that the individual include/exclude sets are mutually exclusive.");
                }

                processedClasses.put(serviceClass.toString(), selectors);
                final Java2WsdlParams java2WsdlParams = new Java2WsdlParams(
                        serviceClass.toString(),
                        params.additionalParams().orElse(Collections.emptyList()),
                        generateWsdlName(params.wsdlNameTemplate(), serviceClass, targetDir, classesDir));
                if (log.isInfoEnabled()) {
                    log.info(java2WsdlParams.appendLog(new StringBuilder("Running java2ws")).toString());
                }

                try {
                    new JavaToWS(java2WsdlParams.toParameterArray()).run();
                } catch (Exception e) {
                    throw new RuntimeException(
                            java2WsdlParams.appendLog(new StringBuilder("Could not run java2ws")).toString(),
                            e);
                }
            }
        }
    }

    static List<String> toSlashNames(Optional<List<String>> includes) {
        if (includes.isPresent()) {
            return includes.get().stream().map(incl -> incl.replace('.', '/')).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    static String generateWsdlName(String template, DotName serviceClass, String targetDir, String classesDir) {
        return template
                .replace("%SIMPLE_CLASS_NAME%", serviceClass.withoutPackagePrefix())
                .replace("%FULLY_QUALIFIED_CLASS_NAME%", serviceClass.toString().replace('.', '_'))
                .replace("%TARGET_DIR%", targetDir)
                .replace("%CLASSES_DIR%", classesDir);
    }

    static class Java2WsdlParams {
        private final String serviceClassName;
        private final List<String> additionalParams;
        private final String outputPath;

        Java2WsdlParams(String serviceClassName, List<String> additionalParams, String outputPath) {
            super();
            this.serviceClassName = serviceClassName;
            this.additionalParams = additionalParams;
            this.outputPath = outputPath;
        }

        StringBuilder appendLog(StringBuilder sb) {
            render(value -> sb.append(' ').append(value));
            return sb;
        }

        String[] toParameterArray() {
            final String[] result = new String[additionalParams.size() + 4];
            final AtomicInteger i = new AtomicInteger(0);
            render(value -> result[i.getAndIncrement()] = value);
            return result;
        }

        void render(Consumer<String> paramConsumer) {
            paramConsumer.accept("-wsdl");
            paramConsumer.accept("-o");
            paramConsumer.accept(outputPath);
            additionalParams.forEach(paramConsumer);
            paramConsumer.accept(serviceClassName);
        }

    }

}
