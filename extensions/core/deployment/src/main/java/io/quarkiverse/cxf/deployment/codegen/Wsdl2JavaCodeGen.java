package io.quarkiverse.cxf.deployment.codegen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
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
import io.quarkus.util.GlobUtil;

/**
 * Generates Java classes out of WSDL files using CXF {@code wsdl2Java} tool.
 * The WSDL files have to be located under {@code src/main/wsdl} or {@code src/test/wsdl}.
 * Additional parameters for {@code wsdl2Java} can be passed via {@code application.properties} - see the configuration
 * classes linked below:
 *
 * @see io.quarkiverse.cxf.deployment.CxfBuildTimeConfig#codegen
 * @see io.quarkiverse.cxf.deployment.CxfBuildTimeConfig.Wsdl2JavaConfig
 * @see io.quarkiverse.cxf.deployment.CxfBuildTimeConfig.Wsdl2JavaParameterSet
 */
public class Wsdl2JavaCodeGen implements CodeGenProvider {
    private static final Logger log = Logger.getLogger(Wsdl2JavaCodeGen.class);
    static final String WSDL2JAVA_NAMED_CONFIG_KEY_PREFIX = "quarkus.cxf.codegen.wsdl2java.";
    static final String WSDL2JAVA_CONFIG_KEY_PREFIX = "quarkus.cxf.codegen.wsdl2java";

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
        return "wsdl";
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
        final Wsdl2JavaParameterSet rootParams = buildParameterSet(configFunction, WSDL2JAVA_CONFIG_KEY_PREFIX,
                Wsdl2JavaParameterSet.DEFAULT_INCLUDES);
        final Map<String, String> processedFiles = new HashMap<>();
        boolean result = false;
        result |= wsdl2java(context.inputDir(), rootParams, outDir, WSDL2JAVA_CONFIG_KEY_PREFIX, processedFiles);

        final Set<String> names = findParamSetNames(config.getPropertyNames());
        for (String name : names) {
            final String prefix = WSDL2JAVA_NAMED_CONFIG_KEY_PREFIX + name;
            final Wsdl2JavaParameterSet namedParams = buildParameterSet(configFunction,
                    prefix, null);
            result |= wsdl2java(context.inputDir(), namedParams, outDir, prefix, processedFiles);
        }

        return result;
    }

    static boolean wsdl2java(Path inputDir, Wsdl2JavaParameterSet params, Path outDir, String prefix,
            Map<String, String> processedFiles) {

        return scan(inputDir, params.includes, params.excludes, prefix, processedFiles, (Path wsdlFile) -> {

            final List<String> args = new ArrayList<>();
            args.add("-d");
            args.add(outDir.toString());
            params.additionalParams.ifPresent(args::addAll);
            args.add(wsdlFile.toString());

            log.infof("Running wsdl2java %s", args);
            final ToolContext ctx = new ToolContext();
            try {
                new WSDLToJava(args.toArray(new String[0])).run(ctx);
            } catch (Exception e) {
                throw new RuntimeException("Could not run wsdl2Java " + args.stream().collect(Collectors.joining(" ")), e);
            }
        });
    }

    static boolean scan(
            Path inputDir,
            Optional<List<String>> includes,
            Optional<List<String>> excludes,
            String prefix,
            Map<String, String> processedFiles,
            Consumer<Path> wsdlFileConsumer) {

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

        final List<Pattern> excludePatterns = excludes.orElse(Collections.emptyList()).stream()
                .map(GlobUtil::toRegexPattern)
                .map(Pattern::compile)
                .collect(Collectors.toList());

        final AtomicBoolean result = new AtomicBoolean(false);
        for (String include : includes.get()) {
            String[] parts = splitGlob(include);

            if (parts.length == 1) {
                Stream.of(include)
                        .filter(relPath -> excludePatterns.stream()
                                .noneMatch(exclPattern -> exclPattern.matcher(relPath).matches()))
                        .map(inputDir::resolve)
                        .filter(Files::isRegularFile)
                        .peek(path -> result.set(true))
                        .forEach(chainedConsumer);
            } else if (parts.length == 2) {
                if (parts[0].length() == 0) {
                    /* we have to scan the whole workDir */
                    if (walk(inputDir, inputDir, parts[1], excludePatterns, chainedConsumer)) {
                        result.set(true);
                    }
                } else {
                    /* scan the subtree of workDir */
                    if (walk(inputDir, inputDir.resolve(parts[0]), parts[1], excludePatterns, chainedConsumer)) {
                        result.set(true);
                    }
                }
            } else {
                throw new IllegalStateException("Expected glob split array lenght 1 or 2, found " + parts.length);
            }
        }
        return result.get();
    }

    /**
     * Walk the file tree starting at {@code directory} selecting files matching {@code includeGlob} and skipping files
     * matching any of {@code excludePatterns}; the selected files are passed to {@code wsdlFileConsumer}.
     *
     * @param workDir
     * @param directory
     * @param includeGlob
     * @param excludePatterns
     * @param wsdlFileConsumer
     * @return {@code true} if some file was processed; {@code false} otherwise
     */
    static boolean walk(
            Path workDir,
            Path directory,
            String includeGlob,
            List<Pattern> excludePatterns,
            Consumer<Path> wsdlFileConsumer) {
        final Pattern includePattern = Pattern.compile(GlobUtil.toRegexPattern(includeGlob));
        final AtomicBoolean result = new AtomicBoolean(false);
        try (Stream<Path> stream = Files.walk(directory)) {
            stream
                    .map(directory::resolve)
                    .filter(absPath -> {
                        String relPathDir; // relative to directory
                        String relPathWorkDir; // relative to workDir
                        if (File.separatorChar != '/') {
                            relPathDir = directory.relativize(absPath).toString().replace(File.separatorChar, '/');
                            relPathWorkDir = workDir.relativize(absPath).toString().replace(File.separatorChar, '/');
                        } else {
                            relPathDir = directory.relativize(absPath).toString();
                            relPathWorkDir = workDir.relativize(absPath).toString();
                        }

                        return includePattern.matcher(relPathDir).matches()
                                && excludePatterns.stream()
                                        .noneMatch(exclPattern -> exclPattern.matcher(relPathWorkDir).matches());
                    })
                    .filter(Files::isRegularFile)
                    .peek(path -> result.set(true))
                    .forEach(wsdlFileConsumer);
        } catch (IOException e) {
            throw new RuntimeException("Could not walk directory " + directory);
        }
        return result.get();
    }

    /**
     * Split the glob pattern into a prefix containing one or more file path segments without any glob wildcards and
     * the rest. This will allow us to walk less directories in some situations.
     *
     * @param glob the glob pattern to split
     * @return a string array consisting of one (if the whole glob contains no wildcards) or two elements
     */
    static String[] splitGlob(String glob) {
        int lastSlashPos = Integer.MAX_VALUE;
        for (int i = 0; i < glob.length(); i++) {
            final char ch = glob.charAt(i);
            switch (ch) {
                case '\\':
                    /* the next char is escaped */
                    i++;
                    break;
                case '/':
                    lastSlashPos = i;
                    break;
                case '[':
                case '*':
                case '?':
                case '{':
                    /* wildcard */
                    if (i > lastSlashPos) {
                        return new String[] { glob.substring(0, lastSlashPos), glob.substring(lastSlashPos + 1) };
                    } else {
                        return new String[] { "", glob };
                    }
                default:
                    /* just go to the next char */
            }
        }
        /* No wildcard found in the glob */
        return new String[] { glob };
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
     * @param defaultIncludes use this if {@code prefix + ".includes"} is not available
     * @return a new {@link Wsdl2JavaParameterSet}
     */
    static Wsdl2JavaParameterSet buildParameterSet(Function<String, Optional<List<String>>> config, String prefix,
            String defaultIncludes) {
        final Wsdl2JavaParameterSet result = new Wsdl2JavaParameterSet();

        final Optional<List<String>> maybeIncludes = config.apply(prefix + ".includes");
        final List<String> includes;
        if (maybeIncludes.isPresent()) {
            includes = maybeIncludes.get();
        } else if (defaultIncludes != null) {
            includes = List.of(defaultIncludes);
        } else {
            includes = null;
        }

        final Optional<List<String>> excludes = config.apply(prefix + ".excludes");
        final Optional<List<String>> additionalParams = config.apply(prefix + ".additional-params");

        if (includes == null && (excludes.isPresent() || additionalParams.isPresent())) {
            throw new IllegalStateException("Incomplete configuration: you must set " + prefix + ".includes if you set"
                    + " any of " + prefix + ".excludes or " + prefix + ".additional-params");
        }

        result.includes = Optional.of(includes);
        result.excludes = excludes;
        result.additionalParams = additionalParams;

        return result;
    }

}
