package io.quarkiverse.cxf.it.security.policy;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

@WebService
public class Saml1PolicyHelloServiceImpl implements Saml1PolicyHelloService {

    @WebMethod
    @Override
    public String hello(String text) {
        return "Hello " + text + " from helloSaml1!";
    }

}
