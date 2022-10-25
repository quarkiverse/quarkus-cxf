package io.quarkiverse.cxf.it.ws.mtom.awt.server;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.soap.MTOM;

@WebService(name = "ImageService", targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/mtom-awt")
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT, parameterStyle = SOAPBinding.ParameterStyle.BARE)
@MTOM
public interface ImageService {

    @WebMethod
    ImageData downloadImage(String name);

    @WebMethod
    String uploadImage(ImageData image);

}
