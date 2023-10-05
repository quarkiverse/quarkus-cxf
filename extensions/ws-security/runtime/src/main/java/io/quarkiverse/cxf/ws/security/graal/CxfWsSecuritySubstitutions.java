package io.quarkiverse.cxf.ws.security.graal;

import java.text.Normalizer;
import java.util.ServiceLoader;

import org.jasypt.exceptions.EncryptionInitializationException;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.ehcache.core.util.ClassLoading")
final class Target_org_ehcache_core_util_ClassLoading {

    @Substitute
    public static <T> Iterable<T> servicesOfType(Class<T> serviceType) {
        return ServiceLoader.load(serviceType, Thread.currentThread().getContextClassLoader());
    }

}

@TargetClass(className = "org.jasypt.normalization.Normalizer")
final class Target_org_jasypt_normalization_Normalizer {

    @Substitute
    public static String normalizeToNfc(final String message) {
        /*
         * Because we are on Java 1.4+ we can afford to skip the reflection based detection
         * done in org.jasypt.normalization.Normalizer.normalizeToNfc(char[])
         * whether icu4j is in class path and whether the current Java is 1.4+
         * and go straight to calling the JDK's Normalizer.normalize()
         */
        try {
            return Normalizer.normalize(message, java.text.Normalizer.Form.NFC);
        } catch (final Exception e) {
            throw new EncryptionInitializationException(
                    "Could not perform a valid UNICODE normalization", e);
        }
    }

    @Substitute
    public static char[] normalizeToNfc(final char[] message) {
        final String result = normalizeToNfc(new String(message));
        return result.toCharArray();
    }

}

public class CxfWsSecuritySubstitutions {

}
