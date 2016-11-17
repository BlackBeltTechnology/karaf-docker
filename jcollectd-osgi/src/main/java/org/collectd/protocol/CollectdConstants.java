package org.collectd.protocol;

/**
 * Collectd constants. More details: https://collectd.org/wiki/index.php/Binary_protocol
 */
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
public class CollectdConstants {

    public static final int DEFAULT_UDP_PORT = 25826;
    public static final String DEFAULT_IPV4_ADDRESS = "239.192.74.66";
    public static final String DEFAULT_IPV6_ADDRESS = "ff18::efc0:4a42";

    public static final int DEFAULT_PACKET_SIZE = 1024;
    public static final int MAX_PACKET_SIZE = 1452;
}
