package io.quarkiverse.cxf.hc5.it;

import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.jboss.eap.quickstarts.wscalculator.calculator.CalculatorService;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(Hc5TestResource.class)
class Hc5ContextlessTest {

    @Inject
    @CXFClient("myCalculator")
    CalculatorService myCalculator;

    /**
     * Make sure that the client still works when called from a thread where the request context is not set
     *
     * @throws InterruptedException
     */
    @Test
    void contextless() throws InterruptedException {
        Assertions.assertThat(myCalculator.add(5, 5)).isEqualTo(10);
        myCalculator.addAsync(5, 7, response -> {
            try {
                Assertions.assertThat(response.get().getReturn()).isEqualTo(12);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
