package io.quarkiverse.cxf.deployment.test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.annotation.CXFClient;

@ApplicationScoped
public class CxfClientConstructorInjectionBean {

    private final CXFClientInfo clientInfo;
    private final FruitWebService clientProxy;

    @Inject
    public CxfClientConstructorInjectionBean(
            // @Named is omitted here because not required
            CXFClientInfo clientInfo,
            @CXFClient FruitWebService clientProxy) {
        this.clientInfo = clientInfo;
        this.clientProxy = clientProxy;
    }

    public CXFClientInfo getClientInfo() {
        return clientInfo;
    }

    public FruitWebService getClientProxy() {
        return clientProxy;
    }
}
