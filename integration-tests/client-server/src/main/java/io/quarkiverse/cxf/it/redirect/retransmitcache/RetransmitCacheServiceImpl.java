package io.quarkiverse.cxf.it.redirect.retransmitcache;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import jakarta.jws.WebService;
import jakarta.xml.ws.AsyncHandler;
import jakarta.xml.ws.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkiverse.cxf.annotation.CXFEndpoint;
import io.quarkus.logging.Log;

@WebService(serviceName = "RetransmitCacheService", targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test")
@CXFEndpoint("/retransmitCache")
public class RetransmitCacheServiceImpl implements RetransmitCacheService {
    @ConfigProperty(name = "qcxf.retransmitCacheDir")
    String retransmitCacheDir;

    @Override
    public Response<RetransmitCacheResponse> retransmitCacheAsync(int expectedFileCount, String payload) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<?> retransmitCacheAsync(int expectedFileCount, String payload,
            AsyncHandler<RetransmitCacheResponse> asyncHandler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RetransmitCacheOutput retransmitCache(int expectedFileCount, String payload) {
        final Properties props = listTempFiles(expectedFileCount, retransmitCacheDir);
        props.put("payload.length", String.valueOf(payload.length()));
        return new RetransmitCacheOutput(toString(props));
    }

    public static String toString(final Properties props) {
        final StringWriter sw = new StringWriter();
        try {
            props.store(sw, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sw.toString();
    }

    public static Properties listTempFiles(int expectedFileCount, String retransmitCacheDir) {
        final int timeout = 3000;
        final long deadline = System.currentTimeMillis() + timeout;
        final Properties props = new Properties();
        final Path dir = Path.of(retransmitCacheDir);
        Log.infof("Expecting %d files in %s", expectedFileCount, retransmitCacheDir);
        if (expectedFileCount == 0) {
            sleep(500);
        }
        final AtomicReference<Exception> lastException = new AtomicReference<>();
        do {
            if (Files.isDirectory(dir)) {
                try (Stream<Path> dirFiles = Files.list(dir)) {
                    dirFiles
                            .filter(RetransmitCacheServiceImpl::isRetransmitFile)
                            .forEach(path -> {
                                Log.infof("Found temp file %s", path);
                                String content;

                                /* We have to wait a bit till the event loop finishes writing to the file */
                                while (true) {
                                    try {
                                        content = Files.readString(path, StandardCharsets.UTF_8);
                                        if (content.endsWith("</payload></ns2:retransmitCache></soap:Body></soap:Envelope>")) {
                                            break;
                                        }
                                    } catch (IOException e) {
                                        lastException.set(e);
                                    }
                                    sleep(50);
                                }
                                props.setProperty(path.toString(), content);
                            });
                } catch (IOException e) {
                    lastException.set(e);
                }
            }
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("" + expectedFileCount + " expected files in " + retransmitCacheDir
                        + " did not appear within " + timeout + " ms; found: " + props.keySet(), lastException.get());
            }
        } while (props.size() < expectedFileCount);

        return props;
    }

    public static boolean isRetransmitFile(Path p) {
        final String fn = p.getFileName().toString();
        return fn.startsWith("qcxf-TempStore-") //  io.quarkiverse.cxf.vertx.http.client.TempStore
                || // org.apache.cxf.io.CachedOutputStream.createFileOutputStream()
                (fn.startsWith("cos") && fn.endsWith("tmp"));
    }

    private static void sleep(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
