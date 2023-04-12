package io.quarkiverse.cxf.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

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

    /**
     * Build time configuration options for {@code java2ws}
     */
    @ConfigItem(name = "java2ws")
    public Java2WsConfig java2ws;

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
        //todo do we want to set to false, to avoid possible java2ws and wsdl2java cycles
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

    @ConfigGroup
    public static class Java2WsConfig {

        /**
         * If {@code true} {@code java2ws} wsdl generation is run whenever there are Java classes annotated with
         * {@link jakarta.jws.WebService} selected via following properties;
         * otherwise {@code java2ws} is not executed.
         */
        @ConfigItem(defaultValue = "false")
        public boolean enabled;

        /**
         * Parameters for the CXF {@code java2ws} tool. Use this when you want to generate wsdl files from all your
         * Java classes annotated with {@link jakarta.jws.WebService}. You should use {@link #namedParameterSets} instead
         * if you need to invoke {@code java2ws} with different parameters for some of your Java classes.
         * <p>
         * {@code rootParameterSet} is <strong>ignored</strong> if there is at least one {@code namedParameterSet}.
         * </p>
         */
        @ConfigItem(name = ConfigItem.PARENT)
        public Java2WsParameterSet rootParameterSet;

        /**
         * A collection of named parameter sets for the CXF {@code java2ws} tool. Each entry selects a set of Java classes
         * annotated with {@link jakarta.jws.WebService} and defines options to be used when invoking {@code java2ws}
         * with the selected classes.
         */
        @ConfigItem(name = ConfigItem.PARENT)
        public Map<String, Java2WsParameterSet> namedParameterSets;

    }

    @ConfigGroup
    public static class Java2WsParameterSet {
        public static final String DEFAULT_WSDL_NAME_TEMPLATE = "<CLASS_NAME>.wsdl";
        public static final String DEFAULT_INCLUDES = ".*";

        /**
         * A Java regular expression for selecting classes which should be processed with
         * {@code java2wsdl} tool. Regular expression is used for matching fully qualified names of the classes.
         * The glob syntax is specified in {@link java.util.regex.Pattern}.
         * <p>
         * Examples:
         * <ul>
         * <li>{@code .*} will match both classes
         * {@code src/main/java/test/io/quarkiverse/cxf/deployment/java2ws/FruitWebService.java} and
         * {@code src/main/java/test/io/quarkiverse/cxf/deployment/java2ws/GreeterService.java} under the current Maven or
         * Gradle module
         * <li>{@code .*Fruit.*} matches {@code src/main/java/test/io/quarkiverse/cxf/deployment/java2ws/FruitWebService.java}
         * and not
         * {@code src/main/java/test/io/quarkiverse/cxf/deployment/java2ws/GreeterService.java}
         * </ul>
         * <p>
         * The default value for {@code quarkus.cxf.java2ws.include} is <code>.*</code> .
         * Named parameter sets, such as {@code quarkus.cxf.java2ws.my-name.include},
         * have no default and not specifying any {@code include} value there will cause a build time error.
         * <p>
         * Make sure that the class sets selected by {@code quarkus.cxf.java2ws.includes} and
         * {@code quarkus.cxf.java2ws.[whatever-name].include} do not overlap.
         * <p>
         * The generated wsdls are <strong>not</strong> included in native image.
         */
        @ConfigItem
        public Optional<String> include;

        /**
         * A Java regular expression for selecting classes which should <strong>not</strong> be
         * processed with {@code java2ws} tool. Same syntax as {@code include}.
         */
        @ConfigItem
        public Optional<String> exclude;

        /**
         * A comma separated list of additional command line parameters that should be passed to CXF {@code java2ws} tool
         * along with the files selected by {@link #include} and {@link #exclude}. Example:
         * {@code -portname,12345}. Check
         * <a href="https://cxf.apache.org/docs/java-to-wsdl.html"><code>java2ws</code> documentation</a> for all
         * supported options.
         * <p>
         */
        @ConfigItem
        public Optional<List<String>> additionalParams;

        /**
         * The directory in which the WSDL output files are placed. The paths are relative to the working directory of the
         * current
         * Maven or Gradle module. If not specified, path {@code target/classes/wsdl} for Maven and {@code build/classes/wsdl}
         * is used by default.
         */
        @ConfigItem
        public Optional<String> outputDir;

        /**
         * A template for the names of the generated WSDL files.
         *
         * <p>
         * There are 2 placeholders, which could be used in the template
         * <ul>
         * <li>{@code &lt;CLASS_NAME&gt;} will be replaced by the class's simple name.
         * <li>{@code &lt;FULLY_QUALIFIED_CLASS_NAME&gt;} will be replaced by the class's fully qualified name, where all
         * occurrences of '.' are replaced with '_'.
         * </ul>
         * <p>
         * The default value is <code>&lt;CLASS_NAME&gt;.wsdl</code>
         * <p>
         * Examples:
         * <ul>
         * <li>{@code &lt;CLASS_NAME&gt;.wsdl} - generated file from class {@code *.GreeterService} will be named
         * {@code GreeterService.wsdl}
         * <li>{@code &lt;FULLY_QUALIFIED_CLASS_NAME&gt;.xml} - generated file from class {@code my.package.GreeterService} will
         * be named my_package_GreeterService.xml
         * </ul>
         */
        @ConfigItem
        public Optional<String> wsdlNameTemplate;
    }

}
