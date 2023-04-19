package io.quarkiverse.cxf.deployment.codegen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.wsdlto.WSDLToJava;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import io.quarkiverse.cxf.deployment.CxfBuildTimeConfig.Wsdl2JavaParameterSet;
import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.deployment.CodeGenContext;
import io.quarkus.deployment.CodeGenProvider;
import io.quarkus.paths.DirectoryPathTree;
import io.quarkus.paths.PathFilter;

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
        final Config config = context.config();
        if (!config.getOptionalValue("quarkus.cxf.codegen.wsdl2java.enabled", Boolean.class).orElse(true)) {
            log.info("Skipping " + this.getClass() + " invocation on user's request");
            return false;
        }

        final Path outDir = context.outDir();

        final Function<String, Optional<List<String>>> configFunction = key -> config.getOptionalValues(key, String.class);
        final Wsdl2JavaParameterSet rootParams = buildParameterSet(configFunction, WSDL2JAVA_CONFIG_KEY_PREFIX);
        final Map<String, String> processedFiles = new HashMap<>();
        boolean result = false;
        result |= wsdl2java(context.inputDir(), rootParams, outDir, WSDL2JAVA_CONFIG_KEY_PREFIX, processedFiles);

        final Set<String> names = findParamSetNames(config.getPropertyNames());
        for (String name : names) {
            final String prefix = WSDL2JAVA_NAMED_CONFIG_KEY_PREFIX + name;
            final Wsdl2JavaParameterSet namedParams = buildParameterSet(configFunction, prefix);
            result |= wsdl2java(context.inputDir(), namedParams, outDir, prefix, processedFiles);
        }

        if (!result) {
            log.infof(
                    "wsdl2java processed 0 WSDL files under %s",
                    absModuleRoot(context.inputDir()).relativize(context.inputDir()));
        }
        return result;
    }

    static boolean wsdl2java(Path inputDir, Wsdl2JavaParameterSet params, Path outDir, String prefix,
            Map<String, String> processedFiles) {

        return scan(inputDir, params.includes, params.excludes, prefix, processedFiles, (Path wsdlFile) -> {
            final Wsdl2JavaParams wsdl2JavaParams = new Wsdl2JavaParams(inputDir, outDir, wsdlFile,
                    params.additionalParams.orElse(Collections.emptyList()));
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
                        + "\n\nPlease make sure that the individual include/exclude sets are mutually exclusive.");
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

    static Set<String> findParamSetNames(Iterable<String> propertyNames) {
        final Set<String> result = new TreeSet<>();
        for (String key : propertyNames) {
            if (key.startsWith(WSDL2JAVA_NAMED_CONFIG_KEY_PREFIX)) {
                Stream.of(".includes", ".excludes", ".additional-params")
                        .filter(suffix -> key.endsWith(suffix))
                        .findFirst()
                        .ifPresent(suffix -> {
                            if (WSDL2JAVA_NAMED_CONFIG_KEY_PREFIX.length() + suffix.length() < key.length()) {
                                /* this is a named param set key */
                                final String name = key.substring(WSDL2JAVA_NAMED_CONFIG_KEY_PREFIX.length(),
                                        key.length() - suffix.length());
                                result.add(name);
                            }
                        });
            }
        }
        return result;
    }

    /**
     * An inelegant way to assemble {@link Wsdl2JavaParameterSet} out of {@link Config} as long as something like
     * {@code config.getValue("quarkus.cxf.codegen.wsdl2java", Wsdl2JavaParameterSet.class)} is not supported.
     * See also
     * <a href="https://github.com/quarkusio/quarkus/issues/31783">https://github.com/quarkusio/quarkus/issues/31783</a>.
     *
     * @param config an abstraction of {@link Config#getOptionalValues(String, Class)} for easier testing
     * @param prefix the prefix of configuration keys from which we build the resulting {@link Wsdl2JavaParameterSet}
     * @return a new {@link Wsdl2JavaParameterSet}
     */
    static Wsdl2JavaParameterSet buildParameterSet(Function<String, Optional<List<String>>> config, String prefix) {
        final Wsdl2JavaParameterSet result = new Wsdl2JavaParameterSet();

        final Optional<List<String>> maybeIncludes = config.apply(prefix + ".includes");
        final List<String> includes;
        if (maybeIncludes.isPresent()) {
            includes = maybeIncludes.get();
        } else {
            includes = null;
        }

        final Optional<List<String>> excludes = config.apply(prefix + ".excludes");
        final Optional<List<String>> additionalParams = config.apply(prefix + ".additional-params");

        if (includes == null && (excludes.isPresent() || additionalParams.isPresent())) {
            throw new IllegalStateException("Incomplete configuration: you must set " + prefix + ".includes if you set"
                    + " any of " + prefix + ".excludes or " + prefix + ".additional-params");
        }

        result.includes = Optional.ofNullable(includes);
        result.excludes = excludes;
        result.additionalParams = additionalParams;

        return result;
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
        private final Path inputDir;
        private final Path outDir;
        private final Path wsdlFile;
        private final List<String> additionalParams;

        public Wsdl2JavaParams(Path inputDir, Path outDir, Path wsdlFile, List<String> additionalParams) {
            super();
            this.inputDir = inputDir;
            this.outDir = outDir;
            this.wsdlFile = wsdlFile;
            this.additionalParams = additionalParams;
        }

        public StringBuilder appendLog(StringBuilder sb) {
            final Path moduleRoot = absModuleRoot(inputDir);
            render(path -> moduleRoot.relativize(path).toString(), value -> sb.append(' ').append(value));
            return sb;
        }

        public String[] toParameterArray() {
            final String[] result = new String[additionalParams.size() + 3];
            final AtomicInteger i = new AtomicInteger(0);
            render(Path::toString, value -> result[i.getAndIncrement()] = value);
            return result;
        }

        void render(Function<Path, String> pathTransformer, Consumer<String> paramConsumer) {
            paramConsumer.accept("-d");
            paramConsumer.accept(pathTransformer.apply(outDir));
            additionalParams.forEach(paramConsumer);
            paramConsumer.accept(pathTransformer.apply(wsdlFile));
        }

    }

}
