package io.quarkiverse.cxf.it.security.policy;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

/**
 * A service implementation with a transport policy set
 */
@WebService
public class UsernameTokenPolicyHelloServiceImpl implements UsernameTokenPolicyHelloService {

    @WebMethod
    @Override
    public String hello(String text) {
        return "Hello " + text + " from UsernameToken!";
    }

}
