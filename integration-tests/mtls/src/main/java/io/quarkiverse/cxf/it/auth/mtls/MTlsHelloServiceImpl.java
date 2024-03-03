package io.quarkiverse.cxf.it.auth.mtls;

import jakarta.jws.WebService;

@WebService
public class MTlsHelloServiceImpl implements HelloService {
    @Override
    public String hello(String person) {
        return "Hello " + person + " authenticated by mTLS!";
    }
}
