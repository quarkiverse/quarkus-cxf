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
 * logged <code>NamespaceWrapper not found : java.lang.NullPointerException</code>.
 * The test makes sure that no CXF class logs a warning when the WSDL is served.
 */
@QuarkusTest
public class WsAddressingWsdlNoWarningTest {

    @Test
    void wsdlDoesNotLogNamespaceWrapperWarning() {
        /*
         * The warning was originally logged by org.apache.cxf.common.spi.ClassLoaderProxyService,
         * which is not registered by QuarkusBusFactory anymore.
         * Therefore we listen on the org.apache.cxf parent logger,
         * so that warnings from any CXF class on the ?wsdl path are caught.
         */
        final Logger logger = Logger.getLogger("org.apache.cxf");
        final List<String> warnings = new CopyOnWriteArrayList<>();
        final Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                /*
                 * This method is called by the thread that logs the record.
                 * We ignore the await-application-readiness thread started by RestAsyncWithWsdlWithEagerInit,
                 * because it initializes an unrelated client in the background right after the application start
                 * and CXF logs a warning about a port not found in the WSDL of that client.
                 */
                if (record.getLevel().intValue() >= Level.WARNING.intValue()
                        && !"await-application-readiness".equals(Thread.currentThread().getName())) {
                    final Throwable t = record.getThrown();
                    warnings.add(record.getLoggerName() + ": " + record.getMessage() + (t != null ? " " + t : ""));
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
                "Unexpected warnings from org.apache.cxf loggers: " + warnings);
    }
}
