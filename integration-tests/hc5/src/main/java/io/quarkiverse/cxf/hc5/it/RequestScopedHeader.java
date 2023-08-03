package io.quarkiverse.cxf.hc5.it;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class RequestScopedHeader {
    public static final String header = "my-header";

    public String getHeaderValue() {
        return headerValue;
    }

    public void setHeaderValue(String headerValue) {
        this.headerValue = headerValue;
    }

    private String headerValue;
}
