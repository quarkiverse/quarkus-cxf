package io.quarkiverse.cxf.graal;

import java.util.function.BooleanSupplier;

public class IsWsSecurityPresent implements BooleanSupplier {

    static final String CXF_WS_SECURITY_CLASS = "org.apache.cxf.ws.security.SecurityConstants";

    @Override
    public boolean getAsBoolean() {
        try {
            Class.forName(CXF_WS_SECURITY_CLASS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
