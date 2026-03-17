package io.quarkiverse.cxf.test.internal;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QuarkusCxfInternalTestUtil {

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    /**
     * Returns an XPath 1.0 equivalent {@code /*[local-name() = 'foo']/*[local-name() = 'bar']} of XPath 2.0 {@code *:foo/*:bar}
     *
     * @param elementNames
     * @return an XPath 1.0 compatible expression matching the named elements regardless of their namespace
     */
    public static String anyNs(String... elementNames) {
        return Stream.of(elementNames)
                .collect(Collectors.joining("']/*[local-name() = '", "/*[local-name() = '", "']"));
    }

    public static Predicate<String> messageExists(String messageKind, String payload) {
        return msg -> Pattern.compile(
                "^" + messageKind + ".*\\QPayload: " + payload + "\n\\E$",
                Pattern.DOTALL).matcher(msg).matches();
    }

    public static int randomFreePort() {
        while (true) {
            try (ServerSocket ss = new ServerSocket()) {
                ss.setReuseAddress(true);
                ss.bind(new InetSocketAddress((InetAddress) null, 0), 1);

                int port = ss.getLocalPort();
                if (port < 8080 || port > 8500) {
                    /* Avoid some well known testing ports */
                    return port;
                }
            } catch (IOException e) {
                throw new IllegalStateException("Cannot find free port", e);
            }
        }
    }

    public static String maybeWinPath(String path) {
        return path != null && IS_WINDOWS ? path.replace('/', '\\') : path;
    }

    public static <T> T printThreadDumpAtTimeout(Supplier<T> action, Duration timeout, Consumer<String> log) {
        log.accept("Scheduling to print thread dump in " + timeout.toMillis() + " ms");
        return threadDumpAtTimeout(
                action,
                timeout,
                threads -> {
                    StringBuffer threadDump = new StringBuffer(System.lineSeparator());
                    for (ThreadInfo threadInfo : threads.dumpAllThreads(true, true)) {
                        threadDump.append(threadInfo.toString());
                    }
                    log.accept("Thread dump: \n%s".formatted(threadDump.toString()));
                });
    }

    public static <T> T threadDumpAtTimeout(Supplier<T> action, Duration timeout, Consumer<ThreadMXBean> threadsConsumer) {
        final AtomicBoolean latch = new AtomicBoolean(false);
        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> {
            if (!latch.get()) {
                threadsConsumer.accept(ManagementFactory.getThreadMXBean());
            }
            executor.shutdown();
        }, timeout.toMillis(), TimeUnit.MILLISECONDS);
        final T body;
        try {
            body = action.get();
            latch.set(true);
        } catch (Throwable t) {
            throw t;
        }
        executor.shutdown();
        return body;
    }

}
