package io.quarkiverse.cxf.it.security.policy;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.apache.cxf.annotations.Policy;

/**
 * A service with a WS-SecurityPolicy attached, security policy is customAlgorithmSuite with default values
 */
@WebService(serviceName = "CustomEncryptSignWrong2PolicyHelloService")
@Policy(placement = Policy.Placement.BINDING, uri = "custom-encrypt-sign-wrong2-policy.xml")
public interface CustomEncryptSignWrong2PolicyHelloService extends AbstractHelloService {
    @WebMethod
    @Override
    String hello(String text);
}
