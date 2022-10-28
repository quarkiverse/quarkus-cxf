package io.quarkiverse.cxf.it.ws.mtom.awt.server;

import java.awt.Image;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ImageResponse", propOrder = {
        "_return"
}, namespace = io.quarkiverse.cxf.it.ws.mtom.awt.server.ImageService.NS)
public class ImageResponse {

    @XmlElement(required = true)
    @XmlMimeType("image/png")
    private Image _return;

    public ImageResponse() {
    }

    public ImageResponse(Image data) {
        super();
        this._return = data;
    }

    public Image getReturn() {
        return _return;
    }

    public void setReturn(Image data) {
        this._return = data;
    }
}