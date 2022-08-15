package io.quarkiverse.it.cxf;

import javax.inject.Singleton;

/**
 * A simple bean for testing whether injecting into service implementation works.
 */
@Singleton
public class HelloBean {
    public String getHello() {
        return "Hello ";
    }
}
