package io.quarkiverse.cxf;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.cxf.HttpVersionsConfigTest.TestProxyConfiguration;
import io.quarkus.proxy.ProxyConfiguration;
import io.quarkus.proxy.ProxyType;

class ProxyConfigurationUtilsTest {

    @Test
    void noProxy() {
        assertThat(ProxyConfigurationUtils.isProxyDefined(null)).isFalse();
        assertThat(ProxyConfigurationUtils.isProxied(null, "example.org")).isFalse();

        /* The special configuration returned by ProxyConfigurationRegistry for the name "none" */
        final ProxyConfiguration none = new TestProxyConfiguration("none", 0, null);
        assertThat(ProxyConfigurationUtils.isProxyDefined(none)).isFalse();
        assertThat(ProxyConfigurationUtils.isProxied(none, "example.org")).isFalse();
    }

    @Test
    void noNonProxyHosts() {
        final ProxyConfiguration proxy = new TestProxyConfiguration("proxy.example.org", 3128, null);
        assertThat(ProxyConfigurationUtils.isProxyDefined(proxy)).isTrue();
        assertThat(ProxyConfigurationUtils.isProxied(proxy, "example.org")).isTrue();
        assertThat(ProxyConfigurationUtils.isProxied(proxy, "localhost")).isTrue();
        assertThat(ProxyConfigurationUtils.isProxied(proxy, null)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "localhost,false",
            "localhost.example.org,true",
            "127.0.0.1,false",
            "127.0.0.2,true",
            "service.internal.example.org,false",
            "a.b.internal.example.org,false",
            /* The asterisk stands also for an empty sequence of characters */
            ".internal.example.org,false",
            "internal.example.org,true",
            /* The dot in a pattern is not a wildcard */
            "serviceXinternal.example.org,true",
            "service.internal.example.org.evil.com,true",
            "10.1.2.3,false",
            "10.11.2.3,true",
            "prefix-a-suffix,false",
            "prefix--suffix,false",
            "prefix-a-suffix-b,true",
            "example.org,true"
    })
    void nonProxyHosts(String host, boolean expectedProxied) {
        final ProxyConfiguration proxy = new TestProxyConfiguration("proxy.example.org", 3128,
                List.of("localhost", "127.0.0.1", "*.internal.example.org", "10.1.*", "prefix-*-suffix"));
        assertThat(ProxyConfigurationUtils.isProxied(proxy, host)).isEqualTo(expectedProxied);
        assertThat(ProxyConfigurationUtils.proxiedHosts(proxy).test(host)).isEqualTo(expectedProxied);
    }

    @Test
    void proxiedHostsWithoutProxy() {
        assertThat(ProxyConfigurationUtils.proxiedHosts(null).test("example.org")).isFalse();
        assertThat(ProxyConfigurationUtils.proxiedHosts(null).test(null)).isFalse();
        final ProxyConfiguration none = new TestProxyConfiguration("none", 0, null);
        assertThat(ProxyConfigurationUtils.proxiedHosts(none).test("example.org")).isFalse();
    }

    /** The non proxy hosts are read and compiled when the predicate is created, not when it is used */
    @Test
    void proxiedHostsReadsNonProxyHostsOnce() {
        final AtomicInteger nonProxyHostsReads = new AtomicInteger();
        final ProxyConfiguration proxy = new CountingProxyConfiguration(
                new TestProxyConfiguration("proxy.example.org", 3128, List.of("localhost", "*.internal.example.org")),
                nonProxyHostsReads);
        final Predicate<String> proxiedHosts = ProxyConfigurationUtils.proxiedHosts(proxy);
        assertThat(nonProxyHostsReads).hasValue(1);

        for (int i = 0; i < 3; i++) {
            assertThat(proxiedHosts.test("example.org")).isTrue();
            assertThat(proxiedHosts.test("localhost")).isFalse();
            assertThat(proxiedHosts.test("service.internal.example.org")).isFalse();
            assertThat(proxiedHosts.test(null)).isTrue();
        }
        assertThat(nonProxyHostsReads).hasValue(1);
    }

    @Test
    void asteriskOnly() {
        final ProxyConfiguration proxy = new TestProxyConfiguration("proxy.example.org", 3128, List.of("*"));
        assertThat(ProxyConfigurationUtils.isProxied(proxy, "example.org")).isFalse();
    }

    /** A {@link ProxyConfiguration} counting how many times its {@link #nonProxyHosts()} were read */
    record CountingProxyConfiguration(ProxyConfiguration delegate, AtomicInteger nonProxyHostsReads)
            implements
                ProxyConfiguration {

        @Override
        public String host() {
            return delegate.host();
        }

        @Override
        public int port() {
            return delegate.port();
        }

        @Override
        public Optional<String> username() {
            return delegate.username();
        }

        @Override
        public Optional<String> password() {
            return delegate.password();
        }

        @Override
        public Optional<List<String>> nonProxyHosts() {
            nonProxyHostsReads.incrementAndGet();
            return delegate.nonProxyHosts();
        }

        @Override
        public Optional<Duration> proxyConnectTimeout() {
            return delegate.proxyConnectTimeout();
        }

        @Override
        public ProxyType type() {
            return delegate.type();
        }
    }
}
