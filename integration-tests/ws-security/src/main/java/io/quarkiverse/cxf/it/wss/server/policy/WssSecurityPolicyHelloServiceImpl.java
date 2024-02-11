package io.quarkiverse.cxf.it.wss.server.policy;

import jakarta.jws.WebService;

import org.apache.cxf.annotations.EndpointProperties;
import org.apache.cxf.annotations.EndpointProperty;
import org.apache.cxf.annotations.Policy;

@WebService(portName = "EncryptSecurityServicePort", serviceName = "WssSecurityPolicyHelloService", targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/ws-securitypolicy")
@Policy(placement = Policy.Placement.BINDING, uri = "server/encrypt-sign-policy.xml")
@EndpointProperties(value = {
        @EndpointProperty(key = "ws-security.signature.properties", value = "server/bob.properties"),
        @EndpointProperty(key = "ws-security.encryption.properties", value = "server/bob.properties"),
        @EndpointProperty(key = "ws-security.signature.username", value = "bob"),
        @EndpointProperty(key = "ws-security.encryption.username", value = "alice"),
        @EndpointProperty(key = "ws-security.callback-handler", value = "io.quarkiverse.cxf.it.wss.server.policy.PasswordCallbackHandler")
})
public class WssSecurityPolicyHelloServiceImpl implements WssSecurityPolicyHelloService {

    @Override
    public String sayHello(String name) {
        return "Secure Hello " + name + "!";
    }
}
