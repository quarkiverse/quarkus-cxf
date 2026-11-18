package io.quarkiverse.cxf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.smallrye.config.SmallRyeConfigBuilder;
import io.vertx.core.http.HttpVersion;

class HttpVersionsConfigTest {
    private static final String CLIENT = "ordered.client";
    private static final String PREFIX = "quarkus.cxf.client.\"" + CLIENT + "\"";
    private static final String PROPERTY = PREFIX + ".vertx.http-versions";

    static Stream<List<String>> versions() {
        return Stream.of(List.of("1.0"), List.of("1.1"), List.of("2"),
                List.of("2", "1.1"), List.of("1.1", "2"), List.of("2", "1.0"),
                List.of("2", "1.1", "1.0"), List.of("2", "1.0", "1.1"),
                List.of("1.0", "2", "1.1"), List.of("1.0", "1.1", "2"),
                List.of("1.1", "1.0", "2"), List.of("1.1", "2", "1.0"));
    }

    @ParameterizedTest
    @MethodSource("versions")
    void orderedImmutableVersions(List<String> values) {
        final List<HttpVersion> actual = parse(Map.of(PROPERTY, String.join(",", values)));
        assertThat(actual).containsExactlyElementsOf(values.stream().map(value -> switch (value) {
            case "1.0" -> HttpVersion.HTTP_1_0;
            case "1.1" -> HttpVersion.HTTP_1_1;
            case "2" -> HttpVersion.HTTP_2;
            default -> throw new IllegalArgumentException(value);
        }).toList());
        assertThatThrownBy(() -> actual.add(HttpVersion.HTTP_1_1)).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "3", "auto", "HTTP_2", "2.0", "bogus", "2,3", "2,2", "1.0,1.1,1.0", "1.1,1.1" })
    void invalidValuesIdentifyClientAndCanonicalProperty(String value) {
        assertThatThrownBy(() -> parse(Map.of(PROPERTY, value)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(PROPERTY)
                .hasMessageContaining(value.substring(value.lastIndexOf(',') + 1));
    }

    @ParameterizedTest
    @ValueSource(strings = { "1.0", "1.1", "2" })
    void legacyConflict(String legacy) {
        assertThatThrownBy(() -> parse(Map.of(PROPERTY, "2,1.1", PREFIX + ".version", legacy)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(PROPERTY).hasMessageContaining(PREFIX + ".version=" + legacy);
    }

    @Test
    void absentAndEmptyBindingRetainLegacyMode() {
        assertThat(parse(Map.of())).isEmpty();
        assertThat(parse(Map.of(PROPERTY, ""))).isEmpty();
        for (String legacy : List.of("auto", "1.0", "1.1", "2")) {
            assertThat(parse(Map.of(PREFIX + ".version", legacy))).isEmpty();
        }
        assertThat(parse(Map.of(PROPERTY, "2", PREFIX + ".version", "auto")))
                .containsExactly(HttpVersion.HTTP_2);
    }

    @Test
    void presentEmptyListIsRejected() {
        final CxfClientConfig config = new SmallRyeConfigBuilder().withMapping(EmptyVersionsConfig.class, "empty")
                .build().getConfigMapping(EmptyVersionsConfig.class, "empty");
        assertThat(config.vertx().httpVersions()).contains(List.of());
        assertThatThrownBy(() -> CXFClientInfo.parseHttpVersions(config, CLIENT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(PROPERTY)
                .hasMessageContaining("at least one");
    }

    public interface EmptyVersionsConfig extends CxfClientConfig {
        @Override
        EmptyVertxConfig vertx();
    }

    public interface EmptyVertxConfig extends CxfClientConfig.VertxConfig {
        @Override
        default Optional<List<String>> httpVersions() {
            return Optional.of(List.of());
        }
    }

    private static List<HttpVersion> parse(Map<String, String> properties) {
        final CxfConfig config = new SmallRyeConfigBuilder().withMapping(CxfConfig.class)
                .withDefaultValue(PREFIX + ".client-endpoint-url", "http://localhost/unused")
                .withDefaultValues(properties).build().getConfigMapping(CxfConfig.class);
        return CXFClientInfo.parseHttpVersions(config.clients().get(CLIENT), CLIENT);
    }
}
