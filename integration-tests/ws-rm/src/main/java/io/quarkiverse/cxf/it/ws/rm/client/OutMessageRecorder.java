package io.quarkiverse.cxf.it.ws.rm.client;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.MessageSenderInterceptor;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedOutputStreamCallback;
import org.apache.cxf.io.WriteOnCloseOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.addressing.ContextUtils;

public class OutMessageRecorder extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOG = LogUtils.getLogger(OutMessageRecorder.class);
    private List<byte[]> messages = new ArrayList<>();
    private final Object lock = new Object();

    public OutMessageRecorder() {
        super(Phase.PREPARE_SEND);
        addAfter(MessageSenderInterceptor.class.getName());
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        if (!ContextUtils.isRequestor(message)) {
            return;
        }
        OutputStream os = message.getContent(OutputStream.class);
        if (null == os) {
            return;
        }

        WriteOnCloseOutputStream stream = createCachedStream(message, os);
        stream.registerCallback(new RecorderCallback());
    }

    public static WriteOnCloseOutputStream createCachedStream(Message message, OutputStream os) {
        // We need to ensure that we have an output stream which won't start writing the
        // message until we have a chance to send a createsequence
        if (!(os instanceof WriteOnCloseOutputStream)) {
            WriteOnCloseOutputStream cached = new WriteOnCloseOutputStream(os);
            message.setContent(OutputStream.class, cached);
            os = cached;
        }
        return (WriteOnCloseOutputStream) os;
    }

    public List<byte[]> drainMessages() {
        List<byte[]> result;
        synchronized (lock) {
            result = messages;
            messages = new ArrayList<>();
        }
        return result;
    }

    class RecorderCallback implements CachedOutputStreamCallback {

        @Override
        public void onFlush(CachedOutputStream cos) {

        }

        @Override
        public void onClose(CachedOutputStream cos) {
            // bytes were already copied after flush
            try {
                synchronized (lock) {
                    messages.add(cos.getBytes());
                }
            } catch (Exception e) {
                LOG.fine("Can't record message from output stream class: "
                        + cos.getOut().getClass().getName());
            }
        }

    }

}
