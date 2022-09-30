package io.quarkiverse.cxf.it.ws.trust.server;

import javax.jws.WebMethod;
import javax.jws.WebService;

import org.apache.cxf.annotations.Policies;
import org.apache.cxf.annotations.Policy;

@WebService(targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-trust")
@Policy(placement = Policy.Placement.BINDING, uri = "classpath:/AsymmetricSAML2Policy.xml")
public interface TrustHelloService {
    @WebMethod
    @Policies({
            @Policy(placement = Policy.Placement.BINDING_OPERATION_INPUT, uri = "classpath:/Input_Policy.xml"),
            @Policy(placement = Policy.Placement.BINDING_OPERATION_OUTPUT, uri = "classpath:/Output_Policy.xml")
    })
    String sayHello();
}
