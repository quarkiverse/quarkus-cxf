package io.quarkiverse.cxf.it.server.xml.schema.validation;

import jakarta.jws.WebService;

import org.apache.cxf.annotations.SchemaValidation;

import io.quarkiverse.cxf.it.server.xml.schema.validation.model.CalculatorService;

/* Enable XML Schema validation for both incoming and outgoing messages */
@SchemaValidation
/*
 * An alternative way of enabling XML Schema validation:
 *
 * @org.apache.cxf.annotations.EndpointProperty(key = "schema-validation-enabled", value = "true")
 */
@WebService(serviceName = "CalculatorService", targetNamespace = "http://www.jboss.org/eap/quickstarts/wscalculator/Calculator")
public class AnnotationSchemaValidatedCalculatorServiceImpl implements CalculatorService {

    @Override
    public int addCheckResult(int a, int b) {
        return a + b;
    }

    @Override
    public int addCheckParameters(int a, int b) {
        return a + b;
    }

}
