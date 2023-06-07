package io.quarkiverse.cxf.it.wss.client;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class CxfWssClientTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger log = LoggerFactory.getLogger(CxfWssClientTestResource.class);

    private static final int WILDFLY_PORT = 8080;
    private GenericContainer<?> calculatorContainer;

    @Override
    public Map<String, String> start() {

        final String user = "cxf-user";
        final String password = "secret-password";
        try {
            calculatorContainer = new GenericContainer<>("quay.io/l2x6/calculator-ws:1.3")
                    .withEnv("WSS_USER", user)
                    .withEnv("WSS_PASSWORD", password)
                    .withLogConsumer(new Slf4jLogConsumer(log))
                    .withExposedPorts(WILDFLY_PORT)
                    .waitingFor(Wait.forHttp("/calculator-ws/CalculatorService?wsdl"));

            calculatorContainer.start();

            return Map.of(
                    "cxf.it.calculator.baseUri",
                    "http://" + calculatorContainer.getHost() + ":" + calculatorContainer.getMappedPort(WILDFLY_PORT),
                    "wss.username", user,
                    "wss.password", password);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (calculatorContainer != null) {
                calculatorContainer.stop();
            }
        } catch (Exception e) {
            // ignored
        }
    }
}
