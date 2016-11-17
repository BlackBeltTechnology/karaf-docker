package org.collectd.model;

/**
 * Numeric value data types.
 */
public enum ValueType {
    COUNTER((byte) 0x00),
    GAUGE((byte) 0x01),
    DERIVE((byte) 0x02),
    ABSOLUTE((byte) 0x03);

    private final byte code;

    ValueType(final byte code) {
        this.code = code;
    }

    /**
     * Get data type code.
     *
     * @return code for binary protocol
     */
    public byte getCode() {
        return code;
    }
}
