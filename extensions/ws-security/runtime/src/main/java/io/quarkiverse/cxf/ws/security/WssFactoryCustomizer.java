package io.quarkiverse.cxf.ws.security;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.cxf.interceptor.AbstractBasicInterceptorProvider;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.common.ConfigurationConstants;
import org.jboss.logging.Logger;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.CXFRuntimeUtils;
import io.quarkiverse.cxf.CXFServletInfo;
import io.quarkiverse.cxf.CxfClientProducer.ClientFactoryCustomizer;
import io.quarkiverse.cxf.transport.CxfHandler.EndpointFactoryCustomizer;
import io.quarkiverse.cxf.ws.security.CxfWsSecurityConfig.CryptoConfig;
import io.quarkiverse.cxf.ws.security.CxfWsSecurityConfig.MerlinConfig;
import io.quarkiverse.cxf.ws.security.CxfWsSecurityConfig.WsSecurityAction;
import io.quarkiverse.cxf.ws.security.CxfWsSecurityConfig.WsSecurityConfig;

@ApplicationScoped
public class WssFactoryCustomizer implements ClientFactoryCustomizer, EndpointFactoryCustomizer {
    private static final Logger log = Logger.getLogger(WssFactoryCustomizer.class);
    @Inject
    CxfWsSecurityConfig config;

    @Override
    public void customize(CXFClientInfo cxfClientInfo, JaxWsProxyFactoryBean factory) {
        final String key = cxfClientInfo.getConfigKey();
        if (key != null && Optional.ofNullable(config.clients()).map(m -> m.containsKey(key)).orElse(false)) {
            final WsSecurityConfig wssConfig = Optional.ofNullable(config.clients()).map(m1 -> m1.get(key)).orElse(null)
                    .wsSecurity();
            final List<Interceptor<? extends Message>> interceptors = factory.getOutInterceptors();
            customize(Kind.client, key, WSS4JOutInterceptor.class, interceptors, wssConfig, factory)
                    .ifPresent(props -> interceptors.add(new WSS4JOutInterceptor(props)));
        }
    }

    @Override
    public void customize(CXFServletInfo cxfServletInfo, JaxWsServerFactoryBean factory) {
        final String key = cxfServletInfo.getRelativePath();
        if (key != null && Optional.ofNullable(config.endpoints()).map(m -> m.containsKey(key)).orElse(false)) {
            final WsSecurityConfig wssConfig = Optional.ofNullable(config.endpoints()).map(m1 -> m1.get(key)).orElse(null)
                    .wsSecurity();
            final List<Interceptor<? extends Message>> interceptors = factory.getInInterceptors();
            customize(Kind.endpoint, key, WSS4JInInterceptor.class, interceptors, wssConfig, factory)
                    .ifPresent(props -> interceptors.add(new WSS4JInInterceptor(props)));
        }
    }

    private enum Kind {
        client,
        endpoint
    };

    private Optional<Map<String, Object>> customize(
            Kind kind,
            final String key,
            Class<?> toxicInterceptorType,
            List<Interceptor<? extends Message>> interceptors,
            WsSecurityConfig wssConfig,
            AbstractBasicInterceptorProvider factory) {
        final List<WsSecurityAction> actions = wssConfig.actions();
        if (actions.isEmpty()) {
            log.warnf("No actions configured for " + kind + " \"%s\", thus not adding an %s interceptor to it", key,
                    WSS4JOutInterceptor.class.getSimpleName());
            return Optional.empty();
        }
        if (interceptors.stream()
                .anyMatch(i -> toxicInterceptorType.isAssignableFrom(i.getClass()))) {
            throw new IllegalStateException(toxicInterceptorType.getSimpleName() + " already configured for " + kind + " \""
                    + key + "\". Either remove all quarkus.cxf." + kind + ".\"" + key + "\".ws-security.* options or the "
                    + toxicInterceptorType.getSimpleName() + " you added programmatically or via quarkus.cxf." + kind + ".\""
                    + key + "\"." + (kind == Kind.client ? "out" : "in") + "-interceptors");
        }

        final Map<String, Object> props = new LinkedHashMap<>();
        props.put(
                ConfigurationConstants.ACTION,
                actions.stream().map(WsSecurityAction::name).collect(Collectors.joining(" ")));
        consumeAnnotated(WsSecurityConfig.class, wssConfig, props::put);
        return Optional.of(props);

    }

    static void consumeAnnotated(Class<?> cl, Object config, BiConsumer<String, Object> consumer) {
        for (Method method : cl.getDeclaredMethods()) {
            final WssConfigurationConstant wssConfigurationConstant = method.getAnnotation(WssConfigurationConstant.class);
            if (wssConfigurationConstant != null) {
                Object value = null;
                try {
                    value = method.invoke(config);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    throw new RuntimeException("Could not invoke " + cl.getName() + "." + method.getName() + "()", e);
                }
                if (value instanceof Optional) {
                    if (((Optional<?>) value).isPresent()) {
                        value = ((Optional<?>) value).get();
                    } else {
                        continue;
                    }
                }
                if (value == null) {
                    continue;
                }
                final String propKey = wssConfigurationConstant.key() != null && !wssConfigurationConstant.key().isEmpty()
                        ? wssConfigurationConstant.key()
                        : method.getName();
                switch (wssConfigurationConstant.transformer()) {
                    case toString:
                        consumer.accept(propKey, value.toString());
                        break;
                    case beanRef:
                        consumer.accept(propKey, CXFRuntimeUtils.getInstance((String) value, true));
                        break;
                    case crypto:
                        final CryptoConfig cryptoConfig = (CryptoConfig) value;
                        toCrypto(cryptoConfig).ifPresent(crypto -> crypto.entrySet()
                                .forEach(en -> consumer.accept((String) en.getKey(), en.getValue())));
                        break;
                    default:
                        throw new IllegalStateException("Unexpected "
                                + io.quarkiverse.cxf.ws.security.WssConfigurationConstant.Transformer.class.getName() + ": "
                                + wssConfigurationConstant.transformer());
                }
            }
        }
    }

    static Optional<Properties> toCrypto(CryptoConfig cryptoConfig) {
        if (CryptoConfig.MERLIN_PROVIDER.equals(cryptoConfig.provider())) {
            if (isConfigured(cryptoConfig.merlin())) {
                Properties props = new Properties();
                props.put("org.apache.wss4j.crypto.provider", cryptoConfig.provider());

                consumeAnnotated(
                        MerlinConfig.class,
                        cryptoConfig.merlin(),
                        (key, val) -> props.put("org.apache.wss4j.crypto.merlin." + key, val));
                return Optional.of(props);
            } else {
                return Optional.empty();
            }
        } else {
            Properties props = new Properties();
            props.put("org.apache.wss4j.crypto.provider", cryptoConfig.provider());
            cryptoConfig.properties().entrySet().stream().forEach(en -> props.put(en.getKey(), en.getValue()));
            return Optional.of(props);
        }
    }

    static boolean isConfigured(MerlinConfig merlin) {
        return Stream.<Supplier<Optional<String>>> of(
                merlin::x509crlFile,
                merlin::keystoreProvider,
                merlin::certProvider,
                merlin::keystoreFile,
                merlin::keystorePassword,
                merlin::keystoreType,
                merlin::keystoreAlias,
                merlin::keystorePrivatePassword,
                merlin::truststoreFile,
                merlin::truststorePassword,
                merlin::truststoreType,
                merlin::truststoreProvider)
                .map(Supplier::get)
                .anyMatch(Optional::isPresent);
    }

}
