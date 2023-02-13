package io.quarkiverse.cxf.it.ws.mtom.awt.server;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.xml.ws.soap.MTOM;

@WebService(name = "ImageService", targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/mtom-awt")
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT, parameterStyle = SOAPBinding.ParameterStyle.BARE)
@MTOM
public interface ImageService {

    @WebMethod
    ImageData downloadImage(String name);

    @WebMethod
    String uploadImage(ImageData image);

}
