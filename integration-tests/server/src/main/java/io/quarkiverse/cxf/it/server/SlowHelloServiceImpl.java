package io.quarkiverse.cxf.it.server;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import io.quarkiverse.cxf.annotation.CXFEndpoint;

@WebService(serviceName = "HelloService")
@CXFEndpoint("/SlowHelloServiceImpl")
public class SlowHelloServiceImpl implements HelloService {

    @WebMethod
    @Override
    public String hello(String text) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        return "Hello Slow " + text + "!";
    }

}
