package io.quarkiverse.cxf.it.server;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "ping", namespace = "http://server.it.cxf.quarkiverse.io/")
@XmlType(name = "ping", namespace = "http://server.it.cxf.quarkiverse.io/")
public class Ping {
    private String text;

    public Ping() {
    }

    @XmlElement(name = "text", namespace = "")
    public String getText() {
        return this.text;
    }

    public void setText(String var1) {
        this.text = var1;
    }
}
