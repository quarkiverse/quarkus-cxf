package io.quarkiverse.cxf.it.ws.mtom.server;

import java.io.IOException;

import javax.activation.DataHandler;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.jboss.logging.Logger;

@WebService(name = "MtomService", serviceName = "MtomService", endpointInterface = "io.quarkiverse.cxf.it.ws.mtom.server.MtomService", targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/mtom")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public class MtomServiceImpl implements MtomService {
    private Logger log = Logger.getLogger(MtomServiceImpl.class);

    @WebMethod
    @Override
    public DHResponse echoDataHandler(DHRequest request) {

        DataHandler dataHandler = request.getDataHandler();

        log.infof("Received content type %s", dataHandler.getContentType());
        try {
            String message = (String) dataHandler.getContent();
            log.infof("Received content %s", message);

            DataHandler responseData = new DataHandler(message + " echoed from the server", "text/plain");
            return new DHResponse(responseData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
