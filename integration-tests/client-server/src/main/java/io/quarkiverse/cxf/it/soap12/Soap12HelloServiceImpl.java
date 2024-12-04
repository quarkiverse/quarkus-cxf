package io.quarkiverse.cxf.it.soap12;

import jakarta.annotation.Resource;
import jakarta.jws.WebService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.handler.MessageContext;

import io.quarkiverse.cxf.it.HelloService;

@WebService(serviceName = "HelloService", targetNamespace = HelloService.NS)
public class Soap12HelloServiceImpl implements HelloService {

    @Resource
    WebServiceContext wsContext;

    @Override
    public String hello(String person) {
        final HttpServletRequest req = (HttpServletRequest) wsContext.getMessageContext().get(MessageContext.SERVLET_REQUEST);
        return "Hello " + person + ", Content-Type: " + req.getHeader("Content-Type");
    }

}
