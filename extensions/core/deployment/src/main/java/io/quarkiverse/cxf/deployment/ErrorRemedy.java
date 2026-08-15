package io.quarkiverse.cxf.deployment;

/**
 * What to do when an error or an unsupported condition is detected at build time.
 */
public enum ErrorRemedy {
    /**
     * Fail the build
     */
    FAIL,
    /**
     * Log a warning and continue the build
     */
    WARN,
    /**
     * Do not perform the check at all
     */
    IGNORE
}
