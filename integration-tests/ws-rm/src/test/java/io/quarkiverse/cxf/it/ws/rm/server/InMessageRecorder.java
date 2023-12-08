package io.quarkiverse.cxf.it.ws.rm.server;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.addressing.ContextUtils;

public class InMessageRecorder extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOG = LogUtils.getLogger(InMessageRecorder.class);
    private final List<byte[]> inbound = new CopyOnWriteArrayList<>();

    public InMessageRecorder() {
        super(Phase.RECEIVE);
    }

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

                inbound.add(bout.toByteArray());
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("inbound: " + new String(bout.toByteArray()));
                }
                message.setContent(InputStream.class, bout.createInputStream());
            }
        } catch (Exception ex) {
            throw new Fault(ex);
        }
    }

    public List<byte[]> getInboundMessages() {
        return inbound;
    }
}
