package io.quarkiverse.cxf.it.ws.mtom.awt.server;

import java.awt.Image;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;
import javax.xml.ws.soap.MTOM;

@WebService(name = "ImageService", targetNamespace = ImageService.NS)
@MTOM
public interface ImageService {

    public static final String NS = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/mtom-awt";

    @WebMethod
    @ResponseWrapper(localName = "ImageResponse", targetNamespace = NS, className = "io.quarkiverse.cxf.it.ws.mtom.awt.server.ImageResponse")
    Image downloadImage(
            @WebParam(name = "name", targetNamespace = NS) String name);

    @WebMethod
    @RequestWrapper(localName = "ImageData", targetNamespace = NS, className = "io.quarkiverse.cxf.it.ws.mtom.awt.server.ImageData")
    String uploadImage(
            @WebParam(name = "data", targetNamespace = NS) Image data,
            @WebParam(name = "name", targetNamespace = NS) String name);

}
