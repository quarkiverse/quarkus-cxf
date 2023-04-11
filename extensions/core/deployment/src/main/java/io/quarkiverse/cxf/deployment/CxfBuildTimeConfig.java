package io.quarkiverse.cxf.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.util.GlobUtil;

@ConfigRoot(name = "cxf", phase = ConfigPhase.BUILD_TIME)
public class CxfBuildTimeConfig {
    /**
     * The default path for CXF resources
     */
    @ConfigItem(defaultValue = "/services")
    String path;

    /**
     * The comma-separated list of WSDL resource paths used by CXF.
     * Deprecated! use {@code  quarkus.native.resources.includes/excludes} instead.
     * Note that WSDL files selected by {@code quarkus.cxf.codegen.wsdl2java.includes/excludes} are included in native
     * image
     * automatically.
     */
    @Deprecated(forRemoval = true)
    @ConfigItem
    Optional<List<String>> wsdlPath;

    /**
     * Build time configuration options for Quarkus code generation phase.
     */
    @ConfigItem
    public CodeGenConfig codegen;

    @ConfigGroup
    public static class CodeGenConfig {
        /**
         * Build time configuration options for {@code wsdl2java}
         */
        @ConfigItem(name = "wsdl2java")
        public Wsdl2JavaConfig wsdl2java;

    }

    @ConfigGroup
    public static class Wsdl2JavaConfig {
        /**
         * If {@code true} {@code wsdl2java} code generation is run whenever there are WSDL resources found on default
         * or custom defined locations; otherwise {@code wsdl2java} is not executed.
         */
        @ConfigItem(defaultValue = "true")
        public boolean enabled;

        /**
         * Parameters for the CXF {@code wsdl2java} tool. Use this when you want to generate Java classes from all your
         * WSDL files using the same {@code wsdl2java} parameters. You should use {@link #namedParameterSets} instead
         * if you need to invoke {@code wsdl2java} with different parameters for some of your WSDL files.
         */
        @ConfigItem(name = ConfigItem.PARENT)
        public Wsdl2JavaParameterSet rootParameterSet;

        /**
         * A collection of named parameter sets for the CXF {@code wsdl2java} tool. Each entry selects a set of WSDL
         * files and defines options to be used when invoking {@code wsdl2java} with the selected files.
         */
        @ConfigItem(name = ConfigItem.PARENT)
        public Map<String, Wsdl2JavaParameterSet> namedParameterSets;

    }

    @ConfigGroup
    public static class Wsdl2JavaParameterSet {
        public static final String DEFAULT_INCLUDES = "**.wsdl";

        /**
         * A comma separated list of glob patterns for selecting WSDL files which should be processed with
         * {@code wsdl2java} tool. The paths are relative to {@code src/main/resources} or {@code src/test/resources}
         * directories of the current Maven or Gradle module. The glob syntax is specified in
         * {@code io.quarkus.util.GlobUtil}.
         * <p>
         * Examples:
         * <ul>
         * <li>{@code calculator.wsdl,fruits.wsdl} will match {@code src/main/resources/calculator.wsdl} and
         * {@code src/main/resources/fruits.wsdl} under the current Maven or Gradle module, but will not match anything
         * like
         * {@code src/main/resources/subdir/calculator.wsdl}
         * <li>{@code my-*-service.wsdl} match {@code src/main/resources/my-foo-service.wsdl} and
         * {@code src/main/resources/my-bar-service.wsdl}
         * <li>{@code **.wsdl} will match any of the above
         * </ul>
         * Note that file extensions other than {@code .wsdl} will work during normal builds, but changes in the
         * matching files may get overseen in Quarkus dev mode. Always using the {@code .wsdl} extension is thus
         * recommended.
         * <p>
         * The default value for {@code quarkus.cxf.codegen.wsdl2java.includes} is <code>**.wsdl</code>
         * Named parameter sets, such as {@code quarkus.cxf.codegen.wsdl2java.my-name.includes}
         * have no default and not specifying any {@code includes} value there will cause a build time error.
         * <p>
         * Make sure that the file sets selected by {@code quarkus.cxf.codegen.wsdl2java.includes} and
         * {@code quarkus.cxf.codegen.wsdl2java.[whatever-name].includes} do not overlap.
         * <p>
         * The files from {@code src/main/resources} selected by {@code includes} and {@code excludes} are automatically
         * included in
         * native image and therefore you do not need to include them via {@code quarkus.cxf.wsdl-path} (deprecated) or
         * {@code quarkus.cxf.codegen.wsdl2java.includes/excludes}.
         */
        @ConfigItem
        public Optional<List<String>> includes;

        /**
         * A comma separated list of path patterns for selecting WSDL files which should <strong>not</strong> be
         * processed with {@code wsdl2java} tool. The paths are relative to {@code src/main/resources} or
         * {@code src/test/resources} directories of the current Maven or Gradle module. Same syntax as
         * {@code includes}.
         */
        @ConfigItem
        public Optional<List<String>> excludes;

        /**
         * A comma separated list of additional command line parameters that should passed to CXF {@code wsdl2java} tool
         * along with the files selected by {@link #includes} and {@link #excludes}. Example:
         * {@code -wsdlLocation,classpath:wsdl/CalculatorService.wsdl}. Check
         * <a href="https://cxf.apache.org/docs/wsdl-to-java.html"><code>wsdl2java</code> documentation</a> for all
         * supported options.
         * <p>
         * You need to add {@code io.quarkiverse.cxf:quarkus-cxf-xjc-plugins} dependency to your project to be able to
         * use {@code -xjc-Xboolean}, {@code -xjc-Xdv}, {@code -xjc-Xjavadoc}, {@code -xjc-Xpl}, {@code -xjc-Xts} or
         * {@code -xjc-Xwsdlextension}.
         */
        @ConfigItem
        public Optional<List<String>> additionalParams;

    }

}
