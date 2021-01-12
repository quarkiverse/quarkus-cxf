package io.quarkiverse.it.cxf.mock;

import javax.jws.WebService;

import org.tempuri.alt.AltCalculatorSoap;

/**
 * Mock implementation of {@link AltCalculatorSoap}. We
 * do not implement the interface because that would break the injection of
 * clients for that interface. Instead we use the
 * {@link WebService#endpointInterface} attribute.
 */
@WebService(targetNamespace = "http://alt.tempuri.org/", endpointInterface = "org.tempuri.alt.AltCalculatorSoap", serviceName = "AltCalculator", portName = "AltCalculatorSoap")
public class AltCalculatorMockImpl {

    public int subtract(int intA, int intB) {
        return intA - intB;
    }

    public int divide(int intA, int intB) {
        return intA / intB;
    }

    public Integer add(Integer intA, Integer intB) {
        return intA + intB;
    }

    public int multiply(int intA, int intB) {
        return intA * intB;
    }

}
