package io.quarkiverse.cxf.it.security.policy;

import java.io.IOException;

import org.assertj.core.api.Assertions;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.RestAssured;

public abstract class AbstractFipsAwareTest {

    private Boolean _fips;

    void failFipsInNative() throws IOException {
        //if machine where tuest runs is FIPS enabled and this is native fun, fail the execution
        if (this.getClass().isAnnotationPresent(QuarkusIntegrationTest.class) &&
                (PolicyTestUtils.isFipsLocalEnabled() || isFipsEnabled())) {
            //native binary itself has to be FIPS compliant and has to be created on FIPS machine to make native test FIPS compliant
            // because of that, the native tests even with FIPS compliant binaries should fail with the corresponding message
            //therefore the condition asks for local FIPS environment (not for the binary)
            Assertions.fail("Combination of FIPS environment and native mode is not supported.");
        }

    }

    boolean isFipsEnabled() {
        if (_fips == null) {
            _fips = Boolean.valueOf(RestAssured.get("/cxf/security-policy/isfips")
                    .then()
                    .statusCode(200)
                    .extract().body().asString());
        }

        return _fips;
    }
}
