package io.quarkiverse.cxf.it.security.policy;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.apache.cxf.annotations.Policy;

/**
 *
 */
// tag::ws-securitypolicy-auth.adoc[]
@WebService(serviceName = "UsernameTokenPolicyHelloService")
@Policy(placement = Policy.Placement.BINDING, uri = "username-token-policy.xml")
public interface UsernameTokenPolicyHelloService extends AbstractHelloService {
    // end::ws-securitypolicy-auth.adoc[]
    @WebMethod
    @Override
    String hello(String text);
}
