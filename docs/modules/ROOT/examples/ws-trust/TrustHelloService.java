package io.quarkiverse.cxf.it.ws.trust.server;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.apache.cxf.annotations.Policies;
import org.apache.cxf.annotations.Policy;

//tag::ws-trust-usage.adoc-service[]
@WebService(targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-trust")
@Policy(placement = Policy.Placement.BINDING, uri = "classpath:/asymmetric-saml2-policy.xml")
public interface TrustHelloService {
    @WebMethod
    @Policies({
            @Policy(placement = Policy.Placement.BINDING_OPERATION_INPUT, uri = "classpath:/io-policy.xml"),
            @Policy(placement = Policy.Placement.BINDING_OPERATION_OUTPUT, uri = "classpath:/io-policy.xml")
    })
    String hello(String person);
}
//end::ws-trust-usage.adoc-service[]
