package io.quarkiverse.cxf.metrics.it.async;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.jboss.eap.quickstarts.wscalculator.calculator.CalculatorService;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(AsyncClientTestResource.class)
class AsyncClientContextlessTest {

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

        final CountDownLatch latch = new CountDownLatch(1);
        myCalculator.addAsync(5, 7, response -> {
            try {
                Assertions.assertThat(response.get().getReturn()).isEqualTo(12);
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        latch.await(10, TimeUnit.SECONDS);
    }
}
