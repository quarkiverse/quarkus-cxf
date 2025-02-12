package io.quarkiverse.cxf.rt.management.deployment;

import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.apache.cxf.management.jmx.InstrumentationManagerImpl;
import org.jboss.logging.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.gizmo.Gizmo;

public class QuarkusCxfRtManagementProcessor {

    private static final Logger log = Logger.getLogger(QuarkusCxfRtManagementProcessor.class);

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencies) {
        Stream.of(
                "org.apache.cxf:cxf-rt-management")
                .forEach(ga -> {
                    String[] coords = ga.split(":");
                    indexDependencies.produce(new IndexDependencyBuildItem(coords[0], coords[1]));
                });
    }

    @BuildStep(onlyIf = NativeBuild.class)
    void transfromByteCode(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformers) {

        /*
         * Make InstrumentationManagerImpl.init() a no-op in native mode
         * to avoid getting an MBean Server instance in the native image heap
         * See https://github.com/quarkiverse/quarkus-cxf/issues/1697
         */
        final BytecodeTransformerBuildItem transformation = new BytecodeTransformerBuildItem.Builder()
                .setClassToTransform(InstrumentationManagerImpl.class.getName())
                .setCacheable(true)
                .setVisitorFunction(new NoInitTransformer())
                .build();
        bytecodeTransformers.produce(transformation);
    }

    static class NoInitTransformer implements BiFunction<String, ClassVisitor, ClassVisitor> {

        @Override
        public ClassVisitor apply(String t, ClassVisitor classVisitor) {
            return new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {
                private boolean initTransformed = false;

                @Override
                public MethodVisitor visitMethod(int access,
                        String name,
                        String descriptor,
                        String signature,
                        String[] exceptions) {
                    if (name.equals("init")
                            && descriptor.equals("()V")
                            && (access & Opcodes.ACC_PUBLIC) != 0) {
                        initTransformed = true;
                        final MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

                        return new MethodVisitor(api, mv) {

                            @Override
                            public void visitCode() {
                                /* Replace method body with a single RETURN to make it do nothing */
                                visitInsn(Opcodes.RETURN);
                                visitMaxs(0, 0);
                                visitEnd();
                            }
                        };
                    }
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }

                @Override
                public void visitEnd() {
                    if (!initTransformed) {
                        throw new IllegalStateException(
                                InstrumentationManagerImpl.class.getName() + ".init() method not found");
                    }
                    super.visitEnd();
                }

            };
        }
    }

}
