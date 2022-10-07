package io.quarkiverse.cxf.it.ws.mtom.server;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "dataRequest", namespace = "http://org.jboss.ws/xop/doclit")
public class DHRequest {
    private DataHandler dataHandler;

    public DHRequest() {
    }

    public DHRequest(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
    }

    @XmlMimeType("text/plain")
    public DataHandler getDataHandler() {
        return dataHandler;
    }

    public void setDataHandler(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
    }
}
