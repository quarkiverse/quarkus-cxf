package io.quarkiverse.it.cxf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.ws.Endpoint;

import io.quarkiverse.it.cxf.mock.AltCalculatorMockImpl;
import io.quarkiverse.it.cxf.mock.CalculatorMockImpl;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class MockWSTestResource implements QuarkusTestResourceLifecycleManager {

    private static final List<Endpoint> ENDPOINTS = new ArrayList<>();

    @Override
    public Map<String, String> start() {
        // server functionality automatically added by the "cxf-rt-transports-http-netty-server" jar
        String address = "http://localhost:9000/mockCalculator";
        CalculatorMockImpl implementor = new CalculatorMockImpl();

        String altAddress = "http://localhost:9000/mockAltCalculator";
        AltCalculatorMockImpl altImplementor = new AltCalculatorMockImpl();

        ENDPOINTS.add(Endpoint.publish(address, implementor));
        ENDPOINTS.add(Endpoint.publish(altAddress, altImplementor));

        return null;
    }

    @Override
    public void stop() {
        ENDPOINTS.forEach(Endpoint::stop);
    }

}
