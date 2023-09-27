package io.quarkiverse.cxf;

import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.eclipse.microprofile.config.spi.Converter;

public class ConnectionTypeConverter extends AbstractEnumConverter<ConnectionType> implements Converter<ConnectionType> {

    private static final long serialVersionUID = 1L;

    public ConnectionTypeConverter() {
        super(ConnectionType.class, ConnectionType::value);
    }

    @Override
    public ConnectionType convert(String value) {
        return super.convert(value);
    }

}
