package io.quarkiverse.cxf.it.ws.mtom.awt.server;

import java.awt.Image;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ImageData", propOrder = {
        "data",
        "name"
}, namespace = io.quarkiverse.cxf.it.ws.mtom.awt.server.ImageService.NS)
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