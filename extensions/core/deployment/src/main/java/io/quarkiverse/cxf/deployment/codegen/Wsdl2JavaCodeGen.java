package io.quarkiverse.cxf.deployment.codegen;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.wsdlto.WSDLToJava;
import org.jboss.logging.Logger;

import io.quarkiverse.cxf.deployment.CxfBuildTimeConfig;
import io.quarkiverse.cxf.deployment.CxfBuildTimeConfig.Wsdl2JavaConfig;
import io.quarkiverse.cxf.deployment.CxfBuildTimeConfig.Wsdl2JavaParameterSet;
import io.quarkiverse.cxf.deployment.codegen.Wsdl2JavaParam.Wsdl2JavaParamCollection;
import io.quarkiverse.cxf.deployment.codegen.Wsdl2JavaParam.Wsdl2JavaParamTransformer;
import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.deployment.CodeGenContext;
import io.quarkus.deployment.CodeGenProvider;
import io.quarkus.paths.DirectoryPathTree;
import io.quarkus.paths.PathFilter;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.WithDefault;

/**
 * Generates Java classes out of WSDL files using CXF {@code wsdl2Java} tool.
 * The WSDL files have to be located under {@code src/main/resources} or {@code src/test/resources}.
 * Additional parameters for {@code wsdl2Java} can be passed via {@code application.properties} - see the configuration
 * classes linked below:
 *
 * @see io.quarkiverse.cxf.deployment.CxfBuildTimeConfig#codegen
 * @see io.quarkiverse.cxf.deployment.CxfBuildTimeConfig.Wsdl2JavaConfig
 * @see io.quarkiverse.cxf.deployment.CxfBuildTimeConfig.Wsdl2JavaParameterSet
 */
public class Wsdl2JavaCodeGen implements CodeGenProvider {
    private static final Logger log = Logger.getLogger(Wsdl2JavaCodeGen.class);
    public static final String WSDL2JAVA_NAMED_CONFIG_KEY_PREFIX = "quarkus.cxf.codegen.wsdl2java.";
    public static final String WSDL2JAVA_CONFIG_KEY_PREFIX = "quarkus.cxf.codegen.wsdl2java";
    private static final Path SRC_MAIN_RESOURCES = Paths.get("src/main/resources");
    private static final Path SRC_TEST_RESOURCES = Paths.get("src/test/resources");

    @Override
    public String providerId() {
        return "wsdl2java";
    }

    @Override
    public String inputExtension() {
        return "wsdl";
    }

    @Override
    public String inputDirectory() {
        return "resources";
    }

    @Override
    public boolean trigger(CodeGenContext context) throws CodeGenException {
        final Wsdl2JavaConfig config = context.config().unwrap(SmallRyeConfig.class)
                .getConfigMapping(CxfBuildTimeConfig.class)
                .codegen().wsdl2java();

        if (!config.enabled()) {
            log.info("Skipping " + this.getClass() + " invocation on user's request");
            return false;
        }

        final Path outDir = context.outDir();

        final Wsdl2JavaParameterSet rootParams = config.rootParameterSet();
        final Map<String, String> processedFiles = new HashMap<>();
        boolean result = false;

        /*
         * TODO: this is a workaround for https://github.com/quarkusio/quarkus/issues/34422
         * While context.workDir() returns target or any other direct subdirectory of the project directory
         * then this workaround will work. But it may fail as long as the project has configured some non-standard
         * build directory.
         */
        final Path projectDir = context.workDir().getParent();
        result |= wsdl2java(projectDir, context.inputDir(), rootParams, outDir, WSDL2JAVA_CONFIG_KEY_PREFIX,
                processedFiles);

        for (Entry<String, Wsdl2JavaParameterSet> en : config.namedParameterSets().entrySet()) {
            final String prefix = WSDL2JAVA_NAMED_CONFIG_KEY_PREFIX + en.getKey();
            final Wsdl2JavaParameterSet namedParams = en.getValue();
            result |= wsdl2java(projectDir, context.inputDir(), namedParams, outDir, prefix, processedFiles);
        }

        if (!result) {
            log.infof(
                    "wsdl2java processed 0 WSDL files under %s",
                    absModuleRoot(context.inputDir()).relativize(context.inputDir()));
        }
        return result;
    }

    static boolean wsdl2java(Path projectDir, Path inputDir, Wsdl2JavaParameterSet params, Path defaultOutDir, String prefix,
            Map<String, String> processedFiles) {

        return scan(inputDir, params.includes(), params.excludes(), prefix, processedFiles, (Path wsdlFile) -> {
            final Wsdl2JavaParams wsdl2JavaParams = new Wsdl2JavaParams(
                    projectDir,
                    defaultOutDir, wsdlFile, params);
            if (log.isInfoEnabled()) {
                log.info(wsdl2JavaParams.appendLog(new StringBuilder("Running wsdl2java")).toString());
            }
            final ToolContext ctx = new ToolContext();
            try {
                new WSDLToJava(wsdl2JavaParams.toParameterArray()).run(ctx);
            } catch (Exception e) {
                throw new RuntimeException(wsdl2JavaParams.appendLog(new StringBuilder("Could not run wsdl2Java")).toString(),
                        e);
            }
        });
    }

    public static boolean scan(
            Path inputDir,
            Optional<List<String>> includes,
            Optional<List<String>> excludes,
            String prefix,
            Map<String, String> processedFiles,
            Consumer<Path> wsdlFileConsumer) {

        if (includes.isEmpty()) {
            return false;
        }

        final String selectors = "    " + prefix + ".includes = "
                + includes.get().stream().collect(Collectors.joining(","))
                + (excludes.isPresent()
                        ? "\n    " + prefix + ".excludes = " + excludes.get().stream().collect(Collectors.joining(","))
                        : "");

        final Consumer<Path> chainedConsumer = wsdlFile -> {
            final String oldSelectors = processedFiles.get(wsdlFile.toString());
            if (oldSelectors != null) {
                throw new IllegalStateException("WSDL file " + wsdlFile + " was already selected by\n\n"
                        + oldSelectors
                        + "\n\nand therefore it cannot once again be selected by\n\n" + selectors
                        + "\n\nEnsure that the individual include/exclude sets are mutually exclusive.");
            }
            processedFiles.put(wsdlFile.toString(), selectors);
            wsdlFileConsumer.accept(wsdlFile);
        };

        try (DirectoryPathTree pathTree = new DirectoryPathTree(inputDir,
                new PathFilter(includes.orElse(null), excludes.orElse(null)))) {
            pathTree.walk(pathVisit -> {
                Path path = pathVisit.getPath();
                if (Files.isRegularFile(path)) {
                    chainedConsumer.accept(path);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Could not walk directory " + inputDir, e);
        }

        return !processedFiles.isEmpty();
    }

    static Path absModuleRoot(final Path inputDir) {
        if (inputDir.endsWith(SRC_MAIN_RESOURCES) || inputDir.endsWith(SRC_TEST_RESOURCES)) {
            return inputDir.getParent().getParent().getParent();
        } else {
            throw new IllegalStateException(
                    "inputDir '" + inputDir + "' expected to end with " + SRC_MAIN_RESOURCES + " or " + SRC_TEST_RESOURCES);
        }
    }

    static class Wsdl2JavaParams {
        private final Path projectDir;
        private final Path defaultOutDir;
        private final Path wsdlFile;
        private final Wsdl2JavaParameterSet params;

        public Wsdl2JavaParams(Path projectDir, Path defaultOutDir, Path wsdlFile, Wsdl2JavaParameterSet params) {
            super();
            this.projectDir = projectDir;
            this.defaultOutDir = defaultOutDir;
            this.wsdlFile = wsdlFile;
            this.params = params;
        }

        /* A fix for https://github.com/quarkiverse/quarkus-cxf/issues/907 */
        static String absolutizeBindings(Path projectDir, String rawBindingFile) {
            Path bindingPath = Paths.get(rawBindingFile);
            if (!bindingPath.isAbsolute()) {
                /* A fix for https://github.com/quarkiverse/quarkus-cxf/issues/907 */
                return projectDir.resolve(bindingPath).toString();
            } else {
                return rawBindingFile;
            }
        }

        static List<String> absolutizeBindings(Path projectDir, List<String> additionalParams) {
            List<String> result = new ArrayList<>(additionalParams);
            ListIterator<String> it = result.listIterator();
            while (it.hasNext()) {
                String val = it.next();
                if ("-b".equals(val) && it.hasNext()) {
                    it.set(absolutizeBindings(projectDir, it.next()));
                }
            }
            return result;
        }

        public StringBuilder appendLog(StringBuilder sb) {
            // final Path moduleRoot = absModuleRoot(inputDir);
            // render(path -> moduleRoot.relativize(path).toString(), value -> sb.append(' ').append(value));
            render(Path::toString, value -> sb.append(' ').append(value));
            return sb;
        }

        public String[] toParameterArray() {
            final List<String> result = new ArrayList<>();
            render(Path::toString, result::add);
            return result.toArray(new String[0]);
        }

        void render(Function<Path, String> pathTransformer, Consumer<String> paramConsumer) {
            paramConsumer.accept("-d");
            final Optional<String> outputDirectory = params.outputDirectory();
            paramConsumer.accept(
                    pathTransformer.apply(
                            outputDirectory.isEmpty()
                                    ? defaultOutDir
                                    : projectDir.resolve(outputDirectory.get())));

            Stream.of(Wsdl2JavaParameterSet.class.getDeclaredMethods())
                    .sorted(Comparator.comparing(Method::getName))
                    .forEach(method -> {
                        final Wsdl2JavaParam wsdl2JavaParam = method.getAnnotation(Wsdl2JavaParam.class);
                        final WithDefault withDefault = method.getAnnotation(WithDefault.class);
                        if (wsdl2JavaParam != null) {
                            final String paramName = wsdl2JavaParam.value();
                            try {
                                final Object value = params.getClass().getDeclaredMethod(method.getName()).invoke(params);
                                if (value instanceof Optional) {
                                    if (((Optional<?>) value).isPresent()) {
                                        final Object optValue = ((Optional<?>) value).get();
                                        if (optValue instanceof Collection) {
                                            renderCollection(paramName, (Collection<?>) optValue, wsdl2JavaParam, paramConsumer,
                                                    projectDir);
                                        } else {
                                            renderSingle(paramName, optValue, wsdl2JavaParam, null, paramConsumer);
                                        }
                                    }
                                } else if (value instanceof Collection) {
                                    renderCollection(paramName, (Collection<?>) value, wsdl2JavaParam, paramConsumer,
                                            projectDir);
                                } else {
                                    renderSingle(paramName, value, wsdl2JavaParam, withDefault, paramConsumer);
                                }
                            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                                    | NoSuchMethodException | SecurityException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
            params.additionalParams().ifPresent(vals -> absolutizeBindings(projectDir, vals).forEach(paramConsumer));
            paramConsumer.accept(pathTransformer.apply(wsdlFile));
        }

        static void renderSingle(String paramName, Object value, Wsdl2JavaParam wsdl2JavaParam,
                WithDefault withDefault, Consumer<String> paramConsumer) {
            switch (wsdl2JavaParam.transformer()) {
                case bool:
                    System.out.println("bool " + value + " " + value.getClass().getName());
                    if (paramName != null && Boolean.TRUE.equals(value)) {
                        paramConsumer.accept(paramName);
                    }
                    break;
                case toString:
                    final String stringValue = value.toString();
                    if (withDefault == null || !stringValue.equals(withDefault.value())) {
                        if (paramName != null) {
                            paramConsumer.accept(paramName);
                        }
                        paramConsumer.accept(stringValue);
                    }
                    break;
                default:
                    throw new IllegalStateException(
                            "Unexpected " + Wsdl2JavaParamTransformer.class.getName() + ": " + wsdl2JavaParam.transformer());
            }
        }

        static void renderCollection(
                String paramName,
                Collection<?> collection,
                Wsdl2JavaParam wsdl2JavaParam,
                Consumer<String> paramConsumer,
                Path projectDir) {
            switch (wsdl2JavaParam.collection()) {
                case commaSeparated:
                    paramConsumer.accept(paramName);
                    boolean first = true;
                    final StringBuilder sb = new StringBuilder();
                    for (Object value : collection) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(',');
                        }
                        renderSingle(null, value, wsdl2JavaParam, null, sb::append);
                    }
                    paramConsumer.accept(sb.toString());
                    break;
                case multiParam:
                    for (Object value : collection) {
                        paramConsumer.accept(paramName);
                        if (paramName.equals("-b")) {
                            value = absolutizeBindings(projectDir, (String) value);
                        }
                        renderSingle(null, value, wsdl2JavaParam, null, paramConsumer);
                    }
                    break;
                case xjc:
                    for (Object value : collection) {
                        paramConsumer.accept(paramName + "-X" + value);
                    }
                    break;
                default:
                    throw new IllegalStateException(
                            "Unexpected " + Wsdl2JavaParamCollection.class.getName() + ": " + wsdl2JavaParam.collection());
            }
        }

    }

}
