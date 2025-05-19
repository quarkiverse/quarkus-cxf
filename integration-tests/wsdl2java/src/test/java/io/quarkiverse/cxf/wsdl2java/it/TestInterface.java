package io.quarkiverse.cxf.wsdl2java.it;

public interface TestInterface {
    default String hello() {
        return "Hello!";
    }
}
