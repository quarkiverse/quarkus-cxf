package io.quarkiverse.cxf.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.cxf.metrics.MetricsFeature;
import org.apache.cxf.metrics.micrometer.MicrometerMetricsProperties;
import org.apache.cxf.metrics.micrometer.MicrometerMetricsProvider;
import org.apache.cxf.metrics.micrometer.provider.DefaultExceptionClassProvider;
import org.apache.cxf.metrics.micrometer.provider.DefaultTimedAnnotationProvider;
import org.apache.cxf.metrics.micrometer.provider.StandardTags;
import org.apache.cxf.metrics.micrometer.provider.StandardTagsProvider;
import org.apache.cxf.metrics.micrometer.provider.TagsCustomizer;
import org.apache.cxf.metrics.micrometer.provider.TagsProvider;
import org.apache.cxf.metrics.micrometer.provider.TimedAnnotationProvider;
import org.apache.cxf.metrics.micrometer.provider.jaxws.JaxwsFaultCodeProvider;
import org.apache.cxf.metrics.micrometer.provider.jaxws.JaxwsFaultCodeTagsCustomizer;
import org.apache.cxf.metrics.micrometer.provider.jaxws.JaxwsOperationTagsCustomizer;
import org.apache.cxf.metrics.micrometer.provider.jaxws.JaxwsTags;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.quarkiverse.cxf.CXFRuntimeUtils;

public class QuarkusCxfMetricsFeature extends MetricsFeature {

    private static final MeterRegistry meterRegistry = Metrics.globalRegistry;
    private static final JaxwsTags jaxwsTags = new JaxwsTags();
    private static final TagsCustomizer operationsCustomizer = new JaxwsOperationTagsCustomizer(jaxwsTags);
    private static final TagsCustomizer faultsCustomizer = new JaxwsFaultCodeTagsCustomizer(jaxwsTags,
            new JaxwsFaultCodeProvider());
    private static final TimedAnnotationProvider timedAnnotationProvider = new DefaultTimedAnnotationProvider();
    private static final TagsProvider tagsProvider = new StandardTagsProvider(new DefaultExceptionClassProvider(),
            new StandardTags());
    private static final MicrometerMetricsProperties micrometerMetricsProperties = new MicrometerMetricsProperties();

    public QuarkusCxfMetricsFeature(Optional<List<String>> tagsCustomizersRefs) {
        super(new MicrometerMetricsProvider(meterRegistry, tagsProvider,
                joinTagsCustomizers(tagsCustomizersRefs, operationsCustomizer, faultsCustomizer), timedAnnotationProvider,
                micrometerMetricsProperties));
    }

    private static List<TagsCustomizer> joinTagsCustomizers(Optional<List<String>> tagsCustomizersRefsOptional,
            TagsCustomizer operationsCustomizer,
            TagsCustomizer faultsCustomizer) {
        List<String> tagsCustomizersRefs = null;
        if (tagsCustomizersRefsOptional.isPresent() && (tagsCustomizersRefs = tagsCustomizersRefsOptional.get()) != null
                && !tagsCustomizersRefs.isEmpty()) {
            final List<TagsCustomizer> result = new ArrayList<>(tagsCustomizersRefs.size() + 2);
            result.add(operationsCustomizer);
            result.add(faultsCustomizer);
            for (String ref : tagsCustomizersRefs) {
                result.add(CXFRuntimeUtils.getInstance(ref, true));
            }
            return result;
        } else {
            return List.of(operationsCustomizer, faultsCustomizer);
        }
    }

}
