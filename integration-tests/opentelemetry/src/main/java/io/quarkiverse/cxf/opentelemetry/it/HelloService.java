package io.quarkiverse.cxf.opentelemetry.it;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;

/**
 * The simplest Hello service.
 */
@WebService(name = "HelloService", serviceName = "HelloService")
public interface HelloService {

    @WebMethod
    String hello(@WebParam(name = "text") String text);

    @WebMethod
    String helloTraced(@WebParam(name = "text") String text);

}
