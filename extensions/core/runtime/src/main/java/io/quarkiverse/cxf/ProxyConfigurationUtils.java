package io.quarkiverse.cxf;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import io.quarkus.proxy.ProxyConfiguration;
import io.quarkus.proxy.ProxyConfigurationRegistry;

/**
 * {@link ProxyConfiguration} related utilities.
 *
 * @since 4.0.0
 */
public final class ProxyConfigurationUtils {

    private ProxyConfigurationUtils() {
    }

    /**
     * Decides whether a request to the given {@code host} would be sent through the proxy defined by the given
     * {@link ProxyConfiguration}.
     * <p>
     * This method compiles the patterns in {@link ProxyConfiguration#nonProxyHosts()} on each call.
     * Use {@link #proxiedHosts(ProxyConfiguration)} to decide about more hosts with a single {@link ProxyConfiguration}.
     *
     * @param proxyConfiguration the {@link ProxyConfiguration} to consult or {@code null} if there is none
     * @param host the host name or IP address to decide about
     * @return {@code true} if the given {@code proxyConfiguration} defines a proxy and the given {@code host} is not
     *         matched by any of its {@link ProxyConfiguration#nonProxyHosts()}; {@code false} otherwise
     * @since 4.0.0
     */
    /* TODO: propose moving this method to Quarkus https://github.com/quarkiverse/quarkus-cxf/issues/2444 */
    public static boolean isProxied(ProxyConfiguration proxyConfiguration, String host) {
        return proxiedHosts(proxyConfiguration).test(host);
    }

    /**
     * Creates a predicate deciding whether a request to a host would be sent through the proxy defined by the given
     * {@link ProxyConfiguration}.
     * <p>
     * The entries of {@link ProxyConfiguration#nonProxyHosts()} are interpreted in the same way as Vert.x
     * {@code HttpClient} interprets its {@code nonProxyHosts}: an entry containing {@code *} is a pattern, in which
     * {@code *} stands for any sequence of characters; any other entry has to be equal to the given host.
     * Vert.x treats a dot in a pattern as a wildcard for a single character, whereas the returned predicate takes it
     * literally.
     * Hence, when the returned predicate returns {@code false}, Vert.x {@code HttpClient} will not use the proxy either.
     * <p>
     * {@link ProxyConfiguration#nonProxyHosts()} are read and their patterns are compiled only once, when this method is
     * called.
     * The returned predicate is immutable and it can be used from multiple threads.
     *
     * @param proxyConfiguration the {@link ProxyConfiguration} to consult or {@code null} if there is none
     * @return a predicate taking a host name or IP address and returning {@code true} if the given
     *         {@code proxyConfiguration} defines a proxy and the host is {@code null} or it is not matched by any of
     *         {@link ProxyConfiguration#nonProxyHosts()}; {@code false} otherwise
     * @since 4.0.0
     */
    public static Predicate<String> proxiedHosts(ProxyConfiguration proxyConfiguration) {
        if (!isProxyDefined(proxyConfiguration)) {
            return host -> false;
        }
        final List<Predicate<String>> nonProxyHosts = proxyConfiguration.nonProxyHosts().orElse(List.of()).stream()
                .map(nonProxyHost -> nonProxyHost.indexOf('*') >= 0
                        ? toPattern(nonProxyHost).asMatchPredicate()
                        : (Predicate<String>) nonProxyHost::equals)
                .toList();
        return host -> {
            if (host == null) {
                return true;
            }
            for (Predicate<String> nonProxyHost : nonProxyHosts) {
                if (nonProxyHost.test(host)) {
                    return false;
                }
            }
            return true;
        };
    }

    /**
     * @param proxyConfiguration the {@link ProxyConfiguration} to check or {@code null} if there is none
     * @return {@code true} if the given {@code proxyConfiguration} is not {@code null} and if it is not the special
     *         {@link ProxyConfigurationRegistry#NONE} configuration; {@code false} otherwise
     * @since 4.0.0
     */
    public static boolean isProxyDefined(ProxyConfiguration proxyConfiguration) {
        return proxyConfiguration != null && !ProxyConfigurationRegistry.NONE.equals(proxyConfiguration.host());
    }

    private static Pattern toPattern(String nonProxyHost) {
        final StringBuilder result = new StringBuilder();
        int start = 0;
        int asterisk;
        while ((asterisk = nonProxyHost.indexOf('*', start)) >= 0) {
            if (asterisk > start) {
                result.append(Pattern.quote(nonProxyHost.substring(start, asterisk)));
            }
            result.append(".*");
            start = asterisk + 1;
        }
        if (start < nonProxyHost.length()) {
            result.append(Pattern.quote(nonProxyHost.substring(start)));
        }
        return Pattern.compile(result.toString());
    }
}
