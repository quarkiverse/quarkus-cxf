package io.quarkiverse.it.cxf;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngineFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkiverse.it.cxf.mock.AltCalculatorMockImpl;
import io.quarkiverse.it.cxf.mock.CalculatorMockImpl;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ClientFacadeResourceTest {

    private static final List<Endpoint> ENDPOINTS = new ArrayList<>();

    @BeforeAll
    public static void setUpClass() throws GeneralSecurityException, IOException {
        Bus defaultBus = BusFactory.getDefaultBus();
        new JettyHTTPServerEngineFactory(defaultBus).createJettyHTTPServerEngine(9000, "http");

        String address = "http://localhost:9000/mockCalculator";
        CalculatorMockImpl implementor = new CalculatorMockImpl();

        String altAddress = "http://localhost:9000/mockAltCalculator";
        AltCalculatorMockImpl altImplementor = new AltCalculatorMockImpl();

        ENDPOINTS.add(Endpoint.publish(address, implementor));
        ENDPOINTS.add(Endpoint.publish(altAddress, altImplementor));
    }

    @AfterAll
    public static void tearDownClass() {
        ENDPOINTS.forEach(
                Endpoint::stop);
    }

    @Test
    public void testMultiply() {
        given().param("a", 13).param("b", 17)
                .when().get("/rest/clientfacade/multiply")
                .then().statusCode(200).body(is("221"));
    }

    @Test
    public void testAdd() {
        given().param("a", 19).param("b", 23)
                .when().get("/rest/clientfacade/add")
                .then().statusCode(200).body(is("42"));
    }

}
