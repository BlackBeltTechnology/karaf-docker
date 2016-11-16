package org.collectd.api;

public enum ValueType {
    COUNTER((byte) 0),
    GAUGE((byte) 1),
    DERIVE((byte) 2),
    ABSOLUTE((byte) 3);

    private final byte code;

    ValueType(final byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }
}
