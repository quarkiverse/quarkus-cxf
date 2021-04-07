package io.quarkiverse.cxf.deployment;

import java.util.Objects;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

public final class CxfSeiBuildItem extends MultiBuildItem {
    public final ClassInfo sei;

    public CxfSeiBuildItem(ClassInfo sei) {
        this.sei = sei;
    }

    public ClassInfo getSei() {
        return sei;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CxfSeiBuildItem that = (CxfSeiBuildItem) o;
        return Objects.equals(sei, that.sei);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sei);
    }

    @Override
    public String toString() {
        return "CxfSeiBuildItem{" +
                "sei=" + sei +
                '}';
    }
}
