package io.quarkiverse.cxf.it.ws.mtom.awt.server;

import java.awt.Image;

import jakarta.xml.bind.annotation.XmlType;

@XmlType(name = "imageData", namespace = "http://org.jboss.ws/xop/doclit")
public class ImageData {

    private Image data;
    private String name;

    public ImageData() {
    }

    public ImageData(Image data, String name) {
        super();
        this.data = data;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Image getData() {
        return data;
    }

    public void setData(Image data) {
        this.data = data;
    }
}