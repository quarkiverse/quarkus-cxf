package io.quarkiverse.it.cxf;

import javax.inject.Singleton;

@Singleton
public class HelloResource {
    public String getHello() {
        return "Hello ";
    }
}
