package io.quarkiverse.cxf.deployment;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Holds a map from SEI class names to lists of their respective wrapper class names.
 */
public final class CxfWrapperClassNamesBuildItem extends SimpleBuildItem {
    private final Map<String, Set<String>> wrapperClassNames;

    private CxfWrapperClassNamesBuildItem(Map<String, Set<String>> wrapperClassNames) {
        super();
        this.wrapperClassNames = wrapperClassNames;
    }

    public Map<String, Set<String>> getWrapperClassNames() {
        return wrapperClassNames;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<String, Set<String>> wrapperClassNames = new TreeMap<>();

        public Builder put(String sei, Collection<String> wrapperClassNames) {
            this.wrapperClassNames.put(sei, Collections.unmodifiableSet(new LinkedHashSet<>(wrapperClassNames)));
            return this;
        }

        public CxfWrapperClassNamesBuildItem build() {
            Map<String, Set<String>> map = wrapperClassNames;
            wrapperClassNames = null; // do not allow leaking the collection
            return new CxfWrapperClassNamesBuildItem(map);
        }
    }

}
