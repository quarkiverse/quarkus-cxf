package io.quarkiverse.cxf.deployment.test;

import javax.jws.WebService;

@WebService(endpointInterface = "io.quarkiverse.cxf.deployment.test.GreetingWebService", serviceName = "GreetingWebService")
public class GreetingWebServiceImpl implements GreetingWebService {
    @Override
    public String hello() {
        return "hello world";
    }

    @Override
    public void ping() {
    }
}
