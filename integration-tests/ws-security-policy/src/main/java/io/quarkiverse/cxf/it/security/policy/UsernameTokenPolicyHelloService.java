package io.quarkiverse.cxf.it.security.policy;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.apache.cxf.annotations.Policy;

/**
 * The simplest Hello service.
 */
@WebService(serviceName = "UsernameTokenPolicyHelloService")
@Policy(placement = Policy.Placement.BINDING, uri = "username-token-policy.xml")
public interface UsernameTokenPolicyHelloService extends AbstractHelloService {
    @WebMethod
    @Override
    String hello(String text);
}
