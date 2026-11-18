package io.quarkiverse.cxf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.vertx.core.http.HttpVersion;

class ClientHttpVersionsConfigTest {
    @ConfigMapping(prefix = "client")
    interface ClientConfig extends CxfClientConfig {
    }

    private static ClientConfig config(Map<String, String> properties) {
        return new SmallRyeConfigBuilder().withMapping(ClientConfig.class).withDefaultValues(properties)
                .build().getConfigMapping(ClientConfig.class);
    }

    @Test
    void defaults() {
        ClientConfig config = config(Map.of());
        assertThat(config.version()).isEqualTo("auto");
        assertThat(config.vertx().followAlternativeServices()).isFalse();
        assertThat(CXFClientInfo.parseVertxVersions(config, "test")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "auto", "1.0", "1.1", "2" })
    void existingVersionWithoutList(String version) {
        assertThat(CXFClientInfo.parseVertxVersions(config(Map.of("client.version", version)), "test")).isEmpty();
    }

    @Test
    void preservesOrder() {
        assertThat(CXFClientInfo.parseVertxVersions(config(Map.of("client.vertx.versions", "1.1,2,1.0")), "test"))
                .containsExactly(HttpVersion.HTTP_1_1, HttpVersion.HTTP_2, HttpVersion.HTTP_1_0);
    }

    @ParameterizedTest
    @ValueSource(strings = { "4", "auto", "HTTP_2", "2.0", "invalid", "2,4" })
    void unsupportedVersion(String versions) {
        assertThatThrownBy(() -> CXFClientInfo.parseVertxVersions(
                config(Map.of("client.vertx.versions", versions)), "test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quarkus.cxf.client.test.vertx.versions")
                .hasMessageContaining("supported values are 1.0, 1.1, 2 and 3");
    }

    @Test
    void http3Discovery() {
        ClientConfig config = config(Map.of("client.vertx.versions", "2,1.1,3",
                "client.vertx.follow-alternative-services", "true"));
        assertThat(CXFClientInfo.parseVertxVersions(config, "test"))
                .containsExactly(HttpVersion.HTTP_2, HttpVersion.HTTP_1_1, HttpVersion.HTTP_3);
        assertThat(config.vertx().followAlternativeServices()).isTrue();
    }

    @Test
    void duplicates() {
        assertThatThrownBy(() -> CXFClientInfo.parseVertxVersions(
                config(Map.of("client.vertx.versions", "2,1.1,2")), "test"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Duplicate HTTP version '2'");
    }

    @ParameterizedTest
    @ValueSource(strings = { "1.0", "1.1", "2" })
    void conflictingVersion(String version) {
        assertThatThrownBy(() -> CXFClientInfo.parseVertxVersions(
                config(Map.of("client.vertx.versions", "2,1.1", "client.version", version)), "test"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("leave version at auto");
    }
}
