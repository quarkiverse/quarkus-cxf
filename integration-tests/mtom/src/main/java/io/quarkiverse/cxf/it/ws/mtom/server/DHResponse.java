package io.quarkiverse.cxf.it.ws.mtom.server;

import jakarta.activation.DataHandler;
import jakarta.xml.bind.annotation.XmlMimeType;
import jakarta.xml.bind.annotation.XmlType;

@XmlType(name = "dataResponse", namespace = "http://org.jboss.ws/xop/doclit")
public class DHResponse {

    private DataHandler dataHandler;

    public DHResponse() {
    }

    public DHResponse(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
    }

    @XmlMimeType("application/octet-stream")
    public DataHandler getDataHandler() {
        return dataHandler;
    }

    public void setDataHandler(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
    }
}
