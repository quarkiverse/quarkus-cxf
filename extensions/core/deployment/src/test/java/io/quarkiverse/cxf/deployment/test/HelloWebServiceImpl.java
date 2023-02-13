package io.quarkiverse.cxf.deployment.test;

import jakarta.jws.WebService;

@WebService(endpointInterface = "io.quarkiverse.cxf.deployment.test.GreetingWebService", serviceName = "GreetingWebService")
public class HelloWebServiceImpl implements GreetingWebService {
    @Override
    public String hello() {
        return "hello world from HelloWebServiceImpl";
    }

    @Override
    public void ping() {
    }
}
