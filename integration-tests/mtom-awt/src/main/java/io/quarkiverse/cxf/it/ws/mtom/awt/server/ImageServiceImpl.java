package io.quarkiverse.cxf.it.ws.mtom.awt.server;

import java.awt.Image;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jws.WebService;

@WebService(name = "ImageService", serviceName = "ImageService", endpointInterface = "io.quarkiverse.cxf.it.ws.mtom.awt.server.ImageService", targetNamespace = ImageService.NS)
public class ImageServiceImpl implements ImageService {

    public static final String MSG_SUCCESS = "Upload Successful";

    private final Map<String, Image> imageRepository = new ConcurrentHashMap<>();

    @Override
    public Image downloadImage(String name) {
        final Image image = imageRepository.get(name);
        if (image == null) {
            throw new IllegalStateException("Image with name " + name + " does not exist.");
        }
        return image;
    }

    @Override
    public String uploadImage(Image data, String name) {
        if (data != null && name != null) {
            imageRepository.put(name, data);
            return MSG_SUCCESS;
        }
        throw new IllegalStateException("Illegal Data Format.");
    }

}
