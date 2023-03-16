package io.quarkiverse.cxf.it.ws.mtom.awt.server;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;

import javax.imageio.ImageIO;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkiverse.cxf.annotation.CXFClient;

@Path("/mtom-awt-rest")
@ApplicationScoped
public class MtomAwtResource {

    @Inject
    @CXFClient("imageServiceClient")
    ImageService imageServiceClient;

    @Path("/image/{imageName}")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    public Response upload(@PathParam("imageName") String imageName, InputStream in) throws Exception {
        String response = imageServiceClient.uploadImage(ImageIO.read(in), imageName);
        return Response
                .created(new URI("https://quarkus.io/"))
                .entity(response)
                .build();
    }

    @Path("/image/{imageName}")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    public byte[] download(@PathParam("imageName") String imageName) throws Exception {
        java.awt.Image image = imageServiceClient.downloadImage(imageName);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write((BufferedImage) image, "png", baos);
            return baos.toByteArray();
        }
    }

}
