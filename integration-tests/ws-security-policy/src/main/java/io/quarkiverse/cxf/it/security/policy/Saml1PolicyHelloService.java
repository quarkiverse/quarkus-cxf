package io.quarkiverse.cxf.it.security.policy;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.apache.cxf.annotations.Policy;

/**
 */
@WebService(serviceName = "Saml1PolicyHelloService")
@Policy(placement = Policy.Placement.BINDING, uri = "saml1-policy.xml")
public interface Saml1PolicyHelloService extends AbstractHelloService {
    @WebMethod
    @Override
    String hello(String text);
}
