package io.quarkiverse.cxf.it.server;

import jakarta.xml.ws.WebFault;

@WebFault(name = "GreetingFault")
public class GreetingException extends Exception {

    private String faultInfo;

    public GreetingException(String message) {
        super(message);
    }

    public GreetingException(String message, String faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    public String getFaultInfo() {
        return this.faultInfo;
    }

}