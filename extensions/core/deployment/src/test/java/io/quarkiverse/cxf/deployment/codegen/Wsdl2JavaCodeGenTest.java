package io.quarkiverse.cxf.deployment.codegen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class Wsdl2JavaCodeGenTest {

    @Test
    void scanRecursive() throws IOException {
        final Path tempDir = createTestTree();
        final List<Path> foundFiles = new ArrayList<>();
        Wsdl2JavaCodeGen.scan(
                tempDir,
                Optional.of(List.of("**.wsdl")),
                Optional.empty(),
                "",
                new HashMap<>(),
                foundFiles::add);

        Assertions.assertThat(foundFiles).containsExactlyInAnyOrder(
                tempDir.resolve("foo.wsdl"),
                tempDir.resolve("bar.wsdl"),
                tempDir.resolve("dir/foo.wsdl"),
                tempDir.resolve("dir/dir/foo.wsdl"));
    }

    @Test
    void scanFlat() throws IOException {
        final Path tempDir = createTestTree();
        final List<Path> foundFiles = new ArrayList<>();
        Wsdl2JavaCodeGen.scan(
                tempDir,
                Optional.of(List.of("*.wsdl")),
                Optional.empty(),
                "",
                new HashMap<>(),
                foundFiles::add);

        Assertions.assertThat(foundFiles).containsExactlyInAnyOrder(
                tempDir.resolve("foo.wsdl"),
                tempDir.resolve("bar.wsdl"));
    }

    @Test
    void scanRecursiveExcudes() throws IOException {
        final Path tempDir = createTestTree();
        final List<Path> foundFiles = new ArrayList<>();
        Wsdl2JavaCodeGen.scan(
                tempDir,
                Optional.of(List.of("*.wsdl")),
                Optional.of(List.of("**foo.wsdl")),
                "",
                new HashMap<>(),
                foundFiles::add);

        Assertions.assertThat(foundFiles).containsExactlyInAnyOrder(
                tempDir.resolve("bar.wsdl"));
    }

    @Test
    void scanSubdir() throws IOException {
        final Path tempDir = createTestTree();
        final List<Path> foundFiles = new ArrayList<>();
        Wsdl2JavaCodeGen.scan(
                tempDir,
                Optional.of(List.of("dir/*.wsdl")),
                Optional.of(List.of("dir/dir/*")),
                "",
                new HashMap<>(),
                foundFiles::add);

        Assertions.assertThat(foundFiles).containsExactlyInAnyOrder(
                tempDir.resolve("dir/foo.wsdl"));
    }

    @Test
    void scanNoWildcards() throws IOException {
        final Path tempDir = createTestTree();
        final List<Path> foundFiles = new ArrayList<>();
        Wsdl2JavaCodeGen.scan(
                tempDir,
                Optional.of(List.of("dir/foo.wsdl")),
                Optional.empty(),
                "",
                new HashMap<>(),
                foundFiles::add);

        Assertions.assertThat(foundFiles).containsExactlyInAnyOrder(
                tempDir.resolve("dir/foo.wsdl"));
    }

    @Test
    void scanOverlappingSelections() throws IOException {
        final Path tempDir = createTestTree();
        final List<Path> foundFiles = new ArrayList<>();

        final Map<String, String> files = new HashMap<String, String>();
        Wsdl2JavaCodeGen.scan(
                tempDir,
                Optional.of(List.of("**.wsdl")),
                Optional.empty(),
                io.quarkiverse.cxf.deployment.codegen.Wsdl2JavaCodeGen.WSDL2JAVA_CONFIG_KEY_PREFIX,
                files,
                foundFiles::add);

        Assertions.assertThat(foundFiles).hasSizeGreaterThan(0);

        Assertions
                .assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> Wsdl2JavaCodeGen.scan(
                        tempDir,
                        Optional.of(List.of("dir/foo.wsdl")),
                        Optional.empty(),
                        io.quarkiverse.cxf.deployment.codegen.Wsdl2JavaCodeGen.WSDL2JAVA_CONFIG_KEY_PREFIX + ".my-name",
                        files,
                        foundFiles::add))
                .withMessageContaining("Ensure that the individual include/exclude sets are mutually exclusive");

    }

    private Path createTestTree() throws IOException {
        final Path tempDir = Files.createTempDirectory(Wsdl2JavaCodeGenTest.class.getSimpleName() + ".scan");
        Stream.of("foo.wsdl", "bar.wsdl", "dir/foo.wsdl", "dir/dir/foo.wsdl")
                .map(tempDir::resolve)
                .forEach(absPath -> {
                    try {
                        Files.createDirectories(absPath.getParent());
                    } catch (IOException e) {
                        throw new RuntimeException("Could not create " + absPath.getParent(), e);
                    }
                    try {
                        Files.write(absPath, new byte[0]);
                    } catch (IOException e) {
                        throw new RuntimeException("Could not write to " + absPath, e);
                    }
                });
        Files.createDirectories(tempDir.resolve("baz.wsdl"));
        return tempDir;
    }

}
