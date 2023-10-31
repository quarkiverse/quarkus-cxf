package io.quarkiverse.cxf.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkiverse.cxf.CxfClientConfig.HTTPConduitImpl;
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
     * The default path for CXF resources.
     * <p>
     * ⚠️ Note that the default value before Quarkus CXF version 2.0.0 was {@code /}.
     * </p>
     */
    @WithDefault("/services")
    String path();

    /**
     * The comma-separated list of WSDL resource paths used by CXF.
     * Deprecated! use {@code  quarkus.native.resources.includes/excludes} instead.
     * Note that WSDL files selected by {@code quarkus.cxf.codegen.wsdl2java.includes/excludes} are included in native
     * image
     * automatically.
     */
    @Deprecated(forRemoval = true)
    Optional<List<String>> wsdlPath();

    /**
     * Build time configuration options for Quarkus code generation phase.
     */
    public CodeGenConfig codegen();

    /**
     * Build time configuration options for {@code java2ws}
     */
    @WithName("java2ws")
    public Java2WsConfig java2ws();

    /**
     * Select the {@code HTTPConduitFactory} implementation for all clients except the ones that override this setting
     * via {@code quarkus.cxf.client.myClient.http-conduit-factory}.
     * <ul>
     * <li>{@code QuarkusCXFDefault} (default): if {@code io.quarkiverse.cxf:quarkus-cxf-rt-transports-http-hc5} is
     * present in class path, then its {@code HTTPConduitFactory} implementation will be used;
     * otherwise this value is equivalent with {@code URLConnectionHTTPConduitFactory} (this may change, once
     * issue <a href="https://github.com/quarkiverse/quarkus-cxf/issues/992">#992</a> gets resolved in CXF)
     * <li>{@code CXFDefault}: the selection of {@code HTTPConduitFactory} implementation is left to CXF
     * <li>{@code HttpClientHTTPConduitFactory}: the {@code HTTPConduitFactory} will be set to an
     * implementation always returning {@code org.apache.cxf.transport.http.HttpClientHTTPConduit}. This will use
     * {@code java.net.http.HttpClient} as the underlying HTTP client.
     * <li>{@code URLConnectionHTTPConduitFactory}: the {@code HTTPConduitFactory} will be set to an
     * implementation always returning {@code org.apache.cxf.transport.http.URLConnectionHTTPConduit}. This will use
     * {@code java.net.HttpURLConnection} as the underlying HTTP client.
     * </ul>
     */
    public Optional<HTTPConduitImpl> httpConduitFactory();

    @ConfigGroup
    public interface CodeGenConfig {

        /**
         * Build time configuration options for {@code wsdl2java}
         */
        @WithName("wsdl2java")
        public Wsdl2JavaConfig wsdl2java();
    }

    @ConfigGroup
    public interface Wsdl2JavaConfig {
        /**
         * If {@code true} {@code wsdl2java} code generation is run whenever there are WSDL resources found on default
         * or custom defined locations; otherwise {@code wsdl2java} is not executed.
         */
        @WithDefault("true")
        public boolean enabled();

        /**
         * Parameters for the CXF {@code wsdl2java} tool. Use this when you want to generate Java classes from all your
         * WSDL files using the same {@code wsdl2java} parameters. You should use {@link #namedParameterSets} instead
         * if you need to invoke {@code wsdl2java} with different parameters for some of your WSDL files.
         */
        @WithParentName
        public Wsdl2JavaParameterSet rootParameterSet();

        /**
         * A collection of named parameter sets for the CXF {@code wsdl2java} tool. Each entry selects a set of WSDL
         * files and defines options to be used when invoking {@code wsdl2java} with the selected files.
         */
        @WithParentName
        public Map<String, Wsdl2JavaParameterSet> namedParameterSets();

    }

    @ConfigGroup
    public interface Wsdl2JavaParameterSet {

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
         * <li>{@code my-*-service.wsdl} will match {@code src/main/resources/my-foo-service.wsdl} and
         * {@code src/main/resources/my-bar-service.wsdl}
         * <li>{@code **.wsdl} will match any of the above
         * </ul>
         * There is a separate {@code wsdl2java} execution for each of the matching WSDL files. If you need different
         * {@link #additionalParams} for each WSDL file, you may want to define a separate named parameter set for each
         * one of them. Here is an example:
         *
         * <pre>
         * # Parameters for foo.wsdl
         * quarkus.cxf.codegen.wsdl2java.foo-params.includes = wsdl/foo.wsdl
         * quarkus.cxf.codegen.wsdl2java.foo-params.wsdl-location = wsdl/foo.wsdl
         * # Parameters for bar.wsdl
         * quarkus.cxf.codegen.wsdl2java.bar-params.includes = wsdl/bar.wsdl
         * quarkus.cxf.codegen.wsdl2java.bar-params.wsdl-location = wsdl/bar.wsdl
         * quarkus.cxf.codegen.wsdl2java.bar-params.xjc = ts
         * </pre>
         * <p>
         * Note that file extensions other than {@code .wsdl} will work during normal builds, but changes in the
         * matching files may get overseen in Quarkus dev mode. Always using the {@code .wsdl} extension is thus
         * recommended.
         * <p>
         * There is no default value for this option, so {@code wsdl2java} code generation is disabled by default.
         * <p>
         * Specifying {@code quarkus.cxf.codegen.wsdl2java.my-name.excludes} without setting any {@code includes}
         * will cause a build time error.
         * <p>
         * Make sure that the file sets selected by {@code quarkus.cxf.codegen.wsdl2java.includes} and
         * {@code quarkus.cxf.codegen.wsdl2java.[whatever-name].includes} do not overlap. Otherwise a build time
         * exception will be thrown.
         * <p>
         * The files from {@code src/main/resources} selected by {@code includes} and {@code excludes} are automatically
         * included in
         * native image and therefore you do not need to include them via {@code quarkus.cxf.wsdl-path} (deprecated) or
         * {@code quarkus.native.resources.includes/excludes}.
         */
        public Optional<List<String>> includes();

        /**
         * A comma separated list of path patterns for selecting WSDL files which should <strong>not</strong> be
         * processed with {@code wsdl2java} tool. The paths are relative to {@code src/main/resources} or
         * {@code src/test/resources} directories of the current Maven or Gradle module. Same syntax as
         * {@code includes}.
         */
        public Optional<List<String>> excludes();

        /**
         * A comma separated list of tokens; each token can be one of the following:
         * <ul>
         * <li>A Java package under which the Java source files should be generated
         * <li>A string of the form {@code namespaceURI=packageName} - in this case the entities coming from the given
         * namespace URI will be generated under the given Java package.
         * </ul>
         * <p>
         * This will be passed as option {@code -p} to {@code wsdl2java}
         */
        @Wsdl2JavaParam(value = "-p", collection = Wsdl2JavaParamCollection.commaSeparated)
        public Optional<List<String>> packageNames();

        /**
         * A comma separated list of WSDL schema namespace URIs to ignore when generating Java code.
         * <p>
         * This will be passed as option {@code -nexclude} to {@code wsdl2java}
         */
        @Wsdl2JavaParam(value = "-nexclude", collection = Wsdl2JavaParamCollection.multiParam)
        public Optional<List<String>> excludeNamespaceUris();

        /**
         * The WSDL service name to use for the generated code.
         * <p>
         * This will be passed as option {@code -sn} to {@code wsdl2java}
         */
        @Wsdl2JavaParam("-sn")
        public Optional<String> serviceName();

        /**
         * A list of paths pointing at JAXWS or JAXB binding files or XMLBeans context files. The path to be either
         * absolute or relative to the current Maven or Gradle module.
         * <p>
         * This will be passed as option {@code -b} to {@code wsdl2java}
         */
        @Wsdl2JavaParam(value = "-b", collection = Wsdl2JavaParamCollection.multiParam)
        public Optional<List<String>> bindings();

        /**
         * If {@code true}, WSDLs are validated before processing; otherwise the WSDLs are not validated.
         * <p>
         * This will be passed as option {@code -validate} to {@code wsdl2java}
         */
        @WithDefault("false")
        @Wsdl2JavaParam(value = "-validate", transformer = Wsdl2JavaParamTransformer.bool)
        public boolean validate();

        /**
         * Specifies the value of the {@code @WebServiceClient} annotation's wsdlLocation property.
         * <p>
         * This will be passed as option {@code -wsdlLocation} to {@code wsdl2java}
         */
        @Wsdl2JavaParam("-wsdlLocation")
        public Optional<String> wsdlLocation();

        /**
         * A comma separated list of XJC extensions to enable. The following extensions are available through
         * {@code io.quarkiverse.cxf:quarkus-cxf-xjc-plugins} dependency:
         * <ul>
         * <li>{@code bg} - generate {@code getX()} methods for boolean fields instead of {@code isX()}
         * <li>{@code bgi} - generate both {@code isX()} and {@code getX()} methods for boolean fields
         * <li>{@code dv} - initialize fields mapped from elements/attributes with their default values
         * <li>{@code javadoc} - generates JavaDoc based on {@code xsd:documentation}
         * <li>{@code property-listener} - add a property listener and the code for triggering the property change events to
         * setter methods
         * <li>{@code ts} - generate {@code toString()} methods
         * <li>{@code wsdlextension} - generate WSDL extension methods in root classes
         * </ul>
         * <p>
         * These values correspond to {@code -wsdl2java} options {@code -xjc-Xbg}, {@code -xjc-Xbgi},
         * {@code -xjc-Xdv}, {@code -xjc-Xjavadoc}, {@code -xjc-Xproperty-listener}, {@code -xjc-Xts} and
         * {@code -xjc-Xwsdlextension} respectively.
         */
        @Wsdl2JavaParam(value = "-xjc", collection = Wsdl2JavaParamCollection.xjc)
        public Optional<List<String>> xjc();

        /**
         * A fully qualified class name to use as a superclass for fault beans generated from {@code wsdl:fault} elements
         * <p>
         * This will be passed as option {@code -exceptionSuper} to {@code wsdl2java}
         */
        @WithDefault("java.lang.Exception")
        @Wsdl2JavaParam("-exceptionSuper")
        public String exceptionSuper();

        /**
         * A comma separated list of SEI methods for which asynchronous sibling methods should be generated; similar to
         * {@code enableAsyncMapping} in a JAX-WS binding file
         * <p>
         * This will be passed as option {@code -asyncMethods} to {@code wsdl2java}
         */
        @Wsdl2JavaParam(value = "-asyncMethods", collection = Wsdl2JavaParamCollection.commaSeparated)
        public Optional<List<String>> asyncMethods();

        /**
         * A comma separated list of SEI methods for which wrapper style sibling methods should be generated; similar to
         * {@code enableWrapperStyle} in JAX-WS binding file
         * <p>
         * This will be passed as option {@code -bareMethods} to {@code wsdl2java}
         */
        @Wsdl2JavaParam(value = "-bareMethods", collection = Wsdl2JavaParamCollection.commaSeparated)
        public Optional<List<String>> bareMethods();

        /**
         * A comma separated list of SEI methods for which {@code mime:content} mapping should be enabled; similar to
         * {@code enableMIMEContent} in JAX-WS binding file
         * <p>
         * This will be passed as option {@code -mimeMethods} to {@code wsdl2java}
         */
        @Wsdl2JavaParam(value = "-mimeMethods", collection = Wsdl2JavaParamCollection.commaSeparated)
        public Optional<List<String>> mimeMethods();

        /**
         * A comma separated list of additional command line parameters that should be passed to CXF {@code wsdl2java} tool
         * along with the files selected by {@link #includes} and {@link #excludes}. Example:
         * {@code -keep,-dex,false}. Check
         * <a href="https://cxf.apache.org/docs/wsdl-to-java.html"><code>wsdl2java</code> documentation</a> for all
         * supported options.
         */
        public Optional<List<String>> additionalParams();

    }

    @ConfigGroup
    public interface Java2WsConfig {

        /**
         * If {@code true} {@code java2ws} WSDL generation is run whenever there are Java classes selected via
         * {@code includes} and {@code excludes} options; otherwise {@code java2ws} is not executed.
         */
        @WithDefault("true")
        public boolean enabled();

        /**
         * Parameters for the CXF {@code java2ws} tool. Use this when you want to generate WSDL files from all your
         * Java classes annotated with {@link jakarta.jws.WebService}. You should use {@link #namedParameterSets}
         * instead
         * if you need to invoke {@code java2ws} with different parameters for some of your Java classes.
         */
        @WithParentName
        public Java2WsParameterSet rootParameterSet();

        /**
         * A collection of named parameter sets for the CXF {@code java2ws} tool. Each entry selects a set of Java
         * classes
         * annotated with {@link jakarta.jws.WebService} and defines options to be used when invoking {@code java2ws}
         * with the selected classes.
         */
        @WithParentName
        public Map<String, Java2WsParameterSet> namedParameterSets();

    }

    @ConfigGroup
    public interface Java2WsParameterSet {

        public static final String JAVA2WS_CONFIG_KEY_PREFIX = "quarkus.cxf.java2ws";

        /**
         * A comma separated list of glob patterns for selecting class names which should be processed with
         * {@code java2ws} tool. The glob syntax is specified in {@code io.quarkus.util.GlobUtil}.
         * The patterns are matched against fully qualified class names, such as
         * {@code org.acme.MyClass}.
         * <p>
         * The universe of class names to which {@code includes} and {@code excludes} are applied is defined as follows:
         * 1. Only classes <a href="https://quarkus.io/guides/cdi-reference#bean_discovery">visible in Jandex</a> are
         * considered. 2. From those, only the ones annotated with <code>@WebService</code> are selected.
         * <p>
         * Examples:
         * <p>
         * Let's say that the application contains two classes annotated with <code>@WebService</code> and that both are
         * visible in Jandex. Their names are {@code org.foo.FruitWebService} and {@code org.bar.HelloWebService}.
         * <p>
         * Then
         * <ul>
         * <li>{@code quarkus.cxf.java2ws.includes = **.*WebService} will match both class names
         * <li>{@code quarkus.cxf.java2ws.includes = org.foo.*} will match only {@code org.foo.FruitWebService}
         * </ul>
         * There is a separate {@code java2ws} execution for each of the matching class names. If you need different
         * {@link #additionalParams} for each class, you may want to define a separate named parameter set for each
         * one of them. Here is an example:
         *
         * <pre>
         * # Parameters for the foo package
         * quarkus.cxf.java2ws.foo-params.includes = org.foo.*
         * quarkus.cxf.java2ws.foo-params.additional-params = -servicename,FruitService
         * # Parameters for the bar package
         * quarkus.cxf.java2ws.bar-params.includes = org.bar.*
         * quarkus.cxf.java2ws.bar-params.additional-params = -servicename,HelloService
         * </pre>
         * <p>
         * There is no default value for this option, so {@code java2ws} WSDL generation is effectively disabled by
         * default.
         * <p>
         * Specifying {@code quarkus.cxf.java2ws.excludes} without setting any {@code includes}
         * will cause a build time error.
         * <p>
         * Make sure that the class names selected by {@code quarkus.cxf.java2ws.includes} and
         * {@code quarkus.cxf.java2ws.[whatever-name].includes} do not overlap. Otherwise a build time
         * exception will be thrown.
         * <p>
         * If you would like to include the generated WSDL files in native image, you need to add them yourself using
         * {@code quarkus.native.resources.includes/excludes}.
         */
        public Optional<List<String>> includes();

        /**
         * A comma separated list of glob patterns for selecting java class names which should <strong>not</strong> be
         * processed with {@code java2ws} tool. Same syntax as {@code includes}.
         */
        public Optional<List<String>> excludes();

        /**
         * A comma separated list of additional command line parameters that should be passed to CXF {@code java2ws}
         * tool
         * along with the files selected by {@link #includes} and {@link #excludes}. Example:
         * {@code -portname,12345}. Check
         * <a href="https://cxf.apache.org/docs/java-to-ws.html"><code>java2ws</code> documentation</a> for all
         * supported options.
         * <p>
         * Note that only options related to generation of WSDL from Java are supported currently.
         */
        public Optional<List<String>> additionalParams();

        /**
         * A template for the names of generated WSDL files.
         *
         * <p>
         * There are 4 place holders, which can be used in the template:
         * <ul>
         * <li>{@code %SIMPLE_CLASS_NAME%} - the simple class name of the Java class from which we are generating
         * <li>{@code %FULLY_QUALIFIED_CLASS_NAME%} - the fully qualified name from which we are generating with all
         * dots are replaced replaced by underscores
         * <li>{@code %TARGET_DIR%} - the target directory of the current module of the current build tool; typically
         * {@code target} for Maven and {@code build} for Gradle.
         * <li>{@code %CLASSES_DIR%} - the compiler output directory of the current module of the current build tool;
         * typically {@code target/classes} for Maven and {@code build/classes} for Gradle.
         * </ul>
         */
        @WithDefault("%CLASSES_DIR%/wsdl/%SIMPLE_CLASS_NAME%.wsdl")
        public String wsdlNameTemplate();

        /**
         * Throws an {@link IllegalStateException} if this {@link Java2WsParameterSet} is invalid.
         *
         * @param prefix the property prefix such as {@code quarkus.cxf.java2ws.foo} to use in the exception message if
         *        this {@link Java2WsParameterSet} is invalid
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
