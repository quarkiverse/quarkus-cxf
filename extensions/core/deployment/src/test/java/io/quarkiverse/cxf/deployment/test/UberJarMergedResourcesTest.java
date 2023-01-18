package io.quarkiverse.cxf.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class UberJarMergedResourcesTest {

    @RegisterExtension
    static final QuarkusProdModeTest runner = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(UberJarMain.class))
            .setApplicationName("uber-jar")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setRun(true)
            .setExpectExit(true)
            .overrideConfigKey("quarkus.package.type", "uber-jar")
            .setLogRecordPredicate(r -> "io.quarkiverse.cxf.deployment.QuarkusCxfProcessor".equals(r.getLoggerName()) ||
                    "io.quarkus.deployment.pkg.steps.JarResultBuildStep".equals(r.getLoggerName()));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertUberJarResourcesMergedSuccessfully() {
        List<LogRecord> buildLogRecords = prodModeTestResults.getRetainedBuildLogRecords();

        // Verify that the uber-jar build was initiated
        assertThat(buildLogRecords)
                .filteredOn(r -> r.getMessage().contains("Building uber jar"))
                .hasSize(1);

        // Message will show up in the logs in case of any error that leads to a merge failure for wsdl.plugin.xml
        assertThat(buildLogRecords)
                .filteredOn(r -> r.getMessage().contains("cannot merge wsdl.plugin.xml"))
                .isEmpty();

        // Message will show up in the logs in case of any error that leads to a merge failure for bus-extensions.txt
        assertThat(buildLogRecords)
                .filteredOn(r -> r.getMessage().contains("cannot merge bus-extensions.txt"))
                .isEmpty();

        assertThat(runner.getExitCode()).isZero();
    }

    @QuarkusMain
    public static class UberJarMain {
        public static void main(String... args) {
            // System.out.println("Started uber-jar successfully");
        }
    }
}
