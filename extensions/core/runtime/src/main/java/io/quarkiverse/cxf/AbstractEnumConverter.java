package io.quarkiverse.cxf;

import java.io.Serializable;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.spi.Converter;

public abstract class AbstractEnumConverter<E extends Enum<E>> implements Converter<E>, Serializable {

    private static final long serialVersionUID = 1L;
    private final Class<E> enumType;
    private final Function<E, String> enumValueMapper;

    public AbstractEnumConverter(Class<E> enumType) {
        super();
        this.enumType = enumType;
        this.enumValueMapper = Enum::name;
    }

    public AbstractEnumConverter(Class<E> enumType, Function<E, String> enumValueMapper) {
        super();
        this.enumType = enumType;
        this.enumValueMapper = enumValueMapper;
    }

    @Override
    public E convert(String value) {
        for (E c : this.enumType.getEnumConstants()) {
            if (enumValueMapper.apply(c).equalsIgnoreCase(value)) {
                return c;
            }
        }
        throw new IllegalArgumentException(
                "Cannot map '" + value + "' to any " + enumType.getName() + " value. Expected: "
                        + Stream.of(enumType.getEnumConstants()).map(enumValueMapper).collect(Collectors.joining(", ")));
    }

}
