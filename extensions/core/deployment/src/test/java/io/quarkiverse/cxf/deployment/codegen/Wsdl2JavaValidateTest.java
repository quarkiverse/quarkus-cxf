package io.quarkiverse.cxf.deployment.codegen;

import static io.quarkiverse.cxf.deployment.codegen.Wsdl2JavaParamsTest.proxy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkiverse.cxf.deployment.CxfBuildTimeConfig.Wsdl2JavaParameterSet;

public class Wsdl2JavaValidateTest {

    @TempDir
    Path tempDir;

    /**
     * ValidateDifference.wsdl has a "mixed style" issue (WSDL binding uses {@code style="rpc"} while each
     * operation overrides it with {@code style="document"}). Without {@code -validate}, wsdl2java ignores
     * this and generates code successfully.
     */
    @Test
    void validateDifferenceWithoutValidateSucceeds() throws Exception {
        Path inputDir = getTestResourcePath();
        Path outDir = tempDir.resolve("without-validate");

        Wsdl2JavaParameterSet params = proxy(
                "includes", Optional.of(List.of("ValidateDifference.wsdl")),
                "excludes", Optional.empty(),
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
                "additionalParams", Optional.empty());

        Map<String, String> processedFiles = new HashMap<>();

        boolean result = Wsdl2JavaCodeGen.wsdl2java(tempDir, inputDir, params, outDir,
                "quarkus.cxf.codegen.wsdl2java.test", processedFiles);

        assertThat(result).isTrue();

        // The WSDL defines service="IciSystemService" and portType="IciSystemServiceSoap"
        try (Stream<Path> files = Files.walk(outDir)) {
            List<String> generatedFiles = files
                    .filter(p -> p.toString().endsWith(".java"))
                    .map(p -> p.getFileName().toString())
                    .toList();

            assertThat(generatedFiles)
                    .as("Should generate Service class")
                    .contains("IciSystemService.java");

            assertThat(generatedFiles)
                    .as("Should generate PortType interface")
                    .contains("IciSystemServiceSoap.java");
        }
    }

    /**
     * The same WSDL processed with {@code validate = true} should catch the WS-I Basic Profile violation
     * (mixed binding style) and throw an exception.
     */
    @Test
    void validateDifferenceWithValidateFails() {
        Path inputDir = getTestResourcePath();
        Path outDir = tempDir.resolve("with-validate");

        Wsdl2JavaParameterSet params = proxy(
                "includes", Optional.of(List.of("ValidateDifference.wsdl")),
                "excludes", Optional.empty(),
                "outputDirectory", Optional.empty(),
                "packageNames", Optional.empty(),
                "serviceName", Optional.empty(),
                "bindings", Optional.empty(),
                "excludeNamespaceUris", Optional.empty(),
                "validate", Boolean.TRUE,
                "wsdlLocation", Optional.empty(),
                "xjc", Optional.empty(),
                "exceptionSuper", "java.lang.Exception",
                "asyncMethods", Optional.empty(),
                "bareMethods", Optional.empty(),
                "mimeMethods", Optional.empty(),
                "additionalParams", Optional.empty());

        Map<String, String> processedFiles = new HashMap<>();

        assertThatThrownBy(() -> Wsdl2JavaCodeGen.wsdl2java(tempDir, inputDir, params, outDir,
                "quarkus.cxf.codegen.wsdl2java.test", processedFiles))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not run wsdl2Java")
                .rootCause()
                .hasMessageContaining("Mixed style, invalid WSDL");
    }

    private Path getTestResourcePath() {
        try {
            return Path.of(Wsdl2JavaValidateTest.class.getResource("/wsdllocation").toURI());
        } catch (Exception e) {
            throw new RuntimeException("Could not find test resources at /wsdllocation", e);
        }
    }

}
