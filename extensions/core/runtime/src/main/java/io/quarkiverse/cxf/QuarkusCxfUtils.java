package io.quarkiverse.cxf;

import io.smallrye.config.NameIterator;

public class QuarkusCxfUtils {
    private QuarkusCxfUtils() {
    }

    /**
     * Keys occurring in free form configuration maps may need quoting with double quotes, if they contain periods.
     *
     * @param key a key occurring in a free form configuration map, such as {@code "client-name"} in
     *        {@code quarkus.cxf.client."client-name".logging.enabled}.
     * @return a possibly quoted key
     */
    public static String quoteCongurationKeyIfNeeded(String key) {
        final NameIterator keyIterator = new NameIterator(key);
        keyIterator.next();
        return keyIterator.hasNext() ? "\"" + key + "\"" : key;
    }
}
