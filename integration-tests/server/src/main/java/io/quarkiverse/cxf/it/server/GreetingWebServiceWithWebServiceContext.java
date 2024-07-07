package io.quarkiverse.cxf.it.server;

import jakarta.annotation.Resource;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceContext;

import io.quarkiverse.cxf.annotation.CXFEndpoint;

@WebService(serviceName = "GreetingWebService", endpointInterface = "io.quarkiverse.cxf.it.server.GreetingWebService")
@CXFEndpoint("/greeting-with-web-service-context")
public class GreetingWebServiceWithWebServiceContext {

    @Resource
    WebServiceContext ctx;

    public String reply(@WebParam(name = "text") String key) {
        return String.valueOf(ctx.getMessageContext().get(key));
    }

}
