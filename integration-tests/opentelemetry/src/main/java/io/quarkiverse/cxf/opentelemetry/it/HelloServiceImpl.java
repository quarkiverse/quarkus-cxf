package io.quarkiverse.cxf.opentelemetry.it;

import jakarta.inject.Inject;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;

/**
 * The simplest Hello service implementation.
 */
@WebService(serviceName = "HelloService")
public class HelloServiceImpl implements HelloService {

    @Inject
    TracedBean traced;

    @WebMethod
    @Override
    public String hello(@WebParam(name = "text") String text) {
        try {
            /* We have to slow down a bit so that the native test is able to see some elapsedTime */
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Hello " + text + "!";
    }

    @WebMethod
    @Override
    public String helloTraced(@WebParam(name = "text") String text) {
        return traced.helloTraced(text);
    }

}
