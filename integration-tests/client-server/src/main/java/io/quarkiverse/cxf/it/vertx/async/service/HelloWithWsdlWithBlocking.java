package io.quarkiverse.cxf.it.vertx.async.service;

import java.util.concurrent.Future;

import jakarta.jws.WebService;
import jakarta.xml.ws.AsyncHandler;
import jakarta.xml.ws.Response;

import io.quarkiverse.cxf.annotation.CXFEndpoint;
import io.quarkiverse.cxf.deployment.test.HelloResponse;
import io.quarkiverse.cxf.deployment.test.HelloService;

@WebService(serviceName = "HelloService", targetNamespace = "http://test.deployment.cxf.quarkiverse.io/")
@CXFEndpoint("/helloWithWsdlWithBlocking")
public class HelloWithWsdlWithBlocking implements HelloService {

    @Override
    public String hello(String person) {
        return "Hello from " + this.getClass().getSimpleName() + " " + person;
    }

    @Override
    public Response<HelloResponse> helloAsync(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<?> helloAsync(String arg0, AsyncHandler<HelloResponse> asyncHandler) {
        throw new UnsupportedOperationException();
    }
}
