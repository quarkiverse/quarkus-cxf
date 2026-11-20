package io.quarkiverse.cxf.it.nsprefix;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Make sure that the namespace prefixes set on the data binding are honored when marshalling SOAP payloads.
 * See <a href="https://github.com/quarkiverse/quarkus-cxf/issues/2432">#2432</a>.
 */
@QuarkusTest
class NamespacePrefixTest {
    static final String SOAP_REQUEST = """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Body>
                <ns2:hello xmlns:ns2="https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test">
                  <arg0>Joe</arg0>
                </ns2:hello>
              </soap:Body>
            </soap:Envelope>
            """;

    @Test
    void namespaceMap() {
        RestAssured.given()
                .header("Content-Type", "text/xml").body(SOAP_REQUEST)
                .post("/soap/namespace-map")
                .then()
                .statusCode(200)
                .body(CoreMatchers.containsString(
                        "<hello:helloResponse xmlns:hello=\"https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test\">"
                                + "<return>Hello Joe!</return>"
                                + "</hello:helloResponse>"));
    }

    @Test
    void contextualNamespaceMap() {
        /*
         * The contextual namespace map tells JAXB that the given prefixes are declared by the context
         * into which the payload is marshalled.
         * Therefore the hello prefix is used, but not declared.
         * CXF passes the contextual namespace map to the namespace mapper via reflection.
         */
        RestAssured.given()
                .header("Content-Type", "text/xml").body(SOAP_REQUEST)
                .post("/soap/contextual-namespace-map")
                .then()
                .statusCode(200)
                .body(CoreMatchers.containsString(
                        "<hello:helloResponse>"
                                + "<return>Hello Joe!</return>"
                                + "</hello:helloResponse>"));
    }
}
