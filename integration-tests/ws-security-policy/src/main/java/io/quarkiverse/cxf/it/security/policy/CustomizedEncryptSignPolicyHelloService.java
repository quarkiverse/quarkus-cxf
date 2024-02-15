package io.quarkiverse.cxf.it.security.policy;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.apache.cxf.annotations.Policy;

/**
 * A service with a WS-SecurityPolicy attached, security policy is customAlgorithmSuite, values are changed by
 * application.properties.
 */
@WebService(serviceName = "CustomizedEncryptSignPolicyHelloService")
@Policy(placement = Policy.Placement.BINDING, uri = "custom-encrypt-sign-policy.xml")
public interface CustomizedEncryptSignPolicyHelloService extends AbstractHelloService {
    @WebMethod
    @Override
    String hello(String text);
}
