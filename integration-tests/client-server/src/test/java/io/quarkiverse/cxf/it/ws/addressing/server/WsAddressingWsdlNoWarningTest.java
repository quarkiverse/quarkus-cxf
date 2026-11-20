package io.quarkiverse.cxf.it.ws.addressing.server;

import static io.restassured.RestAssured.given;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Reproducer for <a href="https://github.com/quarkiverse/quarkus-cxf/issues/2432">#2432</a>:
 * <code>GET endpoint?wsdl</code> of a service whose binding contains <code>wsaw:UsingAddressing</code>
 * logs <code>NamespaceWrapper not found : java.lang.NullPointerException</code>.
 */
@QuarkusTest
public class WsAddressingWsdlNoWarningTest {

    @Test
    void wsdlDoesNotLogNamespaceWrapperWarning() {
        final Logger logger = Logger.getLogger("org.apache.cxf.common.spi.ClassLoaderProxyService");
        final List<String> warnings = new CopyOnWriteArrayList<>();
        final Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                    final Throwable t = record.getThrown();
                    warnings.add(record.getMessage() + (t != null ? " " + t : ""));
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        logger.addHandler(handler);
        try {
            given()
                    .when().get("/soap/addressing-anonymous?wsdl")
                    .then()
                    .statusCode(200);
        } finally {
            logger.removeHandler(handler);
        }
        Assertions.assertTrue(warnings.isEmpty(),
                "Unexpected warnings from ClassLoaderProxyService: " + warnings);
    }
}
