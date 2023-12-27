package io.quarkiverse.cxf;

import org.eclipse.microprofile.config.spi.Converter;

public enum EnabledFor {
    clients,
    services,
    both,
    none;

    public boolean enabledForAny() {
        return this != none;
    }

    public boolean enabledForServices() {
        return this == services || this == both;
    }

    public boolean enabledForClients() {
        return this == clients || this == both;
    }

    public static class EnabledForConverter extends AbstractEnumConverter<EnabledFor> implements Converter<EnabledFor> {

        private static final long serialVersionUID = 1L;

        public EnabledForConverter() {
            super(EnabledFor.class);
        }

        @Override
        public EnabledFor convert(String value) {
            if ("clients-and-services".equals(value)) {
                /* We had this value instead of "both" in 2.6.0 */
                return EnabledFor.both;
            }
            return super.convert(value);
        }

    }

}
