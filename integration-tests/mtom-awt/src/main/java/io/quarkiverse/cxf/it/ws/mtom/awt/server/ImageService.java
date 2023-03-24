package io.quarkiverse.cxf.it.ws.mtom.awt.server;

import java.awt.Image;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.xml.ws.soap.MTOM;

@WebService(name = "ImageService", targetNamespace = ImageService.NS)
@MTOM
public interface ImageService {

    public static final String NS = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/mtom-awt";

    @WebMethod
    Image downloadImage(
            @WebParam(name = "name", targetNamespace = NS) String name);

    @WebMethod
    String uploadImage(
            @WebParam(name = "data", targetNamespace = NS) Image data,
            @WebParam(name = "name", targetNamespace = NS) String name);

}
