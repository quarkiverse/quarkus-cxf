package io.quarkiverse.cxf.deployment.test.path;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class QuarkusRootPathTest extends AbstractCxfPathTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = createDeployment("/quarkus-root-path", null);

}
