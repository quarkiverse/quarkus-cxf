package io.quarkiverse.cxf.it.ws.mtom.awt.server.wrappers;

import java.awt.Image;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.xml.ws.RequestWrapper;
import jakarta.xml.ws.ResponseWrapper;
import jakarta.xml.ws.soap.MTOM;

@WebService(name = "ImageServiceWithWrappers", targetNamespace = ImageServiceWithWrappers.NS)
@MTOM
public interface ImageServiceWithWrappers {

    public static final String NS = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/mtom-awt-with-wrappers";

    @WebMethod
    @ResponseWrapper(localName = "ImageResponse", targetNamespace = NS, className = "io.quarkiverse.cxf.it.ws.mtom.awt.server.wrappers.ImageResponse")
    Image downloadImage(
            @WebParam(name = "name", targetNamespace = NS) String name);

    @WebMethod
    @RequestWrapper(localName = "ImageData", targetNamespace = NS, className = "io.quarkiverse.cxf.it.ws.mtom.awt.server.wrappers.ImageData")
    String uploadImage(
            @WebParam(name = "data", targetNamespace = NS) Image data,
            @WebParam(name = "name", targetNamespace = NS) String name);

}
