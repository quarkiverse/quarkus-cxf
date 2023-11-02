package io.quarkiverse.cxf.deployment.codegen;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.deployment.CxfBuildTimeConfig.Wsdl2JavaParameterSet;
import io.quarkiverse.cxf.deployment.codegen.Wsdl2JavaCodeGen.Wsdl2JavaParams;

public class Wsdl2JavaParamsTest {

    @Test
    void wsdl2JavaParamsNone() {
        assertParams(
                proxy(
                        "outputDirectory", Optional.empty(),
                        "packageNames", Optional.empty(),
                        "serviceName", Optional.empty(),
                        "bindings", Optional.empty(),
                        "excludeNamespaceUris", Optional.empty(),
                        "validate", Boolean.FALSE,
                        "wsdlLocation", Optional.empty(),
                        "xjc", Optional.empty(),
                        "exceptionSuper", "java.lang.Exception",
                        "asyncMethods", Optional.empty(),
                        "bareMethods", Optional.empty(),
                        "mimeMethods", Optional.empty(),
                        "additionalParams", Optional.empty()),
                "-d",
                "/path/to/project/target/classes",
                "/path/to/project/src/main/resources/my.wsdl");
    }

    @Test
    void wsdl2JavaParams() {
        assertParams(
                proxy(
                        "outputDirectory", Optional.of("foo/bar"),
                        "packageNames", Optional.of(Arrays.asList("com.foo", "com.bar")),
                        "serviceName", Optional.of("HelloService"),
                        "bindings", Optional.of(Arrays.asList("src/main/resources/b1.xml", "src/main/resources/b2.xml")),
                        "excludeNamespaceUris", Optional.of(Arrays.asList("http://foo.com", "http://bar.com")),
                        "validate", Boolean.TRUE,
                        "wsdlLocation", Optional.of("my.wsdl"),
                        "xjc", Optional.of(Arrays.asList("bg", "dv")),
                        "exceptionSuper", "java.lang.RuntimeException",
                        "asyncMethods", Optional.of(Arrays.asList("hello", "goodBye")),
                        "bareMethods", Optional.of(Arrays.asList("bare1", "bare2")),
                        "mimeMethods", Optional.of(Arrays.asList("mime1", "mime2")),
                        "additionalParams", Optional.of(Arrays.asList("-keep", "-dex", "true"))),
                "-d", "/path/to/project/foo/bar",
                "-asyncMethods", "hello,goodBye",
                "-bareMethods", "bare1,bare2",
                "-b", "/path/to/project/src/main/resources/b1.xml",
                "-b", "/path/to/project/src/main/resources/b2.xml",
                "-exceptionSuper", "java.lang.RuntimeException",
                "-nexclude", "http://foo.com",
                "-nexclude", "http://bar.com",
                "-mimeMethods", "mime1,mime2",
                "-p", "com.foo,com.bar",
                "-sn", "HelloService",
                "-validate",
                "-wsdlLocation", "my.wsdl",
                "-xjc-Xbg",
                "-xjc-Xdv",
                "-keep", "-dex", "true",
                "/path/to/project/src/main/resources/my.wsdl");
    }

    private Wsdl2JavaParameterSet proxy(final Object... values) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                for (int i = 0; i < values.length;) {
                    Object key = values[i++];
                    Object val = values[i++];
                    if (method.getName().equals(key)) {
                        return val;
                    }
                }
                throw new IllegalStateException("No such key '" + method.getName() + "' in values "
                        + Stream.of(values).map(String::valueOf).collect(Collectors.joining(", ")));
            }
        };
        return (Wsdl2JavaParameterSet) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { Wsdl2JavaParameterSet.class }, handler);
    }

    void assertParams(Wsdl2JavaParameterSet params, String... expectedParams) {

        Path projectDir = Paths.get("/path/to/project");
        Path wsdlFile = projectDir.resolve("src/main/resources/my.wsdl");
        Path outDir = projectDir.resolve("target/classes");
        final Wsdl2JavaParams wsdl2JavaParams = new Wsdl2JavaParams(
                projectDir,
                outDir, wsdlFile, params);
        Assertions.assertThat(wsdl2JavaParams.toParameterArray()).containsExactly(expectedParams);

    }

}
