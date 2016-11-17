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
import org.collectd.model.Severity;
import org.collectd.model.ValueType;
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

    private static final int UINT8_LEN = 1;
    private static final int UINT16_LEN = UINT8_LEN * 2;
    private static final int UINT32_LEN = UINT16_LEN * 2;
    private static final int UINT64_LEN = UINT32_LEN * 2;
    private static final int HEADER_LEN = UINT16_LEN * 2;

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

        if (log.isDebugEnabled()) {
            log.debug("Sending UDP packet, buffer length: " + length);
        }
        if (log.isTraceEnabled()) {
            log.trace("Buffer data: " + Arrays.toString(buffer));
        }

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
        final int len = HEADER_LEN + val.length() + 1;
        writeHeader(type, len);
        writeStringValue(val, true);
    }

    private void writeNumberPart(final short type, final long val) throws IOException {
        final int len = HEADER_LEN + UINT64_LEN;
        writeHeader(type, len);
        writeLongOrDateValue(val);
    }

    private void writeKeyParts(final PluginData data) throws IOException {
        writeStringPart(PacketPartType.HOST.getCode(), data.getHost());
        writeNumberPart(PacketPartType.TIME.getCode(), data.getTime() / 1000);
        writeStringPart(PacketPartType.PLUGIN.getCode(), data.getPlugin());
        if (data.getPluginInstance() != null) {
            writeStringPart(PacketPartType.PLUGIN_INSTANCE.getCode(), data.getPluginInstance());
        }
        if (data.getType() != null) {
            writeStringPart(PacketPartType.TYPE.getCode(), data.getType());
        }
        if (data.getTypeInstance() != null) {
            writeStringPart(PacketPartType.TYPE_INSTANCE.getCode(), data.getTypeInstance());
        }
    }

    private void writeValuesPart(final List<Values.ValueHolder> values, final Long interval) throws IOException {
        final int num = values.size();
        final int len = HEADER_LEN + UINT16_LEN + num * (UINT8_LEN + UINT64_LEN);

        final byte[] types = new byte[num];
        for (int i = 0; i < num; i++) {
            final Values.ValueHolder holder = values.get(i);
            final Number value = holder.getValue();

            if (holder.getType() == null) {
                if (value instanceof Double) {
                    types[i] = ValueType.GAUGE.getCode();
                    holder.setType(ValueType.GAUGE);
                } else {
                    types[i] = ValueType.COUNTER.getCode();
                    holder.setType(ValueType.COUNTER);
                }
            } else {
                types[i] = holder.getType().getCode();
            }
        }

        writeHeader(PacketPartType.VALUES.getCode(), len);
        writeShortValue(num);
        os.write(types);

        for (final Values.ValueHolder holder : values) {
            final Number value = holder.getValue();
            final ValueType type = holder.getType();

            switch (type) {
                case COUNTER:
                case ABSOLUTE: {
                    // unsigned, big-endian
                    writeLongOrDateValue(value.longValue());
                    break;
                }
                case GAUGE: {
                    // little-endian
                    writeDoubleValue(value.doubleValue());
                    break;
                }
                case DERIVE: {
                    // signed, big-endian
                    writeLongOrDateValue(value.longValue());
                    break;
                }
                default: {
                    log.warn("Unsupported numeric value type: " + type);
                }
            }
        }

        if (interval != null) {
            writeNumberPart(PacketPartType.INTERVAL.getCode(), interval);
        }
    }

    private void writeNotificationPart(final Severity severity, final String message) throws IOException {
        if (severity != null) {
            writeNumberPart(PacketPartType.SEVERITY.getCode(), severity.getCode());
        }

        writeStringPart(PacketPartType.MESSAGE.getCode(), message);
    }
}
