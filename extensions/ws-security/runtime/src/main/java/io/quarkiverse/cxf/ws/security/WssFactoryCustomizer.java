package io.quarkiverse.cxf.ws.security;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiConsumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.CXFRuntimeUtils;
import io.quarkiverse.cxf.CXFServletInfo;
import io.quarkiverse.cxf.CxfClientProducer.ClientFactoryCustomizer;
import io.quarkiverse.cxf.transport.CxfHandler.EndpointFactoryCustomizer;
import io.quarkiverse.cxf.ws.security.CxfWsSecurityConfig.SecurityConfig;

@ApplicationScoped
public class WssFactoryCustomizer implements ClientFactoryCustomizer, EndpointFactoryCustomizer {

    @Inject
    CxfWsSecurityConfig config;

    @Override
    public void customize(CXFClientInfo cxfClientInfo, JaxWsProxyFactoryBean factory) {
        final String key = cxfClientInfo.getConfigKey();
        if (key != null && Optional.ofNullable(config.clients()).map(m -> m.containsKey(key)).orElse(false)) {
            final SecurityConfig wssConfig = Optional.ofNullable(config.clients()).map(m1 -> m1.get(key)).orElse(null)
                    .security();
            customize(Kind.client, key, wssConfig, factory.getProperties()::put);
        }
    }

    @Override
    public void customize(CXFServletInfo servletInfo, JaxWsServerFactoryBean factory) {
        final String key = servletInfo.getRelativePath();
        if (key != null && Optional.ofNullable(config.endpoints()).map(m -> m.containsKey(key)).orElse(false)) {
            final SecurityConfig wssConfig = Optional.ofNullable(config.endpoints()).map(m1 -> m1.get(key)).orElse(null)
                    .security();
            customize(Kind.endpoint, key, wssConfig, factory.getProperties()::put);
        }
    }

    private enum Kind {
        client,
        endpoint
    };

    private void customize(
            Kind kind,
            final String key,
            SecurityConfig wssConfig,
            BiConsumer<String, Object> props) {
        consumeAnnotated(SecurityConfig.class, wssConfig, props);
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
                    case properties:
                        final Map<?, ?> map = (Map<?, ?>) value;
                        if (!map.isEmpty()) {
                            final Properties cryptoProps = new Properties();
                            cryptoProps.putAll(map);
                            consumer.accept(propKey, cryptoProps);
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unexpected "
                                + io.quarkiverse.cxf.ws.security.WssConfigurationConstant.Transformer.class.getName() + ": "
                                + wssConfigurationConstant.transformer());
                }
            }
        }
    }

}
