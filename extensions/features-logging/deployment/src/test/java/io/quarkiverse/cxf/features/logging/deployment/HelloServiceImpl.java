package io.quarkiverse.cxf.features.logging.deployment;

import javax.jws.WebMethod;
import javax.jws.WebService;

/**
 * The simplest Hello service implementation.
 */
@WebService(serviceName = "HelloService")
public class HelloServiceImpl implements HelloService {

    @WebMethod
    @Override
    public String hello(String text) {
        return "Hello " + text + "!";
    }

}
