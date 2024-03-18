package io.quarkiverse.cxf.it.security.policy;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.apache.cxf.annotations.Policy;

/**
 * A service with a WS-SecurityPolicy attached, security policy is customAlgorithmSuite with default values
 */
@WebService(serviceName = "CustomizedEncryptSignPolicyHelloService")
@Policy(placement = Policy.Placement.BINDING, uri = "customized-encrypt-sign-policy.xml")
public interface CustomizedEncryptSignPolicyHelloService extends AbstractHelloService {
    @WebMethod
    @Override
    String hello(String text);
}
