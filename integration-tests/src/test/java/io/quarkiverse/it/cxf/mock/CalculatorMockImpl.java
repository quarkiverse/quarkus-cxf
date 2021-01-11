package io.quarkiverse.it.cxf.mock;

import javax.jws.WebService;

/**
 * Mock implementation of {@link org.tempuri.CalculatorSoap CalculatorSoap}. We
 * do not implement the interface because that would break the injection of
 * clients for that interface. Instead we use the
 * {@link WebService#endpointInterface} attribute.
 */
@WebService(targetNamespace = "http://tempuri.org/", endpointInterface = "org.tempuri.Calculator", serviceName = "Calculator", portName = "CalculatorSoap")
public class CalculatorMockImpl {

    public int subtract(int intA, int intB) {
        return intA - intB;
    }

    public int divide(int intA, int intB) {
        return intA / intB;
    }

    public int add(int intA, int intB) {
        return intA + intB;
    }

    public int multiply(int intA, int intB) {
        return intA * intB;
    }

}
