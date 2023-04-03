package io.quarkiverse.cxf.deployment.test.path;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class DefaultPathsTest extends AbstractCxfPathTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = createDeployment(null, null);

}
