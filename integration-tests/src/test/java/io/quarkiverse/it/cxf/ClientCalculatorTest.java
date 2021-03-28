package io.quarkiverse.it.cxf;

import static io.restassured.RestAssured.given;

import java.lang.reflect.Proxy;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tempuri.CalculatorSoap;
import org.tempuri.alt.AltCalculatorSoap;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Test WS (proxy) clients to external WS endpoints.
 * <p>
 * See MockWSTestResource, AltCalculatorMockImpl and CalculatorMockImpl for details.
 */
@QuarkusTest
class ClientCalculatorTest {

    @Inject
    @CXFClient("mockCalculator")
    CalculatorSoap calculatorWS;

    @Inject
    @CXFClient
    AltCalculatorSoap altCalculatorWS;

    @Inject
    @Named("org.tempuri.CalculatorSoap")
    CXFClientInfo calculatorInfo;

    @Inject
    @Named("org.tempuri.alt.AltCalculatorSoap")
    CXFClientInfo altCalculatorInfo;

    @Test
    public void test_clients_injected() {
        Assertions.assertNotNull(calculatorWS);
        Assertions.assertNotNull(altCalculatorWS);
    }

    @Test
    public void test_proxies_injected() {
        Assertions.assertTrue(Proxy.isProxyClass(calculatorWS.getClass()));
        Assertions.assertTrue(Proxy.isProxyClass(altCalculatorWS.getClass()));
    }

    @Test
    public void test_infos_injected() {
        Assertions.assertNotNull(calculatorInfo);
        Assertions.assertNotNull(altCalculatorInfo);
    }

    @Test
    public void test_default_ep_address() {
        Assertions.assertEquals(
                "http://localhost:8080/org.tempuri.calculatorsoap",
                this.calculatorInfo.getEndpointAddress());
        Assertions.assertEquals(
                "http://localhost:8080/org.tempuri.alt.altcalculatorsoap",
                this.altCalculatorInfo.getEndpointAddress());
    }

    @Test
    public void test_active_ep_address() {
        /* Too bad - there is no way of retrieving this information */
    }

    @Test
    public void test_calculator_wsdl_available() {
        given().port(9000)
                .when().get("/mockCalculator?wsdl")
                .then().statusCode(200);
    }

    @Test
    public void test_alt_calculator_wsdl_available() {
        given().port(9000)
                .when().get("/mockAltCalculator?wsdl")
                .then().statusCode(200);
    }

    @Test
    public void testMultiply() {
        Assertions.assertEquals(221, calculatorWS.multiply(13, 17));
        Assertions.assertEquals(221, altCalculatorWS.multiply(13, 17));
    }

    @Test
    public void testAdd() {
        Assertions.assertEquals(42, calculatorWS.add(19, 23));
        Assertions.assertEquals(42, altCalculatorWS.add(23, 19));
    }

}
