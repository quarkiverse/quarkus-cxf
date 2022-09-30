package io.quarkiverse.cxf.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Holds a map from SEI class names to lists of their respective wrapper class names.
 */
public final class CxfWrapperClassNamesBuildItem extends SimpleBuildItem {
    private final Map<String, List<String>> wrapperClassNames;

    private CxfWrapperClassNamesBuildItem(Map<String, List<String>> wrapperClassNames) {
        super();
        this.wrapperClassNames = wrapperClassNames;
    }

    public Map<String, List<String>> getWrapperClassNames() {
        return wrapperClassNames;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<String, List<String>> wrapperClassNames = new TreeMap<>();

        public Builder put(String sei, List<String> wrapperClassNames) {
            this.wrapperClassNames.put(sei, Collections.unmodifiableList(new ArrayList<>(wrapperClassNames)));
            return this;
        }

        public CxfWrapperClassNamesBuildItem build() {
            Map<String, List<String>> map = wrapperClassNames;
            wrapperClassNames = null; // do not allow leaking the collection
            return new CxfWrapperClassNamesBuildItem(map);
        }
    }

}
