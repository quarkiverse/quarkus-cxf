package io.quarkiverse.cxf;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.feature.Features;
import org.apache.cxf.interceptor.AnnotationInterceptors;
import org.apache.cxf.interceptor.InFaultInterceptors;
import org.apache.cxf.interceptor.InInterceptors;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.OutFaultInterceptors;
import org.apache.cxf.interceptor.OutInterceptors;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.message.Message;

/**
 * A JaxWsServerFactoryBean allowing to look up <code>@InInterceptors</code> in the CDI container.
 */
public class QuarkusJaxWsServerFactoryBean extends JaxWsServerFactoryBean {

    private final String endpointString;

    public QuarkusJaxWsServerFactoryBean(JaxWsServiceFactoryBean serviceFactory, String endpointString) {
        super(serviceFactory);
        this.endpointString = endpointString;
    }

    @Override
    protected void initializeAnnotationInterceptors(Endpoint ep, Class<?>... cls) {
        final Class<?> seiClass = ((JaxWsServiceFactoryBean) getServiceFactory())
                .getJaxWsImplementorInfo().getSEIClass();
        if (seiClass != null) {
            boolean found = false;
            for (Class<?> c : cls) {
                if (c.equals(seiClass)) {
                    found = true;
                }
            }
            if (!found) {
                Class<?>[] cls2 = new Class<?>[cls.length + 1];
                System.arraycopy(cls, 0, cls2, 0, cls.length);
                cls2[cls.length] = seiClass;
                cls = cls2;
            }
        }
        final AnnotationInterceptors provider = new QuarkusAnnotationInterceptors(
                ((JaxWsServiceFactoryBean) getServiceFactory()).getJaxWsImplementorInfo().getImplementorClass().getName(),
                endpointString,
                cls);
        initializeAnnotationInterceptors(provider, ep);
    }

    @Override
    protected boolean initializeAnnotationInterceptors(AnnotationInterceptors provider, Endpoint ep) {
        boolean hasAnnotation = false;
        final List<Interceptor<? extends Message>> inFaultInterceptors = provider.getInFaultInterceptors();
        if (inFaultInterceptors != null) {
            ep.getInFaultInterceptors().addAll(inFaultInterceptors);
            hasAnnotation = true;
        }
        final List<Interceptor<? extends Message>> inInterceptors = provider.getInInterceptors();
        if (inInterceptors != null) {
            ep.getInInterceptors().addAll(inInterceptors);
            hasAnnotation = true;
        }
        final List<Interceptor<? extends Message>> outFaultInterceptors = provider.getOutFaultInterceptors();
        if (outFaultInterceptors != null) {
            ep.getOutFaultInterceptors().addAll(outFaultInterceptors);
            hasAnnotation = true;
        }
        final List<Interceptor<? extends Message>> outInterceptors = provider.getOutInterceptors();
        if (outInterceptors != null) {
            ep.getOutInterceptors().addAll(outInterceptors);
            hasAnnotation = true;
        }
        final List<Feature> features2 = provider.getFeatures();
        if (features2 != null) {
            getFeatures().addAll(features2);
            hasAnnotation = true;
        }

        return hasAnnotation;
    }

    static class QuarkusAnnotationInterceptors extends AnnotationInterceptors {

        private final Class<?>[] clazzes;
        private final String implementorClass;
        private final String endpointString;

        public QuarkusAnnotationInterceptors(String implementorClass, String endpointString, Class<?>... clz) {
            this.implementorClass = implementorClass;
            this.endpointString = endpointString;
            this.clazzes = clz;
        }

        private <T> List<T> getAnnotationObject(Class<? extends Annotation> annotationClazz, Class<T> type) {

            for (Class<?> cls : clazzes) {
                Annotation annotation = cls.getAnnotation(annotationClazz);
                if (annotation != null) {
                    return initializeAnnotationObjects(annotation, type);
                }
            }
            return null;
        }

        private <T> List<T> initializeAnnotationObjects(Annotation annotation,
                Class<T> type) {
            final List<T> result = new ArrayList<>();

            CXFRuntimeUtils.addBeansByType(
                    Arrays.asList(getAnnotationObjectClasses(annotation, type)),
                    type.getName(),
                    implementorClass,
                    endpointString,
                    result);
            CXFRuntimeUtils.addBeans(
                    Arrays.asList(getAnnotationObjectNames(annotation)),
                    type.getName(),
                    implementorClass,
                    endpointString,
                    result);

            return result;
        }

        @SuppressWarnings("unchecked")
        private <T> Class<? extends T>[] getAnnotationObjectClasses(Annotation ann, Class<T> type) { //NOPMD
            if (ann instanceof InFaultInterceptors) {
                return (Class<? extends T>[]) ((InFaultInterceptors) ann).classes();
            } else if (ann instanceof InInterceptors) {
                return (Class<? extends T>[]) ((InInterceptors) ann).classes();
            } else if (ann instanceof OutFaultInterceptors) {
                return (Class<? extends T>[]) ((OutFaultInterceptors) ann).classes();
            } else if (ann instanceof OutInterceptors) {
                return (Class<? extends T>[]) ((OutInterceptors) ann).classes();
            } else if (ann instanceof Features) {
                return (Class<? extends T>[]) ((Features) ann).classes();
            }
            throw new UnsupportedOperationException("Doesn't support the annotation: " + ann);
        }

        private String[] getAnnotationObjectNames(Annotation ann) {
            if (ann instanceof InFaultInterceptors) {
                return ((InFaultInterceptors) ann).interceptors();
            } else if (ann instanceof InInterceptors) {
                return ((InInterceptors) ann).interceptors();
            } else if (ann instanceof OutFaultInterceptors) {
                return ((OutFaultInterceptors) ann).interceptors();
            } else if (ann instanceof OutInterceptors) {
                return ((OutInterceptors) ann).interceptors();
            } else if (ann instanceof Features) {
                return ((Features) ann).features();
            }

            throw new UnsupportedOperationException("Doesn't support the annotation: " + ann);
        }

        private List<Interceptor<? extends Message>> getAnnotationInterceptorList(Class<? extends Annotation> t) {
            @SuppressWarnings("rawtypes")
            List<Interceptor> i = getAnnotationObject(t, Interceptor.class);
            if (i == null) {
                return null;
            }
            List<Interceptor<? extends Message>> m = new ArrayList<>();
            for (Interceptor<?> i2 : i) {
                m.add(i2);
            }
            return m;
        }

        @Override
        public List<Interceptor<? extends Message>> getInFaultInterceptors() {
            return getAnnotationInterceptorList(InFaultInterceptors.class);
        }

        @Override
        public List<Interceptor<? extends Message>> getInInterceptors() {
            return getAnnotationInterceptorList(InInterceptors.class);
        }

        @Override
        public List<Interceptor<? extends Message>> getOutFaultInterceptors() {
            return getAnnotationInterceptorList(OutFaultInterceptors.class);
        }

        @Override
        public List<Interceptor<? extends Message>> getOutInterceptors() {
            return getAnnotationInterceptorList(OutInterceptors.class);
        }

        @Override
        public List<Feature> getFeatures() {
            return getAnnotationObject(Features.class, Feature.class);
        }

    }

}
