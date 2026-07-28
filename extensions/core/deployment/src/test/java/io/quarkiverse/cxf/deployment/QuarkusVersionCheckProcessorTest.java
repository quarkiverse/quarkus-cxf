package io.quarkiverse.cxf.deployment;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class QuarkusVersionCheckProcessorTest {

    @Test
    void majorMinor() {
        Assertions.assertThat(QuarkusVersionCheckProcessor.majorMinor("3.38.1")).isEqualTo("3.38");
        Assertions.assertThat(QuarkusVersionCheckProcessor.majorMinor("3.38.1-SNAPSHOT")).isEqualTo("3.38");
        Assertions.assertThat(QuarkusVersionCheckProcessor.majorMinor("3.38.0.CR1")).isEqualTo("3.38");
        Assertions.assertThat(QuarkusVersionCheckProcessor.majorMinor("3.38")).isEqualTo("3.38");
        Assertions.assertThat(QuarkusVersionCheckProcessor.majorMinor("999-SNAPSHOT")).isNull();
        Assertions.assertThat(QuarkusVersionCheckProcessor.majorMinor("whatever")).isNull();
        Assertions.assertThat(QuarkusVersionCheckProcessor.majorMinor("")).isNull();
        Assertions.assertThat(QuarkusVersionCheckProcessor.majorMinor(null)).isNull();
    }

    @Test
    void parseBuiltWithQuarkusCore() {
        Assertions.assertThat(QuarkusVersionCheckProcessor.parseBuiltWithQuarkusCore("---\n"
                + "artifact: \"io.quarkiverse.cxf:quarkus-cxf:3.38.1\"\n"
                + "metadata:\n"
                + "  built-with-quarkus-core: \"3.38.0\"\n"
                + "  requires-quarkus-core: \"[3.38,)\"\n")).isEqualTo("3.38.0");
        Assertions.assertThat(QuarkusVersionCheckProcessor.parseBuiltWithQuarkusCore(
                "built-with-quarkus-core: 3.38.0\n")).isEqualTo("3.38.0");
        Assertions.assertThat(QuarkusVersionCheckProcessor.parseBuiltWithQuarkusCore(
                "requires-quarkus-core: \"[3.38,)\"\n")).isNull();
    }

    @Test
    void checkVersionsMatching() {
        Assertions.assertThat(QuarkusVersionCheckProcessor.checkVersions("3.38.1", "3.38.0", "3.38.0")).isNull();
        /* Micro versions and qualifiers are ignored */
        Assertions.assertThat(QuarkusVersionCheckProcessor.checkVersions("3.38.1", "3.38.0", "3.38.5")).isNull();
        Assertions.assertThat(QuarkusVersionCheckProcessor.checkVersions("3.38.1", "3.38.0", "3.38.1-SNAPSHOT")).isNull();
    }

    @Test
    void checkVersionsNotParseable() {
        Assertions.assertThat(QuarkusVersionCheckProcessor.checkVersions("3.38.1", null, "3.38.0")).isNull();
        Assertions.assertThat(QuarkusVersionCheckProcessor.checkVersions("3.38.1", "999-SNAPSHOT", "3.38.0")).isNull();
        Assertions.assertThat(QuarkusVersionCheckProcessor.checkVersions("3.38.1", "3.38.0", "999-SNAPSHOT")).isNull();
    }

    @Test
    void checkVersionsMismatching() {
        Assertions.assertThat(QuarkusVersionCheckProcessor.checkVersions("3.38.1", "3.38.0", "3.20.2"))
                .isEqualTo("Quarkus CXF 3.38.1 was built with Quarkus 3.38.0"
                        + " but the current build is using Quarkus 3.20.2."
                        + " Combining different Quarkus version streams (3.38 and 3.20)"
                        + " is not supported and may lead to obscure build time or runtime failures."
                        + " Please fix the dependency management of your project as described in"
                        + " https://docs.quarkiverse.io/quarkus-cxf/dev/user-guide/create-project.html#dependency-management"
                        + " or set quarkus.cxf.on-quarkus-version-mismatch = warn or ignore in application.properties"
                        + " if you know what you are doing.");
        Assertions.assertThat(QuarkusVersionCheckProcessor.checkVersions("3.38.1", "3.38.0", "3.39.0")).isNotNull();
        Assertions.assertThat(QuarkusVersionCheckProcessor.checkVersions("3.38.1", "3.38.0", "4.38.0")).isNotNull();
    }
}
