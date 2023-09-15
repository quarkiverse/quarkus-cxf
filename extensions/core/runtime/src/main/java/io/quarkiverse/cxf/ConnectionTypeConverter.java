package io.quarkiverse.cxf;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.eclipse.microprofile.config.spi.Converter;

public class ConnectionTypeConverter implements Converter<ConnectionType> {

    private static final long serialVersionUID = 1L;

    public ConnectionTypeConverter() {
    }

    @Override
    public ConnectionType convert(String value) {
        for (ConnectionType c : ConnectionType.values()) {
            if (c.value().equalsIgnoreCase(value)) {
                return c;
            }
        }
        throw new IllegalArgumentException(
                "Cannot map '" + value + "' to any " + ConnectionType.class.getName() + " value. Expected: "
                        + Stream.of(ConnectionType.values()).map(ConnectionType::value).collect(Collectors.joining(", ")));
    }

}
