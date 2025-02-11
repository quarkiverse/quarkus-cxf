package io.quarkiverse.cxf.mutiny;

import java.util.Map;

/**
 * A combination of an {@link Exception} (via superclass) and of a response context map via {@link #getContext()}.
 *
 * @since 3.19.0
 */
public class FailedResponse extends Exception {

    private static final long serialVersionUID = 1L;
    private final Map<String, Object> context;

    FailedResponse(Throwable cause, Map<String, Object> context) {
        super(cause);
        this.context = context;
    }

    public Map<String, Object> getContext() {
        return context;
    }

}
