package io.quarkiverse.cxf.graal;

import java.util.ServiceLoader;

import org.apache.wss4j.stax.setup.WSSec;

import com.oracle.svm.core.annotate.Alias;
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

    @Alias
    static char[] normalizeWithJavaNormalizer(final char[] message) {
        // TODO: Returning null here does not feel right https://github.com/quarkiverse/quarkus-cxf/issues/519
        return null;
    }

    @Substitute
    public static char[] normalizeToNfc(final char[] message) {
        return normalizeWithJavaNormalizer(message);
    }

}

@TargetClass(className = "org.apache.xml.security.stax.ext.XMLSec")
final class Target_org_apache_xml_security_stax_ext_XMLSec {

    static {
        WSSec.init();
    }

}

public class CxfWsSecuritySubstitutions {

}
