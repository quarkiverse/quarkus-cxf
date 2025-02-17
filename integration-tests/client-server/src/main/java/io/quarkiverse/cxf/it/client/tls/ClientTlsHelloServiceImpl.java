package io.quarkiverse.cxf.it.client.tls;

import io.quarkiverse.cxf.it.HelloService;
import jakarta.jws.WebService;

@WebService
public class ClientTlsHelloServiceImpl implements HelloService {
    @Override
    public String hello(String person) {
        return "Hello from Client Tls, " + person;
    }
}
