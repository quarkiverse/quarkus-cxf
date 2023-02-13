package io.quarkiverse.cxf.saaj.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "jakarta.xml.soap.FactoryFinder")
final class Traget_javax_xml_soap_FactoryFinder {

    /**
     * The target method wants to read a properties file under {@code java.home} which does not work on GraalVM (there
     * is no
     * JRE distro at native runtime).
     *
     * @param factoryId
     * @param deprecatedFactoryId
     * @return
     */
    @Substitute
    private static String fromJDKProperties(String factoryId, String deprecatedFactoryId) {
        return null;
    }
}

public class SoapApiSubstitutions {
}
