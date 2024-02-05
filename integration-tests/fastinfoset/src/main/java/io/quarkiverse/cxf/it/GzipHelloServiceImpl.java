package io.quarkiverse.cxf.it;

import jakarta.jws.WebService;

import org.apache.cxf.annotations.GZIP;
import org.apache.cxf.ext.logging.Logging;

@WebService(serviceName = "HelloService", targetNamespace = FastInfosetHelloService.NS)
@GZIP(force = true, threshold = 0)
@Logging(logBinary = true)
public class GzipHelloServiceImpl implements GzipHelloService {
    @Override
    public String hello(String person) {
        return "Hello " + person + "!";
    }
}
