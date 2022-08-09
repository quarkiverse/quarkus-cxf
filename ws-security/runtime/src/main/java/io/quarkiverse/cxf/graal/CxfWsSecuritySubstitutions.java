package io.quarkiverse.cxf.graal;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.ServiceLoader;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.stream.XMLOutputFactory;

import org.apache.wss4j.stax.setup.WSSec;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.utils.Constants;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
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

@TargetClass(className = "org.apache.xml.security.stax.ext.XMLSecurityConstants")
final class Target_org_apache_xml_security_stax_ext_XMLSecurityConstants {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    public static DatatypeFactory datatypeFactory;

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    public static XMLOutputFactory xmlOutputFactory;

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    public static XMLOutputFactory xmlOutputFactoryNonRepairingNs;

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static SecureRandom SECURE_RANDOM;

    static {
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }

        xmlOutputFactory = XMLOutputFactory.newInstance();
        xmlOutputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);

        xmlOutputFactoryNonRepairingNs = XMLOutputFactory.newInstance();
        xmlOutputFactoryNonRepairingNs.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, false);
    }

    @Substitute
    public Target_org_apache_xml_security_stax_ext_XMLSecurityConstants() {
    }

    @Substitute
    public static byte[] generateBytes(int length) throws XMLSecurityException {
        if (SECURE_RANDOM == null) {
            try {
                SECURE_RANDOM = SecureRandom.getInstance("SHA1PRNG");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            byte[] temp = new byte[length];
            SECURE_RANDOM.nextBytes(temp);
            return temp;
        } catch (Exception ex) {
            throw new XMLSecurityException(ex);
        }
    }
}

@TargetClass(className = "org.apache.wss4j.common.crypto.WSS4JResourceBundle")
final class Target_org_apache_wss4j_common_crypto_WSS4JResourceBundle {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(Target_org_apache_wss4j_common_crypto_WSS4JResourceBundle.class);

    @Alias
    private ResourceBundle wss4jSecResourceBundle;

    @Alias
    private ResourceBundle xmlSecResourceBundle;

    @Substitute
    public Target_org_apache_wss4j_common_crypto_WSS4JResourceBundle() {
        wss4jSecResourceBundle = ResourceBundle.getBundle("messages.wss4j_errors", Locale.getDefault(),
                Thread.currentThread().getContextClassLoader());

        ResourceBundle tmpResourceBundle;
        try {
            tmpResourceBundle = ResourceBundle.getBundle(Constants.exceptionMessagesResourceBundleBase,
                    Locale.getDefault(),
                    Thread.currentThread().getContextClassLoader());
        } catch (MissingResourceException ex) {
            // Using a Locale of which there is no properties file.
            LOG.debug(ex.getMessage());
            // Default to en/US
            tmpResourceBundle = ResourceBundle.getBundle(Constants.exceptionMessagesResourceBundleBase,
                    new Locale("en", "US"), Thread.currentThread().getContextClassLoader());
        }
        xmlSecResourceBundle = tmpResourceBundle;
    }

}

public class CxfWsSecuritySubstitutions {

}
