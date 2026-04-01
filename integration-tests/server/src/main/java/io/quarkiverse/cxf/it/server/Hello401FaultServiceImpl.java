package io.quarkiverse.cxf.it.server;

import jakarta.annotation.Resource;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceContext;

import org.apache.cxf.interceptor.Fault;

/**
 * The simplest Hello service implementation.
 */
@WebService(serviceName = "HelloService")
@io.quarkiverse.cxf.annotation.CXFEndpoint("/hello-401-fault")
public class Hello401FaultServiceImpl implements HelloService {

    @Resource
    WebServiceContext ctx;

    @WebMethod
    @Override
    public String hello(String text) {
        Fault fault = new Fault(new RuntimeException("Unauthorized"));
        fault.setStatusCode(401);
        throw fault;
    }

}
