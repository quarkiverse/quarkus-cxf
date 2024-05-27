package io.quarkiverse.cxf.it.ws.mtom.awt.server.wrappers;

import java.awt.Image;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlMimeType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ImageData", propOrder = {
        "data",
        "name"
}, namespace = ImageServiceWithWrappers.NS)
public class ImageData {

    @XmlElement(required = true)
    @XmlMimeType("image/png")
    private Image data;
    @XmlElement(required = true)
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
