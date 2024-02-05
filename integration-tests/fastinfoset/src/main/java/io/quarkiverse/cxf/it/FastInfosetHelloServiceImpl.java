package io.quarkiverse.cxf.it;

import jakarta.jws.WebService;

@WebService(serviceName = "HelloService", targetNamespace = FastInfosetHelloService.NS)
public class FastInfosetHelloServiceImpl implements FastInfosetHelloService {
    @Override
    public String hello(String person) {
        return "Hello " + person + "!";
    }
}
