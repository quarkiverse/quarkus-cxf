package io.quarkiverse.cxf.it.security.policy;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

@WebService
public class Saml2PolicyHelloServiceImpl implements Saml2PolicyHelloService {

    @WebMethod
    @Override
    public String hello(String text) {
        return "Hello " + text + " from helloSaml2!";
    }

}
