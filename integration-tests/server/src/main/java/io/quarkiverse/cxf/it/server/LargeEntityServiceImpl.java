package io.quarkiverse.cxf.it.server;

import jakarta.jws.WebService;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * The simplest Hello service implementation.
 */
@WebService(serviceName = "LargeEntityService", name = "LargeEntityService")
public class LargeEntityServiceImpl implements LargeEntityService {

    @ConfigProperty(name = "quarkus.cxf.output-buffer-size")
    int outputBufferSize;

    @Override
    public int outputBufferSize() {
        return outputBufferSize;
    }

    @Override
    public String[] items(int count, int itemLength) {
        final String[] items = new String[count];
        final StringBuilder sb = new StringBuilder(itemLength);
        for (int i = 0; i < itemLength; i++) {
            char ch = Character.forDigit(i % 10, 10);
            sb.append(ch);
        }
        final String str = sb.toString();
        for (int i = 0; i < count; i++) {
            items[i] = str;
        }
        return items;
    }

}
