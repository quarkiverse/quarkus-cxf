package io.quarkiverse.cxf.it.security.policy;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.ext.logging.slf4j.Slf4jVerboseEventSender;

public class BeanProducers {

    @Produces
    @ApplicationScoped
    @Named
    public MessageCollector messageCollector() {
        return new MessageCollector();
    }

    public static class MessageCollector extends LoggingFeature {

        private final Object lock = new Object();
        private List<String> messages = new ArrayList<>();

        public MessageCollector() {
            this.setPrettyLogging(true);
            this.setSender(new Slf4jVerboseEventSender() {
                @Override
                protected String getLogMessage(LogEvent event) {
                    String msg = super.getLogMessage(event);
                    synchronized (lock) {
                        messages.add(msg);
                    }
                    return msg;
                }
            });
        }

        public List<String> drainMessages() {
            synchronized (lock) {
                List<String> result = messages;
                this.messages = new ArrayList<>();
                return result;
            }
        }
    }
}
