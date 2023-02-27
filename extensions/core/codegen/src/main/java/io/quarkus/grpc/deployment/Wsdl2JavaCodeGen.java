package io.quarkus.grpc.deployment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.wsdlto.WSDLToJava;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.deployment.CodeGenContext;
import io.quarkus.deployment.CodeGenProvider;

/**
 * Code generation for WSDL. Generates java classes from WSDL files placed in
 * either {@code src/main/wsdl} or {@code src/test/wsdl}
 */
public class Wsdl2JavaCodeGen implements CodeGenProvider {
    private static final Logger log = Logger.getLogger(Wsdl2JavaCodeGen.class);

    private static final String WSDL = ".wsdl";

    @Override
    public String providerId() {
        return "wsdl2java";
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
        if (context.config().getOptionalValue("quarkus.cxf.wsdl.codegen.skip", Boolean.class).orElse(false)) {
            log.info("Skipping " + this.getClass() + " invocation on user's request");
            return false;
        }

        Path outDir = context.outDir();

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
                }
            }

            if (!wsdlFiles.isEmpty()) {

                for (int i = 0; i < wsdlFiles.size(); i++) {
                    String wsdlFile = wsdlFiles.get(i);

                    List<String> args = List.of("-d", outDir.toString(), wsdlFile);

                    ToolContext ctx = new ToolContext();
                    new WSDLToJava(args.toArray(new String[0])).run(ctx);
                }

                log.info("Successfully finished generating and post-processing sources from wsdl files");
                return true;
            }
        } catch (Exception e) {
            throw new CodeGenException(
                    "Failed to generate java files from wsdl file in " + context.inputDir().toAbsolutePath(), e);
        }

        return false;
    }

}
