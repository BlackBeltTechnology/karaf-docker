package org.collectd.protocol;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.collectd.model.Notification;
import org.collectd.model.PluginData;
import org.collectd.model.Values;

/**
 * UDP packet writer for Collectd.
 */
@Slf4j
public class UdpPacketWriter {

    private final ByteArrayOutputStream bos;
    private final DataOutputStream os;

    private final InetSocketAddress server;

    private DatagramSocket socket;
    private MulticastSocket mcast;

    /**
     * Create new UDP packet writer instance. Default packet size is used.
     *
     * @param server collectd server address
     */
    public UdpPacketWriter(final InetSocketAddress server) {
        this(server, CollectdConstants.DEFAULT_PACKET_SIZE);
    }

    /**
     * Create new UDP packet writer instance.
     *
     * @param server collectd server address
     * @param packetSize packet size
     */
    public UdpPacketWriter(final InetSocketAddress server, final int packetSize) {
        this.server = server;
        bos = new ByteArrayOutputStream(packetSize);
        os = new DataOutputStream(bos);
    }

    /**
     * Write value list.
     *
     * @param valueList numeric value list
     * @throws IOException unable to write value to output stream
     */
    public void write(final Values valueList) throws IOException {
        writeKeyParts(valueList);

        writeValuesPart(valueList.getItems(), valueList.getInterval());

        // TODO - write signature and encrypted parts
        flush();
    }

    /**
     * Write notification.
     *
     * @param notification notification
     * @throws IOException unable to write value to output stream
     */
    public void write(final Notification notification) throws IOException {
        writeKeyParts(notification);

        writeNotificationPart(notification.getSeverity(), notification.getMessage());

        // TODO - write signature and encrypted parts
        flush();
    }

    private DatagramSocket getSocket() throws SocketException {
        if (socket == null) {
            socket = new DatagramSocket();
        }
        return socket;
    }

    private MulticastSocket getMulticastSocket() throws IOException {
        if (mcast == null) {
            mcast = new MulticastSocket();
            mcast.setTimeToLive(1);
        }
        return mcast;
    }

    private void flush() throws IOException {
        os.flush();
        
        final byte[] buffer;
        synchronized (this) {
            buffer = bos.toByteArray();
            bos.reset();
        }

        final int length = buffer.length;
        if (length == 0) {
            return;
        }
        
        log.info("Sending UDP packet, buffer length: " + length);
        log.info("Buffer data: " + Arrays.toString(buffer));

        final DatagramPacket packet = new DatagramPacket(buffer, length, server);
        if (server.getAddress().isMulticastAddress()) {
            getMulticastSocket().send(packet);
        } else {
            getSocket().send(packet);
        }
    }

    private void writeHeader(final short type, final int len) throws IOException {
        writeShortValue(type);
        writeShortValue(len);
    }

    private void writeShortValue(final int val) throws IOException {
        os.writeShort(val);
    }

    /**
     * Write long or date (epoch) value.
     *
     * @param val long or epoch value
     * @throws IOException unable to write value to output stream
     */
    private void writeLongOrDateValue(final long val) throws IOException {
        os.writeLong(val);
    }

    private void writeDoubleValue(final double val) throws IOException {
        final ByteBuffer bb = ByteBuffer.wrap(new byte[8]);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putDouble(val);
        os.write(bb.array());
    }

    private void writeStringValue(final String val, final boolean addNullByte) throws IOException {
        os.write(val.getBytes("UTF-8"));
        if (addNullByte) {
            os.write('\0');
        }
    }

    private void writeStringPart(final short type, final String val) throws IOException {
        if (val == null || val.length() == 0) {
            return;
        }
        final int len = CollectdConstants.HEADER_LEN + val.length() + 1;
        writeHeader(type, len);
        writeStringValue(val, true);
    }

    private void writeNumberPart(final short type, final long val) throws IOException {
        final int len = CollectdConstants.HEADER_LEN + CollectdConstants.UINT64_LEN;
        writeHeader(type, len);
        writeLongOrDateValue(val);
    }

    private void writeKeyParts(final PluginData data) throws IOException {
        writeStringPart(CollectdConstants.TYPE_HOST, data.getHost());
        writeNumberPart(CollectdConstants.TYPE_TIME, data.getTime() / 1000);
        writeStringPart(CollectdConstants.TYPE_PLUGIN, data.getPlugin());
        if (data.getPluginInstance() != null) {
            writeStringPart(CollectdConstants.TYPE_PLUGIN_INSTANCE, data.getPluginInstance());
        }
        if (data.getType() != null) {
            writeStringPart(CollectdConstants.TYPE_TYPE, data.getType());
        }
        if (data.getTypeInstance() != null) {
            writeStringPart(CollectdConstants.TYPE_TYPE_INSTANCE, data.getTypeInstance());
        }
    }

    private void writeValuesPart(final List<Values.ValueHolder> values, final Long interval) throws IOException {
        final int num = values.size();
        final int len = CollectdConstants.HEADER_LEN + CollectdConstants.UINT16_LEN + num * (CollectdConstants.UINT8_LEN + CollectdConstants.UINT64_LEN);

        final byte[] types = new byte[num];
        for (int i = 0; i < num; i++) {
            final Values.ValueHolder holder = values.get(i);
            final Number value = holder.getValue();

            if (holder.getType() == null) {
                if (value instanceof Double) {
                    types[i] = CollectdConstants.DATA_TYPE_GAUGE;
                } else {
                    types[i] = CollectdConstants.DATA_TYPE_COUNTER;
                }
            } else {
                types[i] = holder.getType();
            }
        }

        writeHeader(CollectdConstants.TYPE_VALUES, len);
        writeShortValue(num);
        os.write(types);

        for (int i = 0; i < num; i++) {
            final Values.ValueHolder holder = values.get(i);
            final Number value = holder.getValue();

            switch (types[i]) {
                case CollectdConstants.DATA_TYPE_COUNTER:
                case CollectdConstants.DATA_TYPE_ABSOLUTE: {
                    // unsigned, big-endian
                    writeLongOrDateValue(value.longValue());
                    break;
                }
                case CollectdConstants.DATA_TYPE_GAUGE: {
                    // little-endian
                    writeDoubleValue(value.doubleValue());
                    break;
                }
                case CollectdConstants.DATA_TYPE_DERIVE: {
                    // signed, big-endian
                    writeLongOrDateValue(value.longValue());
                    break;
                }
                default: {
                    log.warn("Unsupported data type: " + types[i]);
                }
            }
        }

        if (interval != null) {
            writeNumberPart(CollectdConstants.TYPE_INTERVAL, interval);
        }
    }

    private void writeNotificationPart(final int severity, final String message) throws IOException {
        writeNumberPart(CollectdConstants.TYPE_SEVERITY, severity);
        writeStringPart(CollectdConstants.TYPE_MESSAGE, message);
    }
}
