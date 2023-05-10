package io.quarkiverse.cxf.doc.it;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.BaseConsumer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.OutputFrame.OutputType;
import org.testcontainers.containers.output.WaitingConsumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AntoraTest {

    private static final int INVALID_UID = -1;
    private static Logger log = LoggerFactory.getLogger(AntoraTest.class);

    @Test
    public void antora() throws TimeoutException, IOException {

        final Path targetDir = Paths.get("target");
        if (!Files.isDirectory(targetDir)) {
            try {
                Files.createDirectories(targetDir);
            } catch (IOException e) {
                throw new RuntimeException("Could not create " + targetDir);
            }
        }

        /*
         * We need the current user's uid so that Antora container generates the files as that user
         * so that they can be later deleted with mvn clean
         */
        int uid = INVALID_UID;
        try {
            uid = (Integer) Files.getAttribute(targetDir, "unix:uid");
            log.info("Detected unix:uid " + uid);
        } catch (Exception e) {
            if (System.getProperty("os.name").toLowerCase().indexOf("win") < 0) {
                /* Warn on non-Windows, ignore otherwise */
                log.warn("Could not read unix:uid of " + targetDir + " directory", e);
            }
        }
        final int finalUid = uid;

        WaitingConsumer logConsumer = new WaitingConsumer().withRemoveAnsiCodes(true);

        AntoraFrameConsumer antoraFrameConsumer = new AntoraFrameConsumer();

        try (GenericContainer<?> antoraContainer = new GenericContainer<>("antora/antora:3.0.1")) {
            if (finalUid >= 0) {
                antoraContainer
                        .withCreateContainerCmdModifier(cmd -> {
                            cmd.withUser(String.valueOf(finalUid));
                        });
            }
            antoraContainer
                    .withFileSystemBind(Paths.get("./..").toAbsolutePath().normalize().toString(), "/antora",
                            BindMode.READ_WRITE)
                    .withCommand("--cache-dir=./target/antora-cache", "docs/antora-playbook.yml")
                    .withLogConsumer(antoraFrameConsumer)
                    .withLogConsumer(logConsumer);
            antoraContainer.start();
            logConsumer.waitUntilEnd(30, TimeUnit.SECONDS);
        }
        antoraFrameConsumer.assertNoErrors();

        final String antoraYml = Files.readString(Paths.get("antora.yml"), StandardCharsets.UTF_8);
        final String re = "\nversion: *([^ \n\t\r]*)";
        final Matcher m = Pattern.compile(re).matcher(antoraYml);
        if (!m.find()) {
            throw new IllegalStateException("Unable to find " + re + " in antora.yml");
        }
        final String antoraVersion = m.group(1);
        Assertions.assertNotNull(antoraVersion);

        Path siteIndexHtml = Paths.get("target/site/quarkus-cxf/" + antoraVersion + "/index.html");
        Assertions.assertTrue(Files.isRegularFile(siteIndexHtml), siteIndexHtml + " not found");
        System.out.println("\nYou may want to open\n\n    " + siteIndexHtml + "\n\nin browser");

    }

    static class AntoraFrame {
        String level;
        long time;
        String name;
        AntoraFile file;
        AntoraSource source;
        String msg;
        String hint;
        List<AntoraStackFrame> stack;

        public String getLevel() {
            return level;
        }

        public long getTime() {
            return time;
        }

        public String getName() {
            return name;
        }

        public AntoraFile getFile() {
            return file;
        }

        public AntoraSource getSource() {
            return source;
        }

        public String getMsg() {
            return msg;
        }

        public List<AntoraStackFrame> getStack() {
            return stack;
        }

        @Override
        public String toString() {
            return file + ": " + msg
                    + (hint != null ? (" " + hint) : "")
                    + (stack != null && !stack.isEmpty()
                            ? ("\n    at " + stack.stream()
                                    .map(AntoraStackFrame::toString)
                                    .collect(Collectors.joining("\n    at ")))
                            : "");
        }

        static class AntoraSource {
            String url;
            String worktree;
            String refname;
            String startPath;

            public String getUrl() {
                return url;
            }

            public String getWorktree() {
                return worktree;
            }

            public String getRefname() {
                return refname;
            }

            public String getStartPath() {
                return startPath;
            }
        }

        static class AntoraFile {
            String path;
            int line;

            @Override
            public String toString() {
                return path + ":" + line;
            }

            public String getPath() {
                return path;
            }

            public int getLine() {
                return line;
            }
        }

        static class AntoraStackFrame {
            AntoraFile file;
            AntoraSource source;

            public AntoraFile getFile() {
                return file;
            }

            public AntoraSource getSource() {
                return source;
            }

            @Override
            public String toString() {
                return file != null ? file.toString() : "";
            }
        }
    }

    private static class AntoraFrameConsumer extends BaseConsumer<AntoraFrameConsumer> {

        private final ObjectMapper mapper;
        private final List<AntoraFrame> frames = new ArrayList<>();
        private final Map<String, JsonProcessingException> exceptions = new LinkedHashMap<>();

        public AntoraFrameConsumer() {
            mapper = new ObjectMapper();

        }

        @Override
        public void accept(OutputFrame t) {
            if (t.getType() == OutputType.END) {
                return;
            }
            final String rawFrame = t.getUtf8String();
            try {
                final AntoraFrame frame = mapper.readValue(rawFrame, AntoraFrame.class);
                synchronized (frames) {
                    frames.add(frame);
                }
                switch (frame.getLevel()) {
                    case "info":
                        log.info(frame.toString());
                        break;
                    case "warn":
                        log.warn(frame.toString());
                        break;
                    case "error":
                        log.error(frame.toString());
                        break;
                    default:
                        throw new IllegalStateException("Unexpected AntoraFrame.level " + frame.getLevel());
                }
            } catch (JsonProcessingException e) {
                synchronized (exceptions) {
                    exceptions.put(rawFrame, e);
                }
            }
        }

        public void assertNoErrors() {
            synchronized (exceptions) {
                if (!exceptions.isEmpty()) {
                    Entry<String, JsonProcessingException> e = exceptions.entrySet().iterator().next();
                    throw new RuntimeException("Could not parse AntoraFrame " + e.getKey(), e.getValue());
                }
            }

            synchronized (frames) {
                String errors = frames.stream()
                        .filter(f -> f.getLevel().equals("error"))
                        .map(AntoraFrame::toString)
                        .collect(Collectors.joining("\n"));
                if (errors != null && !errors.isEmpty()) {
                    Assertions.fail(errors);
                }
            }
        }

    }

}
