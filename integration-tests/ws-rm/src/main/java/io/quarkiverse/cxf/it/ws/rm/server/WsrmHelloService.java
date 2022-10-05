package io.quarkiverse.cxf.it.ws.rm.server;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.soap.Addressing;

import org.apache.cxf.interceptor.InInterceptors;

@WebService(targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-rm")
@Addressing(required = true)
@InInterceptors(interceptors = { "io.quarkiverse.cxf.it.ws.rm.server.RMStoreCheckInterceptor" })
public interface WsrmHelloService {
    @WebMethod
    String sayHello();
}
