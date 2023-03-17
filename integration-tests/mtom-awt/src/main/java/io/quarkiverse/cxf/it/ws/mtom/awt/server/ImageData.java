package io.quarkiverse.cxf.it.ws.mtom.awt.server;

import java.awt.Image;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ImageData", propOrder = {
        "data",
        "name"
}, namespace = ImageServiceWithWrappers.NS)
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