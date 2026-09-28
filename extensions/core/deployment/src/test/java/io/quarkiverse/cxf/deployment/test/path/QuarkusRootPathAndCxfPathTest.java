package io.quarkiverse.cxf.deployment.test.path;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class QuarkusRootPathAndCxfPathTest extends AbstractCxfPathTest {

    @RegisterExtension
    public static final QuarkusExtensionTest test = createDeployment("/quarkus-root-path", "/cxf-endpoints");

}
