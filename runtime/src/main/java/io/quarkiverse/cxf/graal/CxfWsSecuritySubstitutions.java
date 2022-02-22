package io.quarkiverse.cxf.graal;

import java.util.ServiceLoader;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.ehcache.core.util.ClassLoading", onlyWith = IsWsSecurityPresent.class)
final class Target_org_ehcache_core_util_ClassLoading {

    @Substitute
    public static Iterable servicesOfType(Class serviceType) {
        return ServiceLoader.load(serviceType, Thread.currentThread().getContextClassLoader());
    }

}

@TargetClass(className = "org.jasypt.normalization.Normalizer", onlyWith = IsWsSecurityPresent.class)
final class Target_org_jasypt_normalization_Normalizer {

    @Alias
    static char[] normalizeWithJavaNormalizer(final char[] message) {
        return null;
    }

    @Substitute
    public static char[] normalizeToNfc(final char[] message) {
        return normalizeWithJavaNormalizer(message);
    }

}

public class CxfWsSecuritySubstitutions {

}
