package io.quarkiverse.cxf.deployment;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.bootstrap.BootstrapDebug;
import io.quarkus.gizmo.ClassOutput;

/**
 * Simple bean recorder.
 *
 * <p>
 * Creates a GeneratedBeanBuildItem out of every class written to interface ClassOutput.
 */
public class GeneratedBeanRecorder implements ClassOutput {
    private static final Logger LOGGER = Logger.getLogger(GeneratedBeanRecorder.class);
    private final Collection<GeneratedBeanBuildItem> sink = new ArrayList<>();
    private final Map<String, StringWriter> sources;

    public GeneratedBeanRecorder() {
        this.sources = BootstrapDebug.DEBUG_SOURCES_DIR != null ? new ConcurrentHashMap<>() : null;
    }

    public Collection<GeneratedBeanBuildItem> getGeneratedBeans() {
        return sink;
    }

    public void write(
            String className,
            byte[] bytes) {
        String source = null;
        if (this.sources != null) {
            StringWriter sw = this.sources.get(className);
            if (sw != null) {
                source = sw.toString();
            }
        }
        //
        // generate new bean out of classname and bytes.
        //
        this.sink.add(new GeneratedBeanBuildItem(className, bytes, source));
    }

    public Writer getSourceWriter(String className) {
        Writer r;
        if (this.sources != null) {
            StringWriter writer = new StringWriter();
            this.sources.put(className, writer);
            r = writer;
        } else {
            r = ClassOutput.super.getSourceWriter(className);
        }
        return r;
    }
}
