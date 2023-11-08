package io.quarkiverse.cxf.it.ws.rm.client;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.addressing.ContextUtils;

@ApplicationScoped
public class InMessageRecorder extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOG = LogUtils.getLogger(InMessageRecorder.class);
    private List<byte[]> messages = new ArrayList<>();
    private final Object lock = new Object();

    public InMessageRecorder() {
        super(Phase.RECEIVE);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        if (!ContextUtils.isRequestor(message)) {
            return;
        }
        try (InputStream is = message.getContent(InputStream.class)) {
            if (is != null) {
                int i = is.available();
                if (i < 4096) {
                    i = 4096;
                }
                LoadingByteArrayOutputStream bout = new LoadingByteArrayOutputStream(i);
                IOUtils.copy(is, bout);
                is.close();

                synchronized (lock) {
                    messages.add(bout.toByteArray());
                }
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("inbound: " + new String(bout.toByteArray()));
                }
                message.setContent(InputStream.class, bout.createInputStream());
            }
        } catch (Exception ex) {
            throw new Fault(ex);
        }
    }

    public List<byte[]> drainMessages() {
        List<byte[]> result;
        synchronized (lock) {
            result = messages;
            messages = new ArrayList<>();
        }
        return result;
    }
}
