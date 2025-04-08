package io.quarkiverse.cxf.metrics.it.async;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.metrics.micrometer.provider.TagsCustomizer;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.quarkiverse.cxf.metrics.it.async.HeaderToMetricsTagRequestFilter.RequestScopedHeader;

@Singleton
@Named("headerToMetricsTagsCustomizer")
public class HeaderToMetricsTagsCustomizer implements TagsCustomizer {

    @Inject
    RequestScopedHeader requestScopedHeader;

    @Override
    public Iterable<Tag> getAdditionalTags(Exchange ex, boolean client) {
        final String val = requestScopedHeader.getHeaderValue();
        if (val != null) {
            return Tags.of(Tag.of("my-header", val));
        }
        return Tags.empty();
    }
}
