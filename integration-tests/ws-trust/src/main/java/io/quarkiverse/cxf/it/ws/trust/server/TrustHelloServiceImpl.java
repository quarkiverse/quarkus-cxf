package io.quarkiverse.cxf.it.ws.trust.server;

import javax.jws.WebMethod;
import javax.jws.WebService;

import org.apache.cxf.annotations.EndpointProperties;
import org.apache.cxf.annotations.EndpointProperty;

@WebService(portName = "TrustHelloServicePort", serviceName = "TrustHelloService", wsdlLocation = "TrustHelloService.wsdl", targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-trust", endpointInterface = "io.quarkiverse.cxf.it.ws.trust.server.TrustHelloService")
@EndpointProperties(value = {
        @EndpointProperty(key = "ws-security.signature.username", value = "myservicekey"),
        @EndpointProperty(key = "ws-security.signature.properties", value = "serviceKeystore.properties"),
        @EndpointProperty(key = "ws-security.encryption.properties", value = "serviceKeystore.properties"),
        @EndpointProperty(key = "ws-security.callback-handler", value = "io.quarkiverse.cxf.it.ws.trust.server.ServerCallbackHandler")
})
public class TrustHelloServiceImpl implements TrustHelloService {
    @WebMethod
    public String sayHello() {
        return "WS-Trust Hello World!";
    }
}
