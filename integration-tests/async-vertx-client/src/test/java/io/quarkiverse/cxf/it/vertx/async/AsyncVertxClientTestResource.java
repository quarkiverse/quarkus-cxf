package io.quarkiverse.cxf.it.vertx.async;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class AsyncVertxClientTestResource implements QuarkusTestResourceLifecycleManager {

    private static final int WILDFLY_PORT = 8080;
    private final List<GenericContainer<?>> calculatorContainers = new ArrayList<>();

    @SuppressWarnings("resource")
    @Override
    public Map<String, String> start() {
        final Map<String, String> result = new LinkedHashMap<>();
        try {
            for (int i = 0; i < 2; i++) {
                GenericContainer<?> calculatorContainer = new GenericContainer<>("quay.io/l2x6/calculator-ws:1.3")
                        .withExposedPorts(WILDFLY_PORT)
                        .waitingFor(Wait.forHttp("/calculator-ws/CalculatorService?wsdl"));
                calculatorContainer.start();

                result.put("cxf.it.calculator.baseUri" + i,
                        "http://" + calculatorContainer.getHost() + ":" + calculatorContainer.getMappedPort(WILDFLY_PORT));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("result " + result);
        return Collections.unmodifiableMap(result);
    }

    @Override
    public void stop() {
        for (GenericContainer<?> calculatorContainer : calculatorContainers) {
            try {
                if (calculatorContainer != null) {
                    calculatorContainer.stop();
                }
            } catch (Exception e) {
                // ignored
            }
        }
    }
}
