package io.quarkiverse.cxf.it.security.policy;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.apache.cxf.annotations.Policy;

/**
 */
@WebService(serviceName = "EncryptSignPolicyHelloService")
@Policy(placement = Policy.Placement.BINDING, uri = "encrypt-sign-policy.xml")
public interface EncryptSignPolicyHelloService extends AbstractHelloService {
    @WebMethod
    @Override
    String hello(String text);
}
