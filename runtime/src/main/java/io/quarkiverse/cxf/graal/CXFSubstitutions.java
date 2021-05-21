package io.quarkiverse.cxf.graal;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.annotations.FastInfoset;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ReflectionInvokationHandler;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.databinding.WrapperHelper;
import org.apache.cxf.interceptor.FIStaxInInterceptor;
import org.apache.cxf.interceptor.FIStaxOutInterceptor;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkiverse.cxf.CXFException;

@TargetClass(className = "org.apache.cxf.wsdl.ExtensionClassGenerator")
final class Target_org_apache_cxf_wsdl_ExtensionClassGenerator {
    @Substitute()
    public Class<?> createExtensionClass(Class<?> cls, QName qname, ClassLoader loader) {
        Logger LOG = LogUtils.getL7dLogger(Target_org_apache_cxf_wsdl_ExtensionClassGenerator.class);
        try {
            LOG.info("extensibility class substitute: " + cls.getName());
            Class<?> clz = Class.forName("io.quarkiverse.cxf." + cls.getSimpleName() + "Extensibility");
            return clz;
        } catch (ClassNotFoundException e) {
            LOG.warning("extensibility class to create: " + cls.getName());
            throw new UnsupportedOperationException(
                    cls.getName() + " extensibility not implemented yet for GraalVM native images", e);
            // TODO CORBA support : org.apache.cxf.wsdl.http.OperationType and org.apache.cxf.wsdl.http.BindingType
        }
    }
}

@TargetClass(className = "org.apache.cxf.jaxb.FactoryClassGenerator")
final class Target_org_apache_cxf_jaxb_FactoryClassGenerator {
    @Substitute()
    private Class<?> createFactory(Class<?> cls) {
        Logger LOG = LogUtils.getL7dLogger(Target_org_apache_cxf_jaxb_FactoryClassGenerator.class);
        try {
            LOG.info("substitute  JAXBContextInitializer.createFactory class for : " + cls.getSimpleName());
            return Class.forName("io.quarkiverse.cxf." + cls.getSimpleName() + "Factory");
        } catch (ClassNotFoundException e) {
            LOG.warning("factory class to create : " + cls.getSimpleName());
            throw new UnsupportedOperationException(cls.getName() + " factory not implemented yet for GraalVM native images",
                    e);
        }
    }
}

@TargetClass(className = "org.apache.cxf.jaxb.WrapperHelperClassGenerator")
final class Target_org_apache_cxf_jaxb_WrapperHelperClassGenerator {

    @Alias
    public static String computeSignature(Method[] setMethods, Method[] getMethods) {
        return null;
    }

    @Substitute()
    public WrapperHelper compile(Class<?> wrapperType, Method[] setMethods,
            Method[] getMethods, Method[] jaxbMethods,
            Field[] fields, Object objectFactory) {
        Logger LOG = LogUtils.getL7dLogger(Target_org_apache_cxf_jaxb_WrapperHelperClassGenerator.class);
        LOG.info("compileWrapperHelper substitution");
        int count = 1;
        String newClassName = wrapperType.getName() + "_WrapperTypeHelper" + count;
        newClassName = newClassName.replaceAll("\\$", ".");
        newClassName = newClassName.replace('/', '.');
        Class<?> cls = null;
        try {
            cls = Thread.currentThread().getContextClassLoader().loadClass(newClassName);
        } catch (ClassNotFoundException e) {
            LOG.warning("Wrapper helper class not found : " + e.toString());
        }
        while (cls != null) {
            try {
                WrapperHelper helper = WrapperHelper.class.cast(cls.getConstructor().newInstance());
                if (!helper.getSignature().equals(computeSignature(setMethods, getMethods))) {
                    LOG.warning("signature of helper : " + helper.getSignature()
                            + " is not equal to : " + computeSignature(setMethods, getMethods));
                    count++;
                    newClassName = wrapperType.getName() + "_WrapperTypeHelper" + count;
                    newClassName = newClassName.replaceAll("\\$", ".");
                    newClassName = newClassName.replace('/', '.');
                    try {
                        cls = Thread.currentThread().getContextClassLoader().loadClass(newClassName);
                    } catch (ClassNotFoundException e) {
                        LOG.warning("Wrapper helper class not found : " + e.toString());
                        break;
                    }
                } else {
                    return helper;
                }
            } catch (Exception e) {
                return null;
            }
        }

        WrapperHelper helper = null;
        try {
            if (cls != null) {
                helper = WrapperHelper.class.cast(cls.getConstructor().newInstance());
                return helper;
            }
        } catch (Exception e) {
            LOG.warning("Wrapper helper class not created : " + e.toString());
        }
        throw new UnsupportedOperationException(cls.getName() + " wrapperHelper not implemented yet for GraalVM native images");
    }

}

@TargetClass(className = "org.apache.cxf.endpoint.dynamic.ExceptionClassGenerator")
final class Target_org_apache_cxf_endpoint_dynamic_ExceptionClassGenerator {

    @Substitute
    public Class<?> createExceptionClass(Class<?> bean) throws ClassNotFoundException {
        Logger LOG = LogUtils.getL7dLogger(org.apache.cxf.endpoint.dynamic.TypeClassInitializer.class);
        LOG.info("Substitute TypeClassInitializer$ExceptionCreator.createExceptionClass");
        //TODO not sure if I use CXFException or generated one. I have both system in place. but I use CXFEx currently.
        String newClassName = CXFException.class.getSimpleName();

        try {
            Class<?> clz = Class.forName("io.quarkiverse.cxf." + newClassName);
            return clz;
        } catch (ClassNotFoundException e) {
            try {
                Class<?> clz = Class.forName("io.quarkiverse.cxf.CXFException");
                return clz;
            } catch (ClassNotFoundException ex) {
                throw new UnsupportedOperationException(
                        newClassName + " exception not implemented yet for GraalVM native images", ex);
            }
        }
    }
}

@TargetClass(className = "org.apache.cxf.common.spi.NamespaceClassGenerator")
final class Target_org_apache_cxf_common_spi_NamespaceClassGenerator {
    @Alias
    private static Logger LOG = null;

    @Substitute
    private synchronized Class<?> createNamespaceWrapperClass(Class<?> mcls, Map<String, String> map) {
        LOG.info("Substitute NamespaceClassGenerator.createNamespaceWrapper");
        Class<?> NamespaceWrapperClass = null;
        Throwable t = null;
        try {
            NamespaceWrapperClass = Class.forName("org.apache.cxf.jaxb.EclipseNamespaceMapper");
        } catch (ClassNotFoundException e) {
            // ignore
            t = e;
        }
        if (NamespaceWrapperClass == null) {
            try {
                NamespaceWrapperClass = Class.forName("org.apache.cxf.jaxb.NamespaceMapper");
            } catch (ClassNotFoundException e) {
                // ignore
                t = e;
            }
        }
        if (NamespaceWrapperClass == null) {
            try {
                NamespaceWrapperClass = Class.forName("org.apache.cxf.jaxb.NamespaceMapperRI");
            } catch (ClassNotFoundException e) {
                // ignore
                t = e;
            }
        }
        if (NamespaceWrapperClass == null && (!mcls.getName().contains(".internal.") && mcls.getName().contains("com.sun"))) {
            try {
                NamespaceWrapperClass = Class.forName("org.apache.cxf.common.jaxb.NamespaceMapper");
            } catch (Throwable ex2) {
                // ignore
                t = ex2;
            }
        }
        if (NamespaceWrapperClass != null) {
            try {
                return NamespaceWrapperClass;
            } catch (Exception e) {
                // ignore
                t = e;
            }
        }
        LOG.log(Level.INFO, "Could not create a NamespaceMapper compatible with Marshaller class " + mcls.getName(), t);
        return null;
    }
}

@TargetClass(className = "org.apache.cxf.common.util.ReflectionInvokationHandler")
final class Target_org_apache_cxf_common_util_ReflectionInvokationHandler {
    @Alias
    private Object target;

    @Alias
    private Class<?>[] getParameterTypes(Method method, Object[] args) {
        return null;
    }

    @Substitute
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //add this to handle args null bug
        if (args == null)
            args = new Object[0];
        ReflectionInvokationHandler.WrapReturn wr = (ReflectionInvokationHandler.WrapReturn) method
                .getAnnotation(ReflectionInvokationHandler.WrapReturn.class);
        Class<?> targetClass = this.target.getClass();
        Class[] parameterTypes = this.getParameterTypes(method, args);

        int i;
        int x;
        try {
            Method m;
            try {
                m = targetClass.getMethod(method.getName(), parameterTypes);
            } catch (NoSuchMethodException var20) {
                boolean[] optionals = new boolean[method.getParameterTypes().length];
                i = 0;
                int optionalNumber = 0;
                Annotation[][] var25 = method.getParameterAnnotations();
                x = var25.length;

                int argI;
                for (argI = 0; argI < x; ++argI) {
                    Annotation[] a = var25[argI];
                    optionals[i] = false;
                    Annotation[] var16 = a;
                    int var17 = a.length;

                    for (int var18 = 0; var18 < var17; ++var18) {
                        Annotation potential = var16[var18];
                        if (ReflectionInvokationHandler.Optional.class.equals(potential.annotationType())) {
                            optionals[i] = true;
                            ++optionalNumber;
                            break;
                        }
                    }

                    ++i;
                }

                Class<?>[] newParams = new Class[args.length - optionalNumber];
                Object[] newArgs = new Object[args.length - optionalNumber];
                argI = 0;

                for (int j = 0; j < parameterTypes.length; ++j) {
                    if (!optionals[j]) {
                        newArgs[argI] = args[j];
                        newParams[argI] = parameterTypes[j];
                        ++argI;
                    }
                }

                m = targetClass.getMethod(method.getName(), newParams);
                args = newArgs;
            }

            ReflectionUtil.setAccessible(m);
            return wrapReturn(wr, m.invoke(this.target, args));
        } catch (InvocationTargetException var21) {
            throw var21.getCause();
        } catch (NoSuchMethodException var22) {
            Method[] var8 = targetClass.getMethods();
            int var9 = var8.length;

            for (i = 0; i < var9; ++i) {
                Method m2 = var8[i];
                if (m2.getName().equals(method.getName())
                        && m2.getParameterTypes().length == method.getParameterTypes().length) {
                    boolean found = true;

                    for (x = 0; x < m2.getParameterTypes().length; ++x) {
                        if (args[x] != null && !m2.getParameterTypes()[x].isInstance(args[x])) {
                            found = false;
                        }
                    }

                    if (found) {
                        ReflectionUtil.setAccessible(m2);
                        return wrapReturn(wr, m2.invoke(this.target, args));
                    }
                }
            }

            throw var22;
        }
    }

    @Alias
    private static Object wrapReturn(ReflectionInvokationHandler.WrapReturn wr, Object t) {
        return null;
    }
}

@TargetClass(className = "org.apache.cxf.common.util.ASMHelperImpl")
final class Target_org_apache_cxf_common_util_ASMHelperImpl {

    @Substitute
    private Class<?> getASMClassWriterClass() {
        return null;
    }

}

/**
 * Verifies if the FastInfoset classes are missing from the classpath. Since
 * testing all classes would be infeasible, we test the few classes used by
 * {@link FIStaxInInterceptor} and {@link FIStaxOutInterceptor}, which are
 * themselves the main users of FastInfoset.
 */
class FastInfosetMissing implements BooleanSupplier {

    /**
     * Used by {@link FIStaxInInterceptor}.
     */
    private static final String PARSER_CLASSNAME = "com.sun.xml.fastinfoset.stax.StAXDocumentParser";

    /**
     * Used by {@link FIStaxOutInterceptor}.
     */
    private static final String SERIALIZER_CLASSNAME = "com.sun.xml.fastinfoset.stax.StAXDocumentSerializer";

    @Override
    public boolean getAsBoolean() {
        try {
            Class.forName(PARSER_CLASSNAME);
        } catch (ClassNotFoundException ex) {
            return true;
        }

        try {
            Class.forName(SERIALIZER_CLASSNAME);
        } catch (ClassNotFoundException ex) {
            return true;
        }

        return false;
    }

}

/**
 * Substitutes {@link FIStaxInInterceptor} when FastInfoset classes are not
 * found in the classpath.
 */
@TargetClass(className = "org.apache.cxf.interceptor.FIStaxInInterceptor", onlyWith = FastInfosetMissing.class)
final class Target_org_apache_cxf_interceptor_FIStaxInInterceptor {

    @Substitute
    private XMLStreamReader getParser(InputStream in) {
        throw new UnsupportedOperationException(
                "FastInfoset support was requested but its classes are not present in the classpath.");
    }

}

/**
 * Substitutes {@link FIStaxOutInterceptor} when FastInfoset classes are not
 * found in the classpath.
 */
@TargetClass(className = "org.apache.cxf.interceptor.FIStaxOutInterceptor", onlyWith = FastInfosetMissing.class)
final class Target_org_apache_cxf_interceptor_FIStaxOutInterceptor {

    @Substitute
    private XMLStreamWriter getOutput(OutputStream out) {
        throw new UnsupportedOperationException(
                "FastInfoset support was requested but its classes are not present in the classpath.");
    }

}

public class CXFSubstitutions {
}
