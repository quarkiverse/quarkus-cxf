package io.quarkiverse.cxf.transport;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import org.apache.cxf.message.Message;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

public class VertxConduit extends AbstractConduit {

    public VertxConduit(EndpointReferenceType t) {
        super(t);
    }

    //TODO call onMessage()
    @Override
    protected Logger getLogger() {
        return null;
    }

    @Override
    public void prepare(Message message) throws IOException {

    }

    @Override
    public void close(Message msg) throws IOException {
        InputStream in = (InputStream) msg.getContent(InputStream.class);

        try {
            if (in != null) {
                int count = 0;

                for (byte[] buffer = new byte[1024]; in.read(buffer) != -1 && count < 25; ++count) {
                }
            }
        } finally {
            super.close(msg);
        }

    }
}
