package io.quarkiverse.cxf.it;

import jakarta.annotation.Priority;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(10000)
//the more generic the mapper, the lower its prio should be(the lower the number the higher the pri), so you can override it with more specific mappers
public class VerboseExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception e) {
        return Response
                .serverError()
                .entity(rootCause(e).getMessage())
                .build();
    }

    private static Throwable rootCause(Throwable e) {
        e.printStackTrace();
        Throwable result = e;
        while (result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }

}