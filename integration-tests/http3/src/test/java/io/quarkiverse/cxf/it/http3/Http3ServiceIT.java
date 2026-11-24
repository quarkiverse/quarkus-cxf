package io.quarkiverse.cxf.it.http3;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * A variant of {@link Http3ServiceTest} run against the packaged application, typically a native executable.
 *
 * @since 4.0.0
 */
@QuarkusIntegrationTest
public class Http3ServiceIT extends Http3ServiceTest {
}
