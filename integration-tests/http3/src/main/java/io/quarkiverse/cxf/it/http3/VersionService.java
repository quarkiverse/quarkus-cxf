package io.quarkiverse.cxf.it.http3;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

@WebService(targetNamespace = "https://quarkiverse.io/cxf/http3")
public interface VersionService {
    @WebMethod
    String inspect(String payload);
}
