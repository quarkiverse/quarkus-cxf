package io.quarkiverse.cxf.wsdl2java.no.config.it;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class Wsdl2JavaNoConfigTest {

    @Test
    void codegenTests() throws IOException {

        /* Make sure that no java files were generated */
        final Path wsdl2java = Paths.get(
                "target/generated-test-sources/wsdl2java");
        Assertions.assertThat(wsdl2java).doesNotExist();

    }

}
