package org.collectd.model;

/**
 * Severity data types.
 */
public enum Severity {
    FAILURE(1),
    WARNING(2),
    INFO(4);

    private final int code;

    Severity(final int code) {
        this.code = code;
    }

    /**
     * Get code of severity.
     *
     * @return code for binary protocol
     */
    public int getCode() {
        return code;
    }
}
