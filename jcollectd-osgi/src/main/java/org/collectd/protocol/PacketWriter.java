package org.collectd.protocol;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.collectd.api.PluginData;
import org.collectd.api.ValueList;
import org.collectd.api.ValueType;

@Slf4j
public class PacketWriter {

    private static final int TYPE_HOST = 0x0000;
    private static final int TYPE_TIME = 0x0001;
    private static final int TYPE_PLUGIN = 0x0002;
    private static final int TYPE_PLUGIN_INSTANCE = 0x0003;
    private static final int TYPE_TYPE = 0x0004;
    private static final int TYPE_TYPE_INSTANCE = 0x0005;
    private static final int TYPE_VALUES = 0x0006;
    private static final int TYPE_INTERVAL = 0x0007;
    private static final int TYPE_TIME_HIGH = 0x0008;
    private static final int TYPE_INTERVAL_HIGH = 0x0009;

    private static final int TYPE_MESSAGE = 0x0100;
    private static final int TYPE_SEVERITY = 0x0101;
    
    private static final int TYPE_SIGNATURE = 0x0200;
    private static final int TYPE_ENCRYPTION = 0x0201;

    private static final int UINT8_LEN = 1;
    private static final int UINT16_LEN = UINT8_LEN * 2;
    private static final int UINT32_LEN = UINT16_LEN * 2;
    private static final int UINT64_LEN = UINT32_LEN * 2;
    private static final int HEADER_LEN = UINT16_LEN * 2;
    
    public static final int BUFFER_SIZE = 1024;

    private final ByteArrayOutputStream bos = new ByteArrayOutputStream(BUFFER_SIZE);
    private final DataOutputStream os = new DataOutputStream(bos);

    public int getSize() {
        return bos.size();
    }

    public byte[] getBytes() {
        return bos.toByteArray();
    }

    public boolean isFull() {
        return getSize() >= BUFFER_SIZE;
    }
    
    public void reset() {
        bos.reset();
    }

    public void write(final PluginData data) throws IOException {
        writeString(TYPE_HOST, data.getHost());
        writeNumber(TYPE_TIME, data.getTime() / 1000);
        writeString(TYPE_PLUGIN, data.getPlugin());
        if (data.getPluginInstance() != null) {
            writeString(TYPE_PLUGIN_INSTANCE, data.getPluginInstance());
        }
        if (data.getType() != null) {
            writeString(TYPE_TYPE, data.getType());
        }
        if (data.getTypeInstance() != null) {
            writeString(TYPE_TYPE_INSTANCE, data.getTypeInstance());
        }

        if (data instanceof ValueList) {
            final ValueList vl = (ValueList) data;
            final List<ValueList.ValueHolder> values = vl.getValues();
            
            if (vl.getInterval() != null) {
                writeNumber(TYPE_INTERVAL, vl.getInterval());
            }
            writeValues(values);
        } else {
            log.error("Notifications are not supported yet");
        }
    }

    private void writeHeader(final int type, final int len) throws IOException {
        os.writeShort(type);
        os.writeShort(len);
    }

    private void writeValues(List<ValueList.ValueHolder> values) throws IOException {
        int num = values.size();
        int len = HEADER_LEN + UINT16_LEN + (num * UINT8_LEN) + (num * UINT64_LEN);

        byte[] types = new byte[num];

        for (int i = 0; i < num; i++) {
            final ValueList.ValueHolder holder = values.get(i);
            final Number value = holder.getValue();
            
            if (holder.getType() == null) {
                if (value instanceof Double) {
                    types[i] = ValueType.GAUGE.getCode();
                } else {
                    types[i] = ValueType.COUNTER.getCode();;
                }
            } else {
                types[i] = holder.getType().getCode();
            }
        }

        writeHeader(TYPE_VALUES, len);
        os.writeShort(num);
        os.write(types);

        for (int i = 0; i < num; i++) {
            final ValueList.ValueHolder holder = values.get(i);
            final Number value = holder.getValue();
            if (ValueType.COUNTER.getCode() == types[i]) {
                os.writeLong(value.longValue());
            } else {
                writeDouble(value.doubleValue());
            }
        }
    }

    private void writeDouble(final double val) throws IOException {
        final ByteBuffer bb = ByteBuffer.wrap(new byte[8]);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putDouble(val);
        os.write(bb.array());
    }

    private void writeString(final int type, final String val) throws IOException {
        if (val == null || val.length() == 0) {
            return;
        }
        final int len = HEADER_LEN + val.length() + 1;
        writeHeader(type, len);
        os.write(val.getBytes());
        os.write('\0');
    }

    private void writeNumber(final int type, final long val) throws IOException {
        final int len = HEADER_LEN + UINT64_LEN;
        writeHeader(type, len);
        os.writeLong(val);
    }
}
