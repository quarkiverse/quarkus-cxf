package io.quarkiverse.cxf.logging;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.EnabledFor;
import io.quarkiverse.cxf.PrettyBoolean;
import io.quarkiverse.cxf.logging.LoggingFactoryCustomizer.Kind;

public class LoggingFactoryCustomizerTest {

    @Test
    void isEnabledFor() {
        final Optional<PrettyBoolean> clientEmpty = Optional.empty();

        /* Global none */
        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.none,
                Kind.client,
                clientEmpty))
                .isEqualTo(false);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.none,
                Kind.client,
                Optional.of(PrettyBoolean.TRUE)))
                .isEqualTo(true);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.none,
                Kind.client,
                Optional.of(PrettyBoolean.PRETTY)))
                .isEqualTo(true);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.none,
                Kind.client,
                Optional.of(PrettyBoolean.FALSE)))
                .isEqualTo(false);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.none,
                Kind.endpoint,
                clientEmpty))
                .isEqualTo(false);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.none,
                Kind.endpoint,
                Optional.of(PrettyBoolean.TRUE)))
                .isEqualTo(true);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.none,
                Kind.endpoint,
                Optional.of(PrettyBoolean.PRETTY)))
                .isEqualTo(true);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.none,
                Kind.endpoint,
                Optional.of(PrettyBoolean.FALSE)))
                .isEqualTo(false);

        /* Global clients */
        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.clients,
                Kind.client,
                clientEmpty))
                .isEqualTo(true);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.clients,
                Kind.client,
                Optional.of(PrettyBoolean.TRUE)))
                .isEqualTo(true);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.clients,
                Kind.client,
                Optional.of(PrettyBoolean.PRETTY)))
                .isEqualTo(true);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.clients,
                Kind.client,
                Optional.of(PrettyBoolean.FALSE)))
                .isEqualTo(false);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.clients,
                Kind.endpoint,
                clientEmpty))
                .isEqualTo(false);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.clients,
                Kind.endpoint,
                Optional.of(PrettyBoolean.TRUE)))
                .isEqualTo(true);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.clients,
                Kind.endpoint,
                Optional.of(PrettyBoolean.PRETTY)))
                .isEqualTo(true);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.clients,
                Kind.endpoint,
                Optional.of(PrettyBoolean.FALSE)))
                .isEqualTo(false);

        /* Global services */
        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.services,
                Kind.client,
                clientEmpty))
                .isEqualTo(false);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.services,
                Kind.client,
                Optional.of(PrettyBoolean.TRUE)))
                .isEqualTo(true);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.services,
                Kind.client,
                Optional.of(PrettyBoolean.PRETTY)))
                .isEqualTo(true);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.services,
                Kind.client,
                Optional.of(PrettyBoolean.FALSE)))
                .isEqualTo(false);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.services,
                Kind.endpoint,
                clientEmpty))
                .isEqualTo(true);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.services,
                Kind.endpoint,
                Optional.of(PrettyBoolean.TRUE)))
                .isEqualTo(true);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.services,
                Kind.endpoint,
                Optional.of(PrettyBoolean.PRETTY)))
                .isEqualTo(true);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.services,
                Kind.endpoint,
                Optional.of(PrettyBoolean.FALSE)))
                .isEqualTo(false);

        /* Global both */
        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.both,
                Kind.client,
                clientEmpty))
                .isEqualTo(true);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.both,
                Kind.client,
                Optional.of(PrettyBoolean.TRUE)))
                .isEqualTo(true);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.both,
                Kind.client,
                Optional.of(PrettyBoolean.PRETTY)))
                .isEqualTo(true);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.both,
                Kind.client,
                Optional.of(PrettyBoolean.FALSE)))
                .isEqualTo(false);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.both,
                Kind.endpoint,
                clientEmpty))
                .isEqualTo(true);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.both,
                Kind.endpoint,
                Optional.of(PrettyBoolean.TRUE)))
                .isEqualTo(true);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.both,
                Kind.endpoint,
                Optional.of(PrettyBoolean.PRETTY)))
                .isEqualTo(true);

        Assertions.assertThat(LoggingFactoryCustomizer.isEnabledFor(
                EnabledFor.both,
                Kind.endpoint,
                Optional.of(PrettyBoolean.FALSE)))
                .isEqualTo(false);
    }
}
