package io.quarkiverse.cxf.it.ws.mtom.server;

import java.net.URL;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sun.xml.messaging.saaj.soap.AttachmentPartImpl;

import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class MtomTest {

    @Test
    public void dataHandler() throws Exception {

        /*
         * This is required only in native mode, where the test code is isolated from the server and thus the server
         * does not call AttachmentPartImpl.initializeJavaActivationHandlers() for us
         */
        AttachmentPartImpl.initializeJavaActivationHandlers();

        final URL serviceUrl = new URL(QuarkusCxfClientTestUtil.getServerUrl() + "/mtom?wsdl");
        final QName qName = new QName("https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/mtom",
                MtomService.class.getSimpleName());
        final Service service = javax.xml.ws.Service.create(serviceUrl, qName);
        final MtomService proxy = service.getPort(MtomService.class);

        DataHandler dh = new DataHandler("Hello from client", "text/plain");
        DHResponse response = proxy.echoDataHandler(new DHRequest(dh));
        Assertions.assertThat(response).isNotNull();

        DataHandler dataHandler = response.getDataHandler();
        Assertions.assertThat(dataHandler.getContent()).isEqualTo("Hello from client echoed from the server");
        Assertions.assertThat(dataHandler.getContentType()).isEqualTo("text/plain");

    }

}
