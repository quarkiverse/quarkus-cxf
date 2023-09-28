package io.quarkiverse.cxf.ws.security;

import org.eclipse.microprofile.config.spi.Converter;

import io.quarkiverse.cxf.AbstractEnumConverter;
import io.quarkiverse.cxf.ws.security.CxfWsSecurityConfig.WsSecurityAction;

public class WsSecurityActionConverter extends AbstractEnumConverter<WsSecurityAction> implements Converter<WsSecurityAction> {

    private static final long serialVersionUID = 1L;

    public WsSecurityActionConverter() {
        super(WsSecurityAction.class);
    }

    @Override
    public WsSecurityAction convert(String value) {
        return super.convert(value);
    }

}
