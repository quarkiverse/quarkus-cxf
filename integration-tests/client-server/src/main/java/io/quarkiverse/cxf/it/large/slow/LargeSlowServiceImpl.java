package io.quarkiverse.cxf.it.large.slow;

import java.util.concurrent.Future;

import jakarta.jws.WebService;
import jakarta.xml.ws.AsyncHandler;
import jakarta.xml.ws.Response;

import io.quarkiverse.cxf.annotation.CXFEndpoint;
import io.quarkiverse.cxf.it.large.slow.generated.LargeSlowOutput;
import io.quarkiverse.cxf.it.large.slow.generated.LargeSlowResponse;
import io.quarkiverse.cxf.it.large.slow.generated.LargeSlowService;
import io.quarkus.logging.Log;

@WebService(serviceName = "LargeSlowService", targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test")
@CXFEndpoint("/largeSlow")
public class LargeSlowServiceImpl implements LargeSlowService {

    @Override
    public LargeSlowOutput largeSlow(
            int sizeBytes,
            int clientDeserializationDelayMs,
            int serviceExecutionDelayMs) {
        Log.infof("Prolonging LargeSlowServiceImpl.largeSlow() execution by %d ms", serviceExecutionDelayMs);
        if (serviceExecutionDelayMs > 0) {
            try {
                Thread.sleep(serviceExecutionDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        return new LargeSlowOutput(clientDeserializationDelayMs, largeString(sizeBytes));
    }

    public static String largeString(int sizeBytes) {
        final StringBuilder sb = new StringBuilder();
        while (sb.length() < sizeBytes) {
            sb.append("0123456789");
        }
        sb.setLength(sizeBytes);
        return sb.toString();
    }

    @Override
    public Response<LargeSlowResponse> largeSlowAsync(int sizeBytes,
            int clientDeserializationDelayMs,
            int serviceExecutionDelayMs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<?> largeSlowAsync(int sizeBytes,
            int clientDeserializationDelayMs,
            int serviceExecutionDelayMs, AsyncHandler<LargeSlowResponse> asyncHandler) {
        throw new UnsupportedOperationException();
    }
}
