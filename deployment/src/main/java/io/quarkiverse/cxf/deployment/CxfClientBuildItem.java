package io.quarkiverse.cxf.deployment;

import static io.quarkus.arc.processor.DotNames.NAMED;

import java.util.Objects;
import java.util.Optional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Type;

import io.quarkus.builder.item.MultiBuildItem;

public final class CxfClientBuildItem extends MultiBuildItem {
    Type sei;
    String named;

    public CxfClientBuildItem(Type sei, String named) {
        this.sei = sei;
        this.named = named;
    }

    public Type getSei() {
        return sei;
    }

    public String getNamed() {
        return named;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CxfClientBuildItem that = (CxfClientBuildItem) o;
        return Objects.equals(sei, that.sei) && Objects.equals(named, that.named);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sei, named);
    }

    @Override
    public String toString() {
        return "CxfClientBuildItem{" +
                "sei='" + sei + '\'' +
                ", named='" + named + '\'' +
                '}';
    }

    public static CxfClientBuildItem valueOf(FieldInfo field) {
        Type type = field.type();
        AnnotationInstance annotation = field.annotation(NAMED);
        AnnotationValue av = Optional.ofNullable(annotation).map(x -> x.value()).orElse(null);
        String named = Optional.ofNullable(av).map(y -> y.asString()).orElse(null);
        return new CxfClientBuildItem(type, named);
    }
}
