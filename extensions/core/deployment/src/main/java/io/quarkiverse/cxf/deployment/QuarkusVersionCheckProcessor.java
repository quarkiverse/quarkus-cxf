package io.quarkiverse.cxf.deployment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.quarkus.builder.Version;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * Checks that the Quarkus version used by the application does not differ from the Quarkus version Quarkus CXF was
 * built with.
 */
class QuarkusVersionCheckProcessor {
    private static final Logger LOGGER = Logger.getLogger(QuarkusVersionCheckProcessor.class);

    private static final String QUARKUS_CXF_GROUP_ID = "io.quarkiverse.cxf";
    private static final String QUARKUS_CXF_ARTIFACT_ID = "quarkus-cxf";
    private static final String QUARKUS_EXTENSION_YAML = "META-INF/quarkus-extension.yaml";
    private static final Pattern BUILT_WITH_QUARKUS_CORE_PATTERN = Pattern
            .compile("built-with-quarkus-core:\\s*\"?([^\"\\s]+)\"?");
    private static final Pattern MAJOR_MINOR_PATTERN = Pattern.compile("^(\\d+\\.\\d+)(?:[.\\-].*)?$");

    @BuildStep
    @Produce(ServiceStartBuildItem.class)
    void checkQuarkusVersion(CxfBuildTimeConfig config, CurateOutcomeBuildItem curateOutcome) {
        final ErrorRemedy action = config.onQuarkusVersionMismatch();
        if (action == ErrorRemedy.IGNORE) {
            return;
        }
        final ResolvedDependency quarkusCxf = curateOutcome.getApplicationModel().getDependencies().stream()
                .filter(d -> QUARKUS_CXF_GROUP_ID.equals(d.getGroupId())
                        && QUARKUS_CXF_ARTIFACT_ID.equals(d.getArtifactId()))
                .findFirst()
                .orElse(null);
        if (quarkusCxf == null) {
            LOGGER.debugf("Could not find %s:%s among the application dependencies; skipping the Quarkus version check",
                    QUARKUS_CXF_GROUP_ID, QUARKUS_CXF_ARTIFACT_ID);
            return;
        }
        final String extensionYaml = quarkusCxf.getContentTree().apply(QUARKUS_EXTENSION_YAML, visit -> {
            if (visit == null) {
                return null;
            }
            try {
                return Files.readString(visit.getPath());
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "Could not read " + QUARKUS_EXTENSION_YAML + " from " + visit.getUrl(), e);
            }
        });
        final String expectedQuarkusVersion = extensionYaml == null ? null : parseBuiltWithQuarkusCore(extensionYaml);
        final String errorMessage = checkVersions(quarkusCxf.getVersion(), expectedQuarkusVersion, Version.getVersion());
        if (errorMessage != null) {
            if (action == ErrorRemedy.FAIL) {
                throw new IllegalStateException(errorMessage);
            }
            LOGGER.warn(errorMessage);
        }
    }

    /**
     * @param quarkusCxfVersion the version of the {@code io.quarkiverse.cxf:quarkus-cxf} artifact found among the
     *        application dependencies
     * @param expectedQuarkusVersion the Quarkus version Quarkus CXF was built with
     * @param actualQuarkusVersion the Quarkus version used by the current application build
     * @return an error message describing the version mismatch or {@code null} if the versions match or if any of the
     *         versions could not be determined
     */
    static String checkVersions(String quarkusCxfVersion, String expectedQuarkusVersion, String actualQuarkusVersion) {
        if (expectedQuarkusVersion == null) {
            LOGGER.debugf("Could not determine the Quarkus version Quarkus CXF was built with;"
                    + " skipping the Quarkus version check");
            return null;
        }
        final String expectedMinorVersion = majorMinor(expectedQuarkusVersion);
        final String actualMinorVersion = majorMinor(actualQuarkusVersion);
        if (expectedMinorVersion == null || actualMinorVersion == null) {
            LOGGER.debugf("Could not parse the major and minor parts of the Quarkus versions %s and %s;"
                    + " skipping the Quarkus version check", expectedQuarkusVersion, actualQuarkusVersion);
            return null;
        }
        if (expectedMinorVersion.equals(actualMinorVersion)) {
            return null;
        }
        return "Quarkus CXF " + quarkusCxfVersion + " was built with Quarkus " + expectedQuarkusVersion
                + " but the current build is using Quarkus " + actualQuarkusVersion
                + ". Combining different Quarkus version streams (" + expectedMinorVersion + " and " + actualMinorVersion
                + ") is not supported and may lead to obscure build time or runtime failures."
                + " Please fix the dependency management of your project as described in"
                + " https://docs.quarkiverse.io/quarkus-cxf/dev/user-guide/create-project.html#dependency-management"
                + " or set quarkus.cxf.on-quarkus-version-mismatch = warn or ignore in application.properties"
                + " if you know what you are doing.";
    }

    /**
     * @param extensionYaml the content of a {@code META-INF/quarkus-extension.yaml} file
     * @return the value of the {@code built-with-quarkus-core} attribute or {@code null} if it could not be found
     */
    static String parseBuiltWithQuarkusCore(String extensionYaml) {
        final Matcher m = BUILT_WITH_QUARKUS_CORE_PATTERN.matcher(extensionYaml);
        return m.find() ? m.group(1) : null;
    }

    /**
     * @param version a version string, such as {@code 3.38.1} or {@code 999-SNAPSHOT}
     * @return the {@code major.minor} part of the given {@code version} or {@code null} if the given {@code version}
     *         does not start with a {@code major.minor} prefix
     */
    static String majorMinor(String version) {
        if (version == null) {
            return null;
        }
        final Matcher m = MAJOR_MINOR_PATTERN.matcher(version);
        return m.matches() ? m.group(1) : null;
    }
}
