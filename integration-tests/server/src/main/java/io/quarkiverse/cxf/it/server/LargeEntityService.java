package io.quarkiverse.cxf.it.server;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;

@WebService(serviceName = "LargeEntityService", name = "LargeEntityService")
public interface LargeEntityService {

    @WebMethod
    int outputBufferSize();

    @WebMethod
    String[] items(
            @WebParam(name = "count") int count,
            @WebParam(name = "itemLength") int itemLength);

}
