package io.quarkiverse.cxf.it.ws.rm.client;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

public class WsrmServer implements Closeable {
    private static final Logger log = Logger.getLogger(WsrmServer.class);
    private Process serverProcess;
    private Thread outputSlurper;
    private volatile boolean stopped = false;

    public WsrmServer(boolean isNative) {

        String mavenLocalRepo = System.getProperty("maven.repo.local");
        if (mavenLocalRepo == null) {
            mavenLocalRepo = System.getProperty("user.home") + "/.m2/repository";
        }
        String quarkusCxfVersion = System.getProperty("quarkus-cxf.version");
        if (quarkusCxfVersion == null) {
            URL pomPropsUrl = this.getClass().getClassLoader()
                    .getResource("META-INF/maven/io.quarkiverse.cxf/quarkus-cxf-integration-test-ws-rm-client/pom.properties");
            if (pomPropsUrl != null) {
                try (InputStream in = pomPropsUrl.openStream()) {
                    Properties props = new Properties();
                    props.load(in);
                    quarkusCxfVersion = props.getProperty("version");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        if (quarkusCxfVersion == null) {
            throw new IllegalStateException("quarkus-cxf.version property not set");
        }
        final Path localRepo = Paths.get(mavenLocalRepo);

        final Path serverLog = Paths.get("target/ws-rm-server-" + (isNative ? "native" : "jvm") + ".log");
        final List<String> cmd = cmd(isNative, quarkusCxfVersion, localRepo, serverLog);
        log.infof("Starting quarkus-cxf-test-ws-rm-server: %s", cmd.stream().collect(Collectors.joining(" ")));
        try {
            serverProcess = new ProcessBuilder()
                    .command(cmd)
                    .redirectErrorStream(true)
                    .start();

            /* Unless we slurp the process output, the server app will eventually freeze on Windows */
            outputSlurper = new Thread(() -> {
                try (InputStream in = serverProcess.getInputStream()) {
                    while (!stopped && in.read() >= 0) {
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            outputSlurper.start();
        } catch (IOException e) {
            throw new RuntimeException(cmd.stream().collect(Collectors.joining(" ")), e);
        }

        awaitStarted(serverLog, "Installed features: [", 10_000L);
    }

    private List<String> cmd(boolean isNative, final String quarkusCxfVersion, final Path localRepo, final Path serverLog) {
        if (isNative) {
            final Path binPath = localRepo
                    .resolve("io/quarkiverse/cxf/quarkus-cxf-test-ws-rm-server-native/" + quarkusCxfVersion
                            + "/quarkus-cxf-test-ws-rm-server-native-" + quarkusCxfVersion + ".exe");

            if (!Files.isRegularFile(binPath)) {
                throw new RuntimeException(binPath.toString()
                        + " does not exist. Have you run `mvn install -Pnative` for quarkus-cxf-test-ws-rm-server module?");
            }
            if (!Files.isExecutable(binPath)) {
                try {
                    Files.setPosixFilePermissions(
                            binPath,
                            EnumSet.allOf(PosixFilePermission.class));
                } catch (IOException e) {
                    throw new RuntimeException("Could not set executable permissions for " + binPath);
                }
            }

            final List<String> cmd = List.of(
                    binPath.toString(),
                    "-Dquarkus.log.file.enable=true",
                    "-Dquarkus.log.file.path=" + serverLog.toString());
            return cmd;
        } else {
            final Path jarPath = localRepo
                    .resolve("io/quarkiverse/cxf/quarkus-cxf-test-ws-rm-server-jvm/" + quarkusCxfVersion
                            + "/quarkus-cxf-test-ws-rm-server-jvm-" + quarkusCxfVersion + "-runner.jar");

            if (!Files.isRegularFile(jarPath)) {
                throw new RuntimeException(jarPath.toString()
                        + " does not exist. Have you run `mvn install` for quarkus-cxf-test-ws-rm-server module?");
            }

            final Path javaHome = Paths.get(System.getProperty("java.home"));

            final List<String> cmd = List.of(
                    javaHome.resolve("bin/java" + (System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : ""))
                            .toString(),
                    "-Dquarkus.log.file.enable=true",
                    "-Dquarkus.log.file.path=" + serverLog.toString(),
                    "-jar",
                    jarPath.toString());
            return cmd;
        }
    }

    private static void awaitStarted(Path serverLog, String startedString, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!Files.isRegularFile(serverLog) && System.currentTimeMillis() < deadline) {
            /* Wait for the file to appear */
        }
        if (!Files.isRegularFile(serverLog)) {
            throw new RuntimeException("Timed out waiting for " + startedString + " in " + serverLog);
        }
        try (BufferedReader reader = Files.newBufferedReader(serverLog, StandardCharsets.UTF_8)) {
            while (true) {
                if (reader.ready()) { // avoid blocking as the input is a file which continually gets more data added
                    String line = reader.readLine();
                    if (line.contains(startedString)) {
                        return;
                    }
                } else {
                    if (System.currentTimeMillis() >= deadline) {
                        throw new RuntimeException("Timed out waiting for " + startedString + " in " + serverLog);
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted waiting for " + startedString + " in " + serverLog);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read " + serverLog, e);
        }
    }

    @Override
    public void close() {
        serverProcess.destroy();
        stopped = true;
        if (outputSlurper != null) {
            try {
                outputSlurper.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
