package io.quarkiverse.cxf.deployment;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import javax.jws.WebService;
import javax.xml.ws.WebServiceProvider;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Carries the collection of all classes annotated with either {@link WebService} or {@link WebServiceProvider} that are
 * available in Jandex.
 */
public final class WebServiceClassesBuildItem extends SimpleBuildItem {
    private final Map<String, ClassInfo> webServiceClasses;

    public Builder builder() {
        return new Builder();
    }

    /**
     * Use {@link #builder()}
     *
     * @param webServiceClasses
     */
    private WebServiceClassesBuildItem(Map<String, ClassInfo> webServiceClasses) {
        super();
        this.webServiceClasses = webServiceClasses;
    }

    public Map<String, ClassInfo> getWebServiceClasses() {
        return webServiceClasses;
    }

    public static class Builder {
        private Map<String, ClassInfo> webServiceClasses = new TreeMap<>();

        public Builder add(ClassInfo cl) {
            webServiceClasses.put(cl.name().toString(), cl);
            return this;
        }

        public WebServiceClassesBuildItem build() {
            Map<String, ClassInfo> cls = webServiceClasses;
            webServiceClasses = null; // do not allow the collection to leak;
            return new WebServiceClassesBuildItem(Collections.unmodifiableMap(cls));
        }
    }
}
