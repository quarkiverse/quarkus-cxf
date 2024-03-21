package io.quarkiverse.cxf.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkiverse.cxf.deployment.codegen.Wsdl2JavaParam;
import io.quarkiverse.cxf.deployment.codegen.Wsdl2JavaParam.Wsdl2JavaParamCollection;
import io.quarkiverse.cxf.deployment.codegen.Wsdl2JavaParam.Wsdl2JavaParamTransformer;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.cxf")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface CxfBuildTimeConfig {

    /**
     * The comma-separated list of WSDL resource paths used by CXF. Deprecated! use `quarkus.native.resources.includes/excludes`
     * instead. Note that WSDL files selected by `quarkus.cxf.codegen.wsdl2java.includes/excludes` are included in native image
     * automatically.
     *
     * @asciidoclet
     * @since 1.0.0
     */
    @Deprecated(forRemoval = true)
    Optional<List<String>> wsdlPath();

    /**
     * Build time configuration options for Quarkus code generation phase.
     *
     * @asciidoclet
     */
    public CodeGenConfig codegen();

    /**
     * Build time configuration options for `java2ws`
     *
     * @asciidoclet
     */
    @WithName("java2ws")
    public Java2WsConfig java2ws();

    @ConfigGroup
    public interface CodeGenConfig {

        /**
         * Build time configuration options for `wsdl2java`
         *
         * @asciidoclet
         */
        @WithName("wsdl2java")
        public Wsdl2JavaConfig wsdl2java();
    }

    @ConfigGroup
    public interface Wsdl2JavaConfig {

        /**
         * If `true` `wsdl2java` code generation is run whenever there are WSDL resources found on default or custom defined
         * locations; otherwise `wsdl2java` is not executed.
         *
         * @asciidoclet
         * @since 2.0.0
         */
        @WithDefault("true")
        public boolean enabled();

        /**
         * Parameters for the CXF `wsdl2java` tool. Use this when you want to generate Java classes from all your WSDL files
         * using the same `wsdl2java` parameters. You should use `named-parameter-sets` instead if you need to invoke
         * `wsdl2java` with different parameters for some of your WSDL files.
         *
         * @asciidoclet
         * @since 2.0.0
         */
        @WithParentName
        public Wsdl2JavaParameterSet rootParameterSet();

        /**
         * A collection of named parameter sets for the CXF `wsdl2java` tool. Each entry selects a set of WSDL files and defines
         * options to be used when invoking `wsdl2java` with the selected files.
         *
         * @asciidoclet
         * @since 2.0.0
         */
        @WithParentName
        public Map<String, Wsdl2JavaParameterSet> namedParameterSets();
    }

    @ConfigGroup
    public interface Wsdl2JavaParameterSet {

        /**
         * A comma separated list of glob patterns for selecting WSDL files which should be processed with `wsdl2java` tool. The
         * paths are relative to `src/main/resources` or `src/test/resources` directories of the current Maven or Gradle module.
         * The glob syntax is specified in `io.quarkus.util.GlobUtil`.
         *
         * Examples:
         *
         * - `calculator.wsdl,fruits.wsdl` will match `src/main/resources/calculator.wsdl` and `src/main/resources/fruits.wsdl`
         * under the current Maven or Gradle module, but will not match anything like
         * `src/main/resources/subdir/calculator.wsdl`
         * - `my-++*++-service.wsdl` will match `src/main/resources/my-foo-service.wsdl` and
         * `src/main/resources/my-bar-service.wsdl`
         * - `++**++.wsdl` will match any of the above There is a separate `wsdl2java` execution for each of the matching WSDL
         * files. If you need different `additional-params` for each WSDL file, you may want to define a separate named
         * parameter set for each one of them. Here is an example:
         *
         * [source,properties]
         * ----
         * # Parameters for foo.wsdl
         * quarkus.cxf.codegen.wsdl2java.foo-params.includes = wsdl/foo.wsdl
         * quarkus.cxf.codegen.wsdl2java.foo-params.wsdl-location = wsdl/foo.wsdl
         * # Parameters for bar.wsdl
         * quarkus.cxf.codegen.wsdl2java.bar-params.includes = wsdl/bar.wsdl
         * quarkus.cxf.codegen.wsdl2java.bar-params.wsdl-location = wsdl/bar.wsdl
         * quarkus.cxf.codegen.wsdl2java.bar-params.xjc = ts
         * ----
         *
         * [NOTE]
         * .File extensions
         * ====
         * File extensions other than `.wsdl` will work during normal builds, but changes in the matching files may get overseen
         * in Quarkus dev mode. We recommend that you always use the `.wsdl` extension.
         * ====
         *
         * There is no default value for this option, so `wsdl2java` code generation is disabled by default.
         *
         * Specifying `quarkus.cxf.codegen.wsdl2java.my-name.excludes` without setting any `includes` will cause a build time
         * error.
         *
         * Make sure that the file sets selected by `quarkus.cxf.codegen.wsdl2java.includes` and
         * `quarkus.cxf.codegen.wsdl2java.++[++whatever-name++]++.includes` do not overlap. Otherwise a build time exception
         * will be thrown.
         *
         * The files from `src/main/resources` selected by `includes` and `excludes` are automatically included in native image
         * and therefore you do not need to include them via `quarkus.cxf.wsdl-path` (deprecated) or
         * `quarkus.native.resources.includes/excludes`.
         *
         * @asciidoclet
         * @since 2.0.0
         */
        public Optional<List<String>> includes();

        /**
         * A comma separated list of path patterns for selecting WSDL files which should *not* be processed with `wsdl2java`
         * tool. The paths are relative to `src/main/resources` or `src/test/resources` directories of the current Maven or
         * Gradle module. Same syntax as `includes`.
         *
         * @asciidoclet
         * @since 2.0.0
         */
        public Optional<List<String>> excludes();

        /**
         * A directory into which the generated files will be written, either absolute or relative to the current Maven or
         * Gradle module directory.
         *
         * The default value is build tool dependent: for Maven, it is typically `target/generated-sources/wsdl2java`, while for
         * Gradle it is `build/classes/java/quarkus-generated-sources/wsdl2java`.
         *
         * Quarkus tooling is only able to set up the default value as a source folder for the given build tool. If you set this
         * to a custom path it is up to you to make sure that your build tool recognizes the path a as source folder.
         *
         * Also, if you choose a path outside `target` directory for Maven or outside `build` directory for Gradle, you need to
         * take care for cleaning stale resources generated by previous builds. E.g. if you change the value of `package-names`
         * option from `org.foo` to `org.bar` you need to take care for the removal of the removal of the old package `org.foo`.
         *
         * This will be passed as option `-d` to `wsdl2java`
         *
         * @since 2.6.0
         * @asciidoclet
         */
        public Optional<String> outputDirectory();

        /**
         * A comma separated list of tokens; each token can be one of the following:
         *
         * - A Java package under which the Java source files should be generated
         * - A string of the form `namespaceURI=packageName` - in this case the entities coming from the given namespace URI
         * will be generated under the given Java package.
         *
         * This will be passed as option `-p` to `wsdl2java`
         *
         * @asciidoclet
         * @since 2.4.0
         */
        @Wsdl2JavaParam(value = "-p", collection = Wsdl2JavaParamCollection.commaSeparated)
        public Optional<List<String>> packageNames();

        /**
         * A comma separated list of WSDL schema namespace URIs to ignore when generating Java code.
         *
         * This will be passed as option `-nexclude` to `wsdl2java`
         *
         * @asciidoclet
         * @since 2.4.0
         */
        @Wsdl2JavaParam(value = "-nexclude", collection = Wsdl2JavaParamCollection.multiParam)
        public Optional<List<String>> excludeNamespaceUris();

        /**
         * The WSDL service name to use for the generated code.
         *
         * This will be passed as option `-sn` to `wsdl2java`
         *
         * @asciidoclet
         * @since 2.4.0
         */
        @Wsdl2JavaParam("-sn")
        public Optional<String> serviceName();

        /**
         * A list of paths pointing at JAXWS or JAXB binding files or XMLBeans context files. The path to be either absolute or
         * relative to the current Maven or Gradle module.
         *
         * This will be passed as option `-b` to `wsdl2java`
         *
         * @asciidoclet
         * @since 2.4.0
         */
        @Wsdl2JavaParam(value = "-b", collection = Wsdl2JavaParamCollection.multiParam)
        public Optional<List<String>> bindings();

        /**
         * If `true`, WSDLs are validated before processing; otherwise the WSDLs are not validated.
         *
         * This will be passed as option `-validate` to `wsdl2java`
         *
         * @asciidoclet
         * @since 2.4.0
         */
        @WithDefault("false")
        @Wsdl2JavaParam(value = "-validate", transformer = Wsdl2JavaParamTransformer.bool)
        public boolean validate();

        /**
         * Specifies the value of the `@WebServiceClient` annotation's wsdlLocation property.
         *
         * This will be passed as option `-wsdlLocation` to `wsdl2java`
         *
         * @asciidoclet
         * @since 2.4.0
         */
        @Wsdl2JavaParam("-wsdlLocation")
        public Optional<String> wsdlLocation();

        /**
         * A comma separated list of XJC extensions to enable. The following extensions are available through
         * `io.quarkiverse.cxf:quarkus-cxf-xjc-plugins` dependency:
         *
         * - `bg` - generate `getX()` methods for boolean fields instead of `isX()`
         * - `bgi` - generate both `isX()` and `getX()` methods for boolean fields
         * - `dv` - initialize fields mapped from elements/attributes with their default values
         * - `javadoc` - generates JavaDoc based on `xsd:documentation`
         * - `property-listener` - add a property listener and the code for triggering the property change events to setter
         * methods
         * - `ts` - generate `toString()` methods
         * - `wsdlextension` - generate WSDL extension methods in root classes
         *
         * These values correspond to `-wsdl2java` options `-xjc-Xbg`, `-xjc-Xbgi`, `-xjc-Xdv`, `-xjc-Xjavadoc`,
         * `-xjc-Xproperty-listener`, `-xjc-Xts` and `-xjc-Xwsdlextension` respectively.
         *
         * @asciidoclet
         * @since 2.4.0
         */
        @Wsdl2JavaParam(value = "-xjc", collection = Wsdl2JavaParamCollection.xjc)
        public Optional<List<String>> xjc();

        /**
         * A fully qualified class name to use as a superclass for fault beans generated from `wsdl:fault` elements
         *
         * This will be passed as option `-exceptionSuper` to `wsdl2java`
         *
         * @asciidoclet
         * @since 2.4.0
         */
        @WithDefault("java.lang.Exception")
        @Wsdl2JavaParam("-exceptionSuper")
        public String exceptionSuper();

        /**
         * A comma separated list of SEI methods for which asynchronous sibling methods should be generated; similar to
         * `enableAsyncMapping` in a JAX-WS binding file
         *
         * This will be passed as option `-asyncMethods` to `wsdl2java`
         *
         * @asciidoclet
         * @since 2.4.0
         */
        @Wsdl2JavaParam(value = "-asyncMethods", collection = Wsdl2JavaParamCollection.commaSeparated)
        public Optional<List<String>> asyncMethods();

        /**
         * A comma separated list of SEI methods for which wrapper style sibling methods should be generated; similar to
         * `enableWrapperStyle` in JAX-WS binding file
         *
         * This will be passed as option `-bareMethods` to `wsdl2java`
         *
         * @asciidoclet
         * @since 2.4.0
         */
        @Wsdl2JavaParam(value = "-bareMethods", collection = Wsdl2JavaParamCollection.commaSeparated)
        public Optional<List<String>> bareMethods();

        /**
         * A comma separated list of SEI methods for which `mime:content` mapping should be enabled; similar to
         * `enableMIMEContent` in JAX-WS binding file
         *
         * This will be passed as option `-mimeMethods` to `wsdl2java`
         *
         * @asciidoclet
         * @since 2.4.0
         */
        @Wsdl2JavaParam(value = "-mimeMethods", collection = Wsdl2JavaParamCollection.commaSeparated)
        public Optional<List<String>> mimeMethods();

        /**
         * A comma separated list of additional command line parameters that should be passed to CXF `wsdl2java` tool along with
         * the files selected by `includes` and `excludes`. Example: `-keep,-dex,false`. Check
         * link:https://cxf.apache.org/docs/wsdl-to-java.html[`wsdl2java` documentation] for all supported options.
         *
         * @asciidoclet
         * @since 2.0.0
         */
        public Optional<List<String>> additionalParams();
    }

    @ConfigGroup
    public interface Java2WsConfig {

        /**
         * If `true` `java2ws` WSDL generation is run whenever there are Java classes selected via `includes` and `excludes`
         * options; otherwise `java2ws` is not executed.
         *
         * @asciidoclet
         * @since 2.0.0
         */
        @WithDefault("true")
        public boolean enabled();

        /**
         * Parameters for the CXF `java2ws` tool. Use this when you want to generate WSDL files from all your Java classes
         * annotated with `jakarta.jws.WebService`. You should use `named-parameter-sets` instead if you need to invoke
         * `java2ws` with different parameters for some of your Java classes.
         *
         * @asciidoclet
         * @since 2.0.0
         */
        @WithParentName
        public Java2WsParameterSet rootParameterSet();

        /**
         * A collection of named parameter sets for the CXF `java2ws` tool. Each entry selects a set of Java classes annotated
         * with `jakarta.jws.WebService` and defines options to be used when invoking `java2ws` with the selected classes.
         *
         * @asciidoclet
         * @since 2.0.0
         */
        @WithParentName
        public Map<String, Java2WsParameterSet> namedParameterSets();
    }

    @ConfigGroup
    public interface Java2WsParameterSet {

        public static final String JAVA2WS_CONFIG_KEY_PREFIX = "quarkus.cxf.java2ws";

        /**
         * A comma separated list of glob patterns for selecting class names which should be processed with `java2ws` tool. The
         * glob syntax is specified in `io.quarkus.util.GlobUtil`. The patterns are matched against fully qualified class names,
         * such as `org.acme.MyClass`.
         *
         * The universe of class names to which `includes` and `excludes` are applied is defined as follows: 1. Only classes
         * link:https://quarkus.io/guides/cdi-reference#bean_discovery[visible in Jandex] are considered. 2. From those, only
         * the ones annotated with `@WebService` are selected.
         *
         * Examples:
         *
         * Let's say that the application contains two classes annotated with `@WebService` and that both are visible in Jandex.
         * Their names are `org.foo.FruitWebService` and `org.bar.HelloWebService`.
         *
         * Then
         *
         * - `quarkus.cxf.java2ws.includes = ++**++.++*++WebService` will match both class names
         * - `quarkus.cxf.java2ws.includes = org.foo.++*++` will match only `org.foo.FruitWebService` There is a separate
         * `java2ws` execution for each of the matching class names. If you need different `additional-params` for each class,
         * you may want to define a separate named parameter set for each one of them. Here is an example:
         *
         * [source,properties]
         * ----
         * # Parameters for the foo package
         * quarkus.cxf.java2ws.foo-params.includes = org.foo.*
         * quarkus.cxf.java2ws.foo-params.additional-params = -servicename,FruitService
         * # Parameters for the bar package
         * quarkus.cxf.java2ws.bar-params.includes = org.bar.*
         * quarkus.cxf.java2ws.bar-params.additional-params = -servicename,HelloService
         * ----
         *
         * There is no default value for this option, so `java2ws` WSDL generation is effectively disabled by default.
         *
         * Specifying `quarkus.cxf.java2ws.excludes` without setting any `includes` will cause a build time error.
         *
         * Make sure that the class names selected by `quarkus.cxf.java2ws.includes` and
         * `quarkus.cxf.java2ws.++[++whatever-name++]++.includes` do not overlap. Otherwise a build time exception will be
         * thrown.
         *
         * If you would like to include the generated WSDL files in native image, you need to add them yourself using
         * `quarkus.native.resources.includes/excludes`.
         *
         * @asciidoclet
         * @since 2.0.0
         */
        public Optional<List<String>> includes();

        /**
         * A comma separated list of glob patterns for selecting java class names which should *not* be processed with `java2ws`
         * tool. Same syntax as `includes`.
         *
         * @asciidoclet
         * @since 2.0.0
         */
        public Optional<List<String>> excludes();

        /**
         * A comma separated list of additional command line parameters that should be passed to CXF `java2ws` tool along with
         * the files selected by `includes` and `excludes`. Example: `-portname,12345`. Check
         * link:https://cxf.apache.org/docs/java-to-ws.html[`java2ws` documentation] for all supported options.
         *
         * [NOTE]
         * .Supported options
         * ====
         * Currently, only options related to generation of WSDL from Java are supported.
         * ====
         *
         * @asciidoclet
         * @since 2.0.0
         */
        public Optional<List<String>> additionalParams();

        /**
         * A template for the names of generated WSDL files.
         *
         * There are 4 place holders, which can be used in the template:
         *
         * - `%SIMPLE_CLASS_NAME%` - the simple class name of the Java class from which we are generating
         * - `%FULLY_QUALIFIED_CLASS_NAME%` - the fully qualified name from which we are generating with all dots are replaced
         * replaced by underscores
         * - `%TARGET_DIR%` - the target directory of the current module of the current build tool; typically `target` for Maven
         * and `build` for Gradle.
         * - `%CLASSES_DIR%` - the compiler output directory of the current module of the current build tool; typically
         * `target/classes` for Maven and `build/classes` for Gradle.
         *
         * @asciidoclet
         * @since 2.0.0
         */
        @WithDefault("%CLASSES_DIR%/wsdl/%SIMPLE_CLASS_NAME%.wsdl")
        public String wsdlNameTemplate();

        /**
         * Throws an `IllegalStateException` if this `Java2WsParameterSet` is invalid.
         *
         * @param prefix the property prefix such as {@code quarkus.cxf.java2ws.foo} to use in the exception message if
         *        this {@link Java2WsParameterSet} is invalid
         * @asciidoclet
         */
        default void validate(String prefix) {
            if (includes().isPresent()) {
                /* valid */
                return;
            } else if (excludes().isPresent() && additionalParams().isPresent()) {
                throw new IllegalStateException(prefix + ".excludes and " + prefix + ".additional-params are specified but "
                        + prefix + ".includes are not specified. Specify some includes");
            } else if (excludes().isPresent()) {
                throw new IllegalStateException(prefix + ".excludes are specified but " + prefix
                        + ".includes are not specified. Specify some includes");
            } else if (additionalParams().isPresent()) {
                throw new IllegalStateException(prefix + ".additional-params are specified but " + prefix
                        + ".includes are not specified. Specify some includes");
            }
        }
    }
}
