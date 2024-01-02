package io.quarkiverse.cxf.deployment.test.dev;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

@WebService(serviceName = "DevUiStats")
public interface DevUiStats {

    @WebMethod
    int getClientCount();

    @WebMethod
    String getClient(int index);

    @WebMethod
    int getServiceCount();

    @WebMethod
    String getService(int index);

}