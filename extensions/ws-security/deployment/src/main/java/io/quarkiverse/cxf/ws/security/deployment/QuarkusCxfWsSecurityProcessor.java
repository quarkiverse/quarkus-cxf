package io.quarkiverse.cxf.ws.security.deployment;

import java.util.stream.Stream;

import org.apache.cxf.ws.security.trust.STSLoginModule;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkiverse.cxf.ws.security.CxfWsSecurityConfig.ClientOrEndpointSecurityConfig;
import io.quarkiverse.cxf.ws.security.CxfWsSecurityConfig.StsClientConfig;
import io.quarkiverse.cxf.ws.security.WssFactoryCustomizer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.gizmo.Gizmo;

public class QuarkusCxfWsSecurityProcessor {

    @BuildStep
    FeatureBuildItem feature() {

        final boolean realBcAvailable = isClassLoadable("org.bouncycastle.LICENSE");
        final boolean bcStubAvailable = isClassLoadable("io.quarkiverse.cxf.ws.security.bc.stub.BcStub");
        if (realBcAvailable && bcStubAvailable) {
            throw new IllegalStateException("Bouncy Castle's org.bouncycastle:bcprov-jdk18on artifact found in dependencies."
                    + " To be able to use it, exclude io.quarkiverse.cxf:quarkus-cxf-bc-stub from"
                    + " io.quarkiverse.cxf:quarkus-cxf-rt-ws-security.");
        }
        if (!realBcAvailable && !bcStubAvailable) {
            throw new IllegalStateException("Neither Bouncy Castle's org.bouncycastle:bcprov-jdk18on"
                    + " nor io.quarkiverse.cxf:quarkus-cxf-bc-stub detected in dependencies."
                    + " For quarkus-cxf-rt-ws-security to work properly, either add io.quarkiverse.cxf:quarkus-cxf-bc-stub (if"
                    + " you do not need Bouncy Castle otherwise) or else add org.bouncycastle:bcprov-jdk18on");
        }
        return new FeatureBuildItem("cxf-rt-ws-security");
    }

    private static boolean isClassLoadable(String cl) {
        try {
            Thread.currentThread().getContextClassLoader().loadClass(cl);
            return true;
        } catch (ClassNotFoundException expected) {
            return false;
        }
    }

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencies) {
        Stream.of(
                "org.apache.cxf:cxf-rt-ws-security",
                "org.apache.cxf:cxf-rt-security-saml",
                "org.apache.cxf:cxf-rt-security",
                "org.apache.cxf:cxf-rt-ws-mex")
                .forEach(ga -> {
                    String[] coords = ga.split(":");
                    indexDependencies.produce(new IndexDependencyBuildItem(coords[0], coords[1]));
                });
    }

    @BuildStep
    void reflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.cxf.ws.security.policy.WSSecurityPolicyLoader",
                "org.apache.cxf.ws.security.tokenstore.SecurityToken",
                "org.apache.xml.resolver.CatalogManager", // xml-resolver
                ClientOrEndpointSecurityConfig.class.getName(),
                StsClientConfig.class.getName()).methods().build());

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.cxf.ws.security.cache.CacheCleanupListener").methods().fields().build());

    }

    @BuildStep
    void runtimeInitializedClass(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        Stream.of(
                "org.apache.cxf.rt.security.saml.xacml2.RequestComponentBuilder")
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClass::produce);
    }

    @BuildStep
    void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(WssFactoryCustomizer.class));
    }

    @BuildStep
    BytecodeTransformerBuildItem transformSTSLoginModule() {
        return new BytecodeTransformerBuildItem.Builder()
                .setClassToTransform(STSLoginModule.class.getName())
                .setCacheable(true)
                .setVisitorFunction((className, classVisitor) -> {
                    return new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {
                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                String[] exceptions) {
                            MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);

                            if (name.equals("configureSTSClient")) {
                                /*
                                 * Remove the part of STSLoginModule.configureSTSClient(Message) that references
                                 * org.apache.cxf.bus.spring.SpringBusFactory
                                 * How it was done:
                                 * 1. Go to rt/ws/security/src/main/java/org/apache/cxf/ws/security/trust/STSLoginModule.java
                                 * in CXF source tree
                                 * 2. Remove the block if (cxfSpringCfg != null) {...}
                                 * 3. Save and compile with mvn clean install -DskipTests -Dcheckstyle.skip -Dpmd.skip
                                 * 4. ASM-ify with
                                 * java -cp
                                 * "$HOME/.m2/repository/org/ow2/asm/asm/9.9.1/asm-9.9.1.jar:$HOME/.m2/repository/org/ow2/asm/asm-util/9.9.1/asm-util-9.9.1.jar:/home/ppalaga/m2/repository/org/apache/cxf/cxf-rt-ws-security/4.1.4/cxf-rt-ws-security-4.1.4.jar"
                                 * \
                                 * org.objectweb.asm.util.ASMifier \
                                 * org.apache.cxf.ws.security.trust.STSLoginModule > STSLoginModule-asmized.java
                                 * 5. Copy the code of configureSTSClient() method below
                                 * 6. Adjust the line offset so that the new code refers to original lines
                                 * 7. Do not call visitMaxs
                                 */
                                return new MethodVisitor(Gizmo.ASM_API_VERSION, visitor) {
                                    @Override
                                    public void visitCode() {
                                        final int lineNumberOffset = 8; // we removed 8 lines from the original
                                        super.visitCode();
                                        Label label0 = new Label();
                                        visitLabel(label0);
                                        visitLineNumber(280, label0);
                                        visitVarInsn(Opcodes.ALOAD, 1);
                                        Label label1 = new Label();
                                        visitJumpInsn(Opcodes.IFNONNULL, label1);
                                        Label label2 = new Label();
                                        visitLabel(label2);
                                        visitLineNumber(lineNumberOffset + 281, label2);
                                        visitInsn(Opcodes.ICONST_1);
                                        visitMethodInsn(Opcodes.INVOKESTATIC, "org/apache/cxf/BusFactory", "getDefaultBus",
                                                "(Z)Lorg/apache/cxf/Bus;", false);
                                        visitVarInsn(Opcodes.ASTORE, 3);
                                        Label label3 = new Label();
                                        visitLabel(label3);
                                        visitLineNumber(lineNumberOffset + 282, label3);
                                        visitTypeInsn(Opcodes.NEW, "org/apache/cxf/ws/security/trust/STSClient");
                                        visitInsn(Opcodes.DUP);
                                        visitVarInsn(Opcodes.ALOAD, 3);
                                        visitMethodInsn(Opcodes.INVOKESPECIAL, "org/apache/cxf/ws/security/trust/STSClient",
                                                "<init>", "(Lorg/apache/cxf/Bus;)V", false);
                                        visitVarInsn(Opcodes.ASTORE, 2);
                                        Label label4 = new Label();
                                        visitLabel(label4);
                                        visitLineNumber(lineNumberOffset + 283, label4);
                                        Label label5 = new Label();
                                        visitJumpInsn(Opcodes.GOTO, label5);
                                        visitLabel(label1);
                                        visitLineNumber(lineNumberOffset + 284, label1);
                                        visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                                        visitVarInsn(Opcodes.ALOAD, 1);
                                        visitLdcInsn("sts");
                                        visitMethodInsn(Opcodes.INVOKESTATIC, "org/apache/cxf/ws/security/trust/STSUtils",
                                                "getClient",
                                                "(Lorg/apache/cxf/message/Message;Ljava/lang/String;)Lorg/apache/cxf/ws/security/trust/STSClient;",
                                                false);
                                        visitVarInsn(Opcodes.ASTORE, 2);
                                        visitLabel(label5);
                                        visitLineNumber(lineNumberOffset + 287, label5);
                                        visitFrame(Opcodes.F_APPEND, 1,
                                                new Object[] { "org/apache/cxf/ws/security/trust/STSClient" }, 0, null);
                                        visitVarInsn(Opcodes.ALOAD, 0);
                                        visitFieldInsn(Opcodes.GETFIELD, "org/apache/cxf/ws/security/trust/STSLoginModule",
                                                "wsdlLocation", "Ljava/lang/String;");
                                        Label label6 = new Label();
                                        visitJumpInsn(Opcodes.IFNULL, label6);
                                        Label label7 = new Label();
                                        visitLabel(label7);
                                        visitLineNumber(lineNumberOffset + 288, label7);
                                        visitVarInsn(Opcodes.ALOAD, 2);
                                        visitVarInsn(Opcodes.ALOAD, 0);
                                        visitFieldInsn(Opcodes.GETFIELD, "org/apache/cxf/ws/security/trust/STSLoginModule",
                                                "wsdlLocation", "Ljava/lang/String;");
                                        visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/apache/cxf/ws/security/trust/STSClient",
                                                "setWsdlLocation", "(Ljava/lang/String;)V", false);
                                        visitLabel(label6);
                                        visitLineNumber(lineNumberOffset + 290, label6);
                                        visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                                        visitVarInsn(Opcodes.ALOAD, 0);
                                        visitFieldInsn(Opcodes.GETFIELD, "org/apache/cxf/ws/security/trust/STSLoginModule",
                                                "serviceName", "Ljava/lang/String;");
                                        Label label8 = new Label();
                                        visitJumpInsn(Opcodes.IFNULL, label8);
                                        Label label9 = new Label();
                                        visitLabel(label9);
                                        visitLineNumber(lineNumberOffset + 291, label9);
                                        visitVarInsn(Opcodes.ALOAD, 2);
                                        visitVarInsn(Opcodes.ALOAD, 0);
                                        visitFieldInsn(Opcodes.GETFIELD, "org/apache/cxf/ws/security/trust/STSLoginModule",
                                                "serviceName", "Ljava/lang/String;");
                                        visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/apache/cxf/ws/security/trust/STSClient",
                                                "setServiceName", "(Ljava/lang/String;)V", false);
                                        visitLabel(label8);
                                        visitLineNumber(lineNumberOffset + 293, label8);
                                        visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                                        visitVarInsn(Opcodes.ALOAD, 0);
                                        visitFieldInsn(Opcodes.GETFIELD, "org/apache/cxf/ws/security/trust/STSLoginModule",
                                                "endpointName", "Ljava/lang/String;");
                                        Label label10 = new Label();
                                        visitJumpInsn(Opcodes.IFNULL, label10);
                                        Label label11 = new Label();
                                        visitLabel(label11);
                                        visitLineNumber(lineNumberOffset + 294, label11);
                                        visitVarInsn(Opcodes.ALOAD, 2);
                                        visitVarInsn(Opcodes.ALOAD, 0);
                                        visitFieldInsn(Opcodes.GETFIELD, "org/apache/cxf/ws/security/trust/STSLoginModule",
                                                "endpointName", "Ljava/lang/String;");
                                        visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/apache/cxf/ws/security/trust/STSClient",
                                                "setEndpointName", "(Ljava/lang/String;)V", false);
                                        visitLabel(label10);
                                        visitLineNumber(lineNumberOffset + 296, label10);
                                        visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                                        visitVarInsn(Opcodes.ALOAD, 0);
                                        visitFieldInsn(Opcodes.GETFIELD, "org/apache/cxf/ws/security/trust/STSLoginModule",
                                                "keySize", "I");
                                        Label label12 = new Label();
                                        visitJumpInsn(Opcodes.IFLE, label12);
                                        Label label13 = new Label();
                                        visitLabel(label13);
                                        visitLineNumber(lineNumberOffset + 297, label13);
                                        visitVarInsn(Opcodes.ALOAD, 2);
                                        visitVarInsn(Opcodes.ALOAD, 0);
                                        visitFieldInsn(Opcodes.GETFIELD, "org/apache/cxf/ws/security/trust/STSLoginModule",
                                                "keySize", "I");
                                        visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/apache/cxf/ws/security/trust/STSClient",
                                                "setKeySize", "(I)V", false);
                                        visitLabel(label12);
                                        visitLineNumber(lineNumberOffset + 299, label12);
                                        visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                                        visitVarInsn(Opcodes.ALOAD, 0);
                                        visitFieldInsn(Opcodes.GETFIELD, "org/apache/cxf/ws/security/trust/STSLoginModule",
                                                "keyType", "Ljava/lang/String;");
                                        Label label14 = new Label();
                                        visitJumpInsn(Opcodes.IFNULL, label14);
                                        Label label15 = new Label();
                                        visitLabel(label15);
                                        visitLineNumber(lineNumberOffset + 300, label15);
                                        visitVarInsn(Opcodes.ALOAD, 2);
                                        visitVarInsn(Opcodes.ALOAD, 0);
                                        visitFieldInsn(Opcodes.GETFIELD, "org/apache/cxf/ws/security/trust/STSLoginModule",
                                                "keyType", "Ljava/lang/String;");
                                        visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/apache/cxf/ws/security/trust/STSClient",
                                                "setKeyType", "(Ljava/lang/String;)V", false);
                                        visitLabel(label14);
                                        visitLineNumber(lineNumberOffset + 302, label14);
                                        visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                                        visitVarInsn(Opcodes.ALOAD, 0);
                                        visitFieldInsn(Opcodes.GETFIELD, "org/apache/cxf/ws/security/trust/STSLoginModule",
                                                "tokenType", "Ljava/lang/String;");
                                        Label label16 = new Label();
                                        visitJumpInsn(Opcodes.IFNULL, label16);
                                        Label label17 = new Label();
                                        visitLabel(label17);
                                        visitLineNumber(lineNumberOffset + 303, label17);
                                        visitVarInsn(Opcodes.ALOAD, 2);
                                        visitVarInsn(Opcodes.ALOAD, 0);
                                        visitFieldInsn(Opcodes.GETFIELD, "org/apache/cxf/ws/security/trust/STSLoginModule",
                                                "tokenType", "Ljava/lang/String;");
                                        visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/apache/cxf/ws/security/trust/STSClient",
                                                "setTokenType", "(Ljava/lang/String;)V", false);
                                        visitLabel(label16);
                                        visitLineNumber(lineNumberOffset + 305, label16);
                                        visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                                        visitVarInsn(Opcodes.ALOAD, 0);
                                        visitFieldInsn(Opcodes.GETFIELD, "org/apache/cxf/ws/security/trust/STSLoginModule",
                                                "namespace", "Ljava/lang/String;");
                                        Label label18 = new Label();
                                        visitJumpInsn(Opcodes.IFNULL, label18);
                                        Label label19 = new Label();
                                        visitLabel(label19);
                                        visitLineNumber(lineNumberOffset + 306, label19);
                                        visitVarInsn(Opcodes.ALOAD, 2);
                                        visitVarInsn(Opcodes.ALOAD, 0);
                                        visitFieldInsn(Opcodes.GETFIELD, "org/apache/cxf/ws/security/trust/STSLoginModule",
                                                "namespace", "Ljava/lang/String;");
                                        visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/apache/cxf/ws/security/trust/STSClient",
                                                "setNamespace", "(Ljava/lang/String;)V", false);
                                        visitLabel(label18);
                                        visitLineNumber(lineNumberOffset + 309, label18);
                                        visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                                        visitVarInsn(Opcodes.ALOAD, 2);
                                        visitVarInsn(Opcodes.ALOAD, 0);
                                        visitFieldInsn(Opcodes.GETFIELD, "org/apache/cxf/ws/security/trust/STSLoginModule",
                                                "stsClientProperties", "Ljava/util/Map;");
                                        visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/apache/cxf/ws/security/trust/STSClient",
                                                "setProperties", "(Ljava/util/Map;)V", false);
                                        Label label20 = new Label();
                                        visitLabel(label20);
                                        visitLineNumber(lineNumberOffset + 311, label20);
                                        visitVarInsn(Opcodes.ALOAD, 0);
                                        visitFieldInsn(Opcodes.GETFIELD, "org/apache/cxf/ws/security/trust/STSLoginModule",
                                                "requireRoles", "Z");
                                        Label label21 = new Label();
                                        visitJumpInsn(Opcodes.IFEQ, label21);
                                        visitVarInsn(Opcodes.ALOAD, 2);
                                        visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/apache/cxf/ws/security/trust/STSClient",
                                                "getClaimsCallbackHandler", "()Ljavax/security/auth/callback/CallbackHandler;",
                                                false);
                                        visitJumpInsn(Opcodes.IFNONNULL, label21);
                                        Label label22 = new Label();
                                        visitLabel(label22);
                                        visitLineNumber(lineNumberOffset + 312, label22);
                                        visitVarInsn(Opcodes.ALOAD, 2);
                                        visitTypeInsn(Opcodes.NEW,
                                                "org/apache/cxf/ws/security/trust/claims/RoleClaimsCallbackHandler");
                                        visitInsn(Opcodes.DUP);
                                        visitMethodInsn(Opcodes.INVOKESPECIAL,
                                                "org/apache/cxf/ws/security/trust/claims/RoleClaimsCallbackHandler", "<init>",
                                                "()V", false);
                                        visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/apache/cxf/ws/security/trust/STSClient",
                                                "setClaimsCallbackHandler", "(Ljavax/security/auth/callback/CallbackHandler;)V",
                                                false);
                                        visitLabel(label21);
                                        visitLineNumber(lineNumberOffset + 315, label21);
                                        visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                                        visitVarInsn(Opcodes.ALOAD, 2);
                                        visitInsn(Opcodes.ARETURN);
                                        Label label23 = new Label();
                                        visitLabel(label23);
                                        visitLocalVariable("bus", "Lorg/apache/cxf/Bus;", null, label3, label4, 3);
                                        visitLocalVariable("c", "Lorg/apache/cxf/ws/security/trust/STSClient;", null, label4,
                                                label1, 2);
                                        visitLocalVariable("this", "Lorg/apache/cxf/ws/security/trust/STSLoginModule;", null,
                                                label0, label23, 0);
                                        visitLocalVariable("msg", "Lorg/apache/cxf/message/Message;", null, label0, label23, 1);
                                        visitLocalVariable("c", "Lorg/apache/cxf/ws/security/trust/STSClient;", null, label5,
                                                label23, 2);
                                        //visitMaxs(3, 4);
                                        visitEnd();
                                    }
                                };
                            }
                            return visitor;
                        }
                    };
                })
                .build();
    }
}
