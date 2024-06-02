package io.quarkiverse.cxf.it.annotation.cxfendpoint;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HelloBean {
    public String hello(String person) {
        return "Hello " + person + " from HelloBean!";
    }
}
