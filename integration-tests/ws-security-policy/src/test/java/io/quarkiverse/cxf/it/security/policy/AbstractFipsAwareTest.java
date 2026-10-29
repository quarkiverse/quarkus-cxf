package io.quarkiverse.cxf.it.security.policy;

import java.io.IOException;

import org.assertj.core.api.Assertions;

import io.restassured.RestAssured;

public abstract class AbstractFipsAwareTest {

    private Boolean _fips;

    /**
     * Fails if the machine is in FIPS mode but the application is not, because such a run would not test FIPS.
     * It happens with a non-FIPS JDK or, in native mode, with a binary built by a non-FIPS JDK (e.g. Temurin).
     * A FIPS application on a non-FIPS machine is allowed.
     */
    void assertFipsModeOnFipsMachine() throws IOException {
        if (PolicyTestUtils.isFipsLocalEnabled() && !isFipsEnabled()) {
            Assertions.fail("The machine is in FIPS mode, but the application is not."
                    + " Use a JDK in FIPS mode or, in native mode, a builder image with a JDK in FIPS mode.");
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
