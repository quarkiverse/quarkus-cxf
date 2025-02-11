package io.quarkiverse.cxf.mutiny;

import java.util.Map;

/**
 * A combination of a response object via {@link #payload} and of a response context map via {@link #getContext()}.
 *
 * @param <T>
 * @since 3.19.0
 */
public class SucceededResponse<T> {
    private final Map<String, Object> context;
    private final T payload;

    SucceededResponse(T payload, Map<String, Object> context) {
        super();
        this.payload = payload;
        this.context = context;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public T getPayload() {
        return payload;
    }

}
