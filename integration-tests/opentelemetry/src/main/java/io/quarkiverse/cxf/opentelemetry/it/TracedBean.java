package io.quarkiverse.cxf.opentelemetry.it;

import jakarta.enterprise.context.ApplicationScoped;

import io.opentelemetry.instrumentation.annotations.WithSpan;

@ApplicationScoped
public class TracedBean {

    @WithSpan("TracedBean.helloTracedSpan")
    public String helloTraced(String person) {
        try {
            /* We have to slow down a bit so that the native test is able to see some elapsedTime */
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Hello traced " + person;
    }
}
