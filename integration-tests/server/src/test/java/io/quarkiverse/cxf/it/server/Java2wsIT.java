package io.quarkiverse.cxf.it.server;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class Java2wsIT extends Java2wsTest {

    /** A pseudo-workaround for https://github.com/quarkiverse/quarkus-cxf/issues/819 */
    protected String normalizeNsPrefixes(String servedWsdl) {
        return servedWsdl
                .replace("ns1", "soap")
                .replace("xmlns:ns2=\"http://schemas.xmlsoap.org/soap/http\"", "");
    }

}
