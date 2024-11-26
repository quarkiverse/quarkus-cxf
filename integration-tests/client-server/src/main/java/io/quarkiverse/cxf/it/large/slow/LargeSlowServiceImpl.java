package io.quarkiverse.cxf.it.large.slow;

import java.util.concurrent.Future;

import jakarta.jws.WebService;
import jakarta.xml.ws.AsyncHandler;
import jakarta.xml.ws.Response;

import io.quarkiverse.cxf.annotation.CXFEndpoint;
import io.quarkiverse.cxf.it.large.slow.generated.LargeSlowOutput;
import io.quarkiverse.cxf.it.large.slow.generated.LargeSlowResponse;
import io.quarkiverse.cxf.it.large.slow.generated.LargeSlowService;

@WebService(serviceName = "LargeSlowService", targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test")
@CXFEndpoint("/largeSlow")
public class LargeSlowServiceImpl implements LargeSlowService {

    @Override
    public LargeSlowOutput largeSlow(int sizeBytes, int delayMs) {
        return new LargeSlowOutput(delayMs, largeString(sizeBytes));
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
    public Response<LargeSlowResponse> largeSlowAsync(int arg0, int arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<?> largeSlowAsync(int arg0, int arg1, AsyncHandler<LargeSlowResponse> asyncHandler) {
        throw new UnsupportedOperationException();
    }
}
