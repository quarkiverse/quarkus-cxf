package io.quarkiverse.cxf.it.ws.rm.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

public class MessageRecorder {

    private OutMessageRecorder outRecorder;
    private InMessageRecorder inRecorder;

    public MessageRecorder(OutMessageRecorder or, InMessageRecorder ir) {
        inRecorder = ir;
        outRecorder = or;
    }

    public void awaitMessages(int nExpectedOut, int nExpectedIn, int timeout) {
        int waited = 0;
        int nOut = 0;
        int nIn = 0;
        while (waited <= timeout) {
            nOut = outRecorder.getOutboundMessages().size();
            nIn = inRecorder.getInboundMessages().size();
            if (nIn >= nExpectedIn && nOut >= nExpectedOut) {
                return;
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException ex) {
                // ignore
            }
            waited += 100;
        }
        if (nExpectedIn != nIn) {
            System.out.println((nExpectedIn < nIn ? "excess" : "shortfall")
                    + " of " + Math.abs(nExpectedIn - nIn)
                    + " incoming messages");
            System.out.println("\nMessages actually received:\n");
            List<byte[]> inbound = inRecorder.getInboundMessages();
            for (byte[] b : inbound) {
                System.out.println(new String(b));
                System.out.println("----------------");
            }
        }
        if (nExpectedOut != nOut) {
            System.out.println((nExpectedOut < nOut ? "excess" : "shortfall")
                    + " of " + Math.abs(nExpectedOut - nOut)
                    + " outgoing messages");
            System.out.println("\nMessages actually sent:\n");
            List<byte[]> outbound = outRecorder.getOutboundMessages();
            for (byte[] b : outbound) {
                System.out.println(new String(b));
                System.out.println("----------------");
            }
        }

        assertEquals(nExpectedIn, nIn, "Did not receive expected number of inbound messages");
        assertEquals(nExpectedOut, nOut, "Did not send expected number of outbound messages");
    }
}
