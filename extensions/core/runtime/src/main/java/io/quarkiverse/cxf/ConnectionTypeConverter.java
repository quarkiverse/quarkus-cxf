package io.quarkiverse.cxf;

import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.eclipse.microprofile.config.spi.Converter;

public class ConnectionTypeConverter implements Converter<ConnectionType> {

    private static final long serialVersionUID = 1L;

    public ConnectionTypeConverter() {
    }

    @Override
    public ConnectionType convert(String value) {
        return ConnectionType.fromValue(value);
    }

}
