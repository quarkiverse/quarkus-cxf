package io.quarkiverse.cxf.it.nsprefix;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;

import org.assertj.core.api.Assertions;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import io.quarkiverse.cxf.it.HelloService;
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
    void contextualNamespaceMap() throws Exception {
        /*
         * The contextual namespace map tells JAXB that the given prefixes are declared by the context
         * into which the payload is marshalled.
         * The endpoint declares the hello prefix on the SOAP envelope via the soap.env.ns.map endpoint property.
         * Therefore the hello prefix is used in the payload, but it is not declared there again.
         * CXF passes the contextual namespace map to the namespace mapper via reflection.
         */
        final String response = RestAssured.given()
                .header("Content-Type", "text/xml").body(SOAP_REQUEST)
                .post("/soap/contextual-namespace-map")
                .then()
                .statusCode(200)
                .extract().body().asString();
        Assertions.assertThat(response).isEqualTo(
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\""
                        + " xmlns:hello=\"https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test\">"
                        + "<soap:Body>"
                        + "<hello:helloResponse>"
                        + "<return>Hello Joe!</return>"
                        + "</hello:helloResponse>"
                        + "</soap:Body>"
                        + "</soap:Envelope>");

        /* Make sure that the response can be parsed and that the hello prefix resolves to the expected namespace */
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        final Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(response)));
        Assertions.assertThat(doc.getElementsByTagNameNS(HelloService.NS, "helloResponse").getLength()).isEqualTo(1);
    }
}
