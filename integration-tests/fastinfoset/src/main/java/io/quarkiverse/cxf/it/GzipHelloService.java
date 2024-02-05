package io.quarkiverse.cxf.it;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.apache.cxf.annotations.GZIP;

@WebService(serviceName = "HelloService", targetNamespace = GzipHelloService.NS)
@GZIP(force = true, threshold = 0)
public interface GzipHelloService {
    public static final String NS = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test";

    @WebMethod
    public String hello(String person);
}
