package io.quarkiverse.cxf.it.server.xml.schema.validation;

import jakarta.jws.WebService;

import io.quarkiverse.cxf.it.server.xml.schema.validation.model.CalculatorService;

@WebService(serviceName = "CalculatorService", targetNamespace = "http://www.jboss.org/eap/quickstarts/wscalculator/Calculator")
public class ApplicationPropertiesSchemaValidatedCalculatorServiceImpl implements CalculatorService {

    @Override
    public int addCheckResult(int a, int b) {
        return a + b;
    }

    @Override
    public int addCheckParameters(int a, int b) {
        return a + b;
    }

}
