package org.collectd.protocol;

/**
 * Collectd constants. More details: https://collectd.org/wiki/index.php/Binary_protocol
 */
public class CollectdConstants {

    public static final short TYPE_HOST = 0x0000;
    public static final short TYPE_TIME = 0x0001;
    public static final short TYPE_PLUGIN = 0x0002;
    public static final short TYPE_PLUGIN_INSTANCE = 0x0003;
    public static final short TYPE_TYPE = 0x0004;
    public static final short TYPE_TYPE_INSTANCE = 0x0005;
    public static final short TYPE_VALUES = 0x0006;
    public static final short TYPE_INTERVAL = 0x0007;
    public static final short TYPE_TIME_HIGH = 0x0008;
    public static final short TYPE_INTERVAL_HIGH = 0x0009;

    public static final short TYPE_MESSAGE = 0x0100;
    public static final short TYPE_SEVERITY = 0x0101;

    public static final short TYPE_SIGNATURE = 0x0200;
    public static final short TYPE_ENCRYPTION = 0x0201;

    public static final byte DATA_TYPE_COUNTER = 0x00;
    public static final byte DATA_TYPE_GAUGE = 0x01;
    public static final byte DATA_TYPE_DERIVE = 0x02;
    public static final byte DATA_TYPE_ABSOLUTE = 0x03;
    
    public static final int UINT8_LEN = 1;
    public static final int UINT16_LEN = UINT8_LEN * 2;
    public static final int UINT32_LEN = UINT16_LEN * 2;
    public static final int UINT64_LEN = UINT32_LEN * 2;
    public static final int HEADER_LEN = UINT16_LEN * 2;

    public static final int DEFAULT_UDP_PORT = 25826;
    public static final String DEFAULT_IPV4_ADDRESS = "239.192.74.66";
    public static final String DEFAULT_IPV6_ADDRESS = "ff18::efc0:4a42";

    public static final int DEFAULT_PACKET_SIZE = 1024;
    public static final int MAX_PACKET_SIZE = 1452;

    public static final int MAX_PAYLOAD_SIZE = 65531;
}
