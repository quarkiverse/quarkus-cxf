package io.quarkiverse.cxf.it.ws.mtom.awt.server;

import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest

// Native test fails on Mac OS with:
// java.lang.UnsatisfiedLinkError: no awt in java.library.path
// https://github.com/oracle/graal/issues/4124
@DisabledOnOs({ OS.MAC })
class MtomAwtIT extends MtomAwtTest {

}
