package io.quarkus.grpc.deployment;

import static java.lang.Boolean.TRUE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.deployment.CodeGenContext;
import io.quarkus.deployment.CodeGenProvider;
import io.quarkus.deployment.util.ProcessUtil;

/**
 * Code generation for Wsdl. Generates java classes from wsdl files placed in
 * either src/main/wsdl or src/test/wsdl
 */
public class WsdlCodeGen implements CodeGenProvider {
    private static final Logger log = Logger.getLogger(WsdlCodeGen.class);

    private static final String WSDL = ".wsdl";

    private Path wsdl2java;

    @Override
    public String providerId() {
        return "wsdl";
    }

    @Override
    public String inputExtension() {
        return "wsdl";
    }

    @Override
    public String inputDirectory() {
        return "wsdl";
    }

    @Override
    public boolean trigger(CodeGenContext context) throws CodeGenException {
        if (TRUE.toString().equalsIgnoreCase(System.getProperties().getProperty("wsdl.codegen.skip", "false"))
                || context.config().getOptionalValue("quarkus.wsdl.codegen.skip", Boolean.class).orElse(false)) {
            log.info("Skipping " + this.getClass() + " invocation on user's request");
            return false;
        }

        Path outDir = context.outDir();
        Path workDir = context.workDir();
        Set<String> wsdlDirs = new HashSet<>();

        try {
            List<String> wsdlFiles = new ArrayList<>();
            if (Files.isDirectory(context.inputDir())) {
                try (Stream<Path> wsdlFilesPaths = Files.walk(context.inputDir())) {
                    wsdlFilesPaths
                            .filter(Files::isRegularFile)
                            .filter(s -> s.toString().endsWith(WSDL))
                            .map(Path::normalize)
                            .map(Path::toAbsolutePath)
                            .map(Path::toString)
                            .forEach(wsdlFiles::add);
                    wsdlDirs.add(context.inputDir().normalize().toAbsolutePath().toString());
                }
            }

            if (!wsdlFiles.isEmpty()) {
                initExecutables(workDir, context.applicationModel());

                for (int i = 0; i < wsdlFiles.size(); i++) {
                    String wsdlFile = wsdlFiles.get(i);

                    List<String> command = new ArrayList<>();
                    command.add(wsdl2java.toString());

                    List<String> args = List.of("-d", outDir.toString(), wsdlFile);

                    command.addAll(args);

                    ProcessBuilder processBuilder = new ProcessBuilder(command);

                    final Process process = ProcessUtil.launchProcess(processBuilder, context.shouldRedirectIO());
                    int resultCode = process.waitFor();
                    if (resultCode != 0) {
                        throw new CodeGenException("Failed to generate Java classes from wsdl file: " + wsdlFile +
                                " to " + outDir.toAbsolutePath() + " with command " + String.join(" ", command));
                    }
                }

                log.info("Successfully finished generating and post-processing sources from wsdl files");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new CodeGenException(
                    "Failed to generate java files from wsdl file in " + context.inputDir().toAbsolutePath(), e);
        }

        return false;
    }

    private void initExecutables(Path workDir, ApplicationModel applicationModel) {
        if (wsdl2java == null) {
            String wsdl2javaPathProperty = System.getProperty("quarkus.cxf.wsdl2java-path");
            if (wsdl2javaPathProperty == null) {
                // download wsdl2java
            } else {
                wsdl2java = Paths.get(wsdl2javaPathProperty);
            }
        }
    }
}
