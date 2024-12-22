package io.quarkiverse.cxf.it.ws.mtom.server;

import java.io.IOException;

import jakarta.activation.DataHandler;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

import org.jboss.logging.Logger;

@WebService(name = "MtomService", serviceName = "MtomService", endpointInterface = "io.quarkiverse.cxf.it.ws.mtom.server.MtomService", targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/mtom")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public class MtomServiceImpl implements MtomService {
    private Logger log = Logger.getLogger(MtomServiceImpl.class);

    @WebMethod
    @Override
    public DHResponse echoDataHandler(DHRequest request) {

        DataHandler dataHandler = request.getDataHandler();

        try {
            int length = RandomBytesDataSource.count(dataHandler.getInputStream());
            log.infof("Received %d bytes of content type %s", length, dataHandler.getContentType());

            /*
             * We do not send back the original bytes, because we do not want to keep them in memory.
             * We mainly care for testing the transport
             */
            DataHandler responseData = new DataHandler(new RandomBytesDataSource(length));
            return new DHResponse(responseData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
