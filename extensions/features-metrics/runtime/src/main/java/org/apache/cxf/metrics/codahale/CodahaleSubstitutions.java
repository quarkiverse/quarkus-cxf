package org.apache.cxf.metrics.codahale;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * We do not support Dropwizard Metrics.
 */
public class CodahaleSubstitutions {

    @TargetClass(ConfigProviderResolver.class)
    @Delete
    static final class ConfigProviderResolver {
    }

    @TargetClass(CodahaleMetricsContext.class)
    @Delete
    static final class CodahaleMetricsContext {
    }
}
