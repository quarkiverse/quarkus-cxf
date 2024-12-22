package io.quarkiverse.cxf.it.ws.mtom.server;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import javax.xml.namespace.QName;

import jakarta.activation.DataHandler;
import jakarta.xml.ws.Service;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.assertj.core.api.Assertions;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class MtomTest {

    private static final Logger log = Logger.getLogger(MtomTest.class);
    private static final int KiB = 1024;

    /**
     * A reproducer for
     * <a href=
     * "https://github.com/quarkiverse/quarkus-cxf/issues/973">https://github.com/quarkiverse/quarkus-cxf/issues/973</a>
     *
     * @throws Exception
     */
    @Test
    public void soak() throws Exception {
        // The following fail with
        // Http2Exception: Flow control window exceeded for stream: 0
        // at io.netty.handler.codec.http2.Http2Exception.connectionError(Http2Exception.java:109)
        // final int size = 63 * KiB + 137; // fails at round 0
        // final int size = 63 * KiB + 136; // fails at round 5
        final int size = 63 * KiB + 135; // fails at round 5
        // final int size = 63 * KiB + 134; // fails at round 50
        // final int size = 63 * KiB + 133; // fails at round 50
        // final int size = 63 * KiB + 132; // fails at round 500
        // final int size = 63 * KiB + 131; // fails at round 500
        // final int size = 63 * KiB + 130; // fails at round 5000
        // final int size = 63 * KiB + 129; // fails at round 5000

        // This one fails with a different exception:
        // final int size = 63 * KiB + 128; // fails at round 10763 Corrupted channel by directly writing to native
        // stream
        final int requestCount = Integer
                .parseInt(Optional.ofNullable(System.getenv("QUARKUS_CXF_MTOM_SOAK_ITERATIONS")).orElse("300"));
        log.infof("Performing %d interations", requestCount);
        for (int i = 0; i < requestCount; i++) {
            log.infof("Soaking with %d bytes, round %d", size, i);
            assertMtom(size);
        }
    }

    /**
     * A reproducer for
     * <a href=
     * "https://github.com/quarkiverse/quarkus-cxf/issues/973">https://github.com/quarkiverse/quarkus-cxf/issues/973</a>
     *
     * @throws Exception
     */
    @Test
    public void largeAttachment() throws Exception {

        final int incrementKb = Integer
                .parseInt(Optional.ofNullable(System.getenv("QUARKUS_CXF_MTOM_LARGE_ATTACHMENT_INCREMENT_KB")).orElse("1024"));
        final int increment = incrementKb * KiB;

        final int maxSizeKb = Integer
                .parseInt(Optional.ofNullable(System.getenv("QUARKUS_CXF_MTOM_LARGE_ATTACHMENT_MAX_KB"))
                        .orElse(String.valueOf(10 * KiB)));
        log.infof("maxSizeKb %d ", maxSizeKb);
        final int subtract = 889;
        final int maxSize = (maxSizeKb * KiB) - subtract;

        log.infof("Testing large attachments starting at %d KiB, incrementing %d KiB up to %d KiB - %d bytes", incrementKb,
                incrementKb, maxSizeKb, subtract);
        int lastSize = 0;
        for (int size = increment; size <= maxSize; size += increment) {
            log.infof("Sending large attachment: %d bytes (%d KiB)", size, size / KiB);
            assertMtom(size);
            lastSize = size;
        }
        if (maxSize > lastSize) {
            /* Make sure that we really test the max size */
            log.infof("Sending max sized attachment: %d bytes (%d KiB)", maxSize, maxSize / KiB);
            assertMtom(maxSize);
        }

    }

    static void assertMtom(int size) throws MalformedURLException, IOException {
        final URL serviceUrl = new URL(QuarkusCxfClientTestUtil.getServerUrl() + "/soap/mtom?wsdl");
        final QName qName = new QName("https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/mtom",
                MtomService.class.getSimpleName());
        final Service service = jakarta.xml.ws.Service.create(serviceUrl, qName);
        final MtomService proxy = service.getPort(MtomService.class);
        final Client client = ClientProxy.getClient(proxy);
        final HTTPConduit conduit = (HTTPConduit) client.getConduit();
        /* Avoid read timeouts on GH actions */
        conduit.getClient().setReceiveTimeout(240_000L);

        DataHandler dh = new DataHandler(new RandomBytesDataSource(size));
        DHResponse response = proxy.echoDataHandler(new DHRequest(dh));
        Assertions.assertThat(response).isNotNull();

        DataHandler dataHandler = response.getDataHandler();
        Assertions.assertThat(RandomBytesDataSource.count(dataHandler.getDataSource().getInputStream())).isEqualTo(size);
        Assertions.assertThat(dataHandler.getContentType()).isEqualTo("application/octet-stream");
    }

}
