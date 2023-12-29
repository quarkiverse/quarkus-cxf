package io.quarkiverse.cxf;

import org.eclipse.microprofile.config.spi.Converter;

public enum PrettyBoolean {
    TRUE,
    FALSE,
    PRETTY;

    public boolean enabled() {
        return this != FALSE;
    }

    public boolean pretty() {
        return this == PRETTY;
    }

    public static class PrettyBooleanConverter implements Converter<PrettyBoolean> {

        private static final long serialVersionUID = 1L;

        @Override
        public PrettyBoolean convert(String value) {
            if ("pretty".equals(value)) {
                return PRETTY;
            } else if ("true".equals(value)) {
                return TRUE;
            } else if ("false".equals(value)) {
                return FALSE;
            }
            throw new IllegalArgumentException(
                    "Cannot map '" + value + "' to any " + PrettyBoolean.class.getName()
                            + " value. Expected: true, false or pretty");
        }

    }

}
