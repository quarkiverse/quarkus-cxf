package io.quarkiverse.cxf.hc5.it;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;

@Singleton
public class MeterFilterProducer {

    @Inject
    RequestScopedHeader requestScopedHeader;

    @Produces
    @Singleton
    public MeterFilter addTags() {
        return new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                if (id.getName().startsWith("http.client") || id.getName().startsWith("cxf.client")) {
                    return id.withTag(Tag.of("my-header", requestScopedHeader.getHeaderValue()));
                } else {
                    return id;
                }
            }
        };
    }
}
