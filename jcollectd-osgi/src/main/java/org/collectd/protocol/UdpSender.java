package org.collectd.protocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import lombok.extern.slf4j.Slf4j;
import org.collectd.api.Notification;
import org.collectd.api.PluginData;
import org.collectd.api.ValueList;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, property = Sender.PROTOCOL_KEY + "=udp")
@Slf4j
public class UdpSender implements Sender {

    @SuppressWarnings("checkstyle:JavadocMethod")
    @ObjectClassDefinition(name = "Collectd sender configuration")
    public @interface Config {

        @AttributeDefinition(required = true, name = "Collectd server host name")
        String stats_collectd_host() default "239.192.74.66";

        @AttributeDefinition(required = true, name = "Collectd server port number")
        int stats_collectd_port() default 25826;
        
        @AttributeDefinition(required = false, name = "Client's hostname")
        String stats_collectd_clientHost();
    }

    private InetSocketAddress server;
    private DatagramSocket socket;
    private MulticastSocket mcast;
    private final PacketWriter writer = new PacketWriter();

    private String host;

    @Activate
    @Modified
    public void startOsgiComponent(final Config config) {
        server = new InetSocketAddress(config.stats_collectd_host(), config.stats_collectd_port());
        host = config.stats_collectd_clientHost();
    }

    @Deactivate
    public void stopOsgiComponent() {
        try {
            flush();
        } catch (IOException ex) {
            log.error("Unable to flush collectd buffer", ex);
        }
        
        server = null;
        host = null;
    }

    private String getHost() {
        if (host == null) {
            try {
                host = InetAddress.getLocalHost().getHostName();
            } catch (IOException ex) {
                log.error("Unable to get host name", ex);
                host = "unknown";
            }
        }
        return host;
    }

    private void setDefaults(final PluginData data) {
        if (data.getHost() == null) {
            data.setHost(getHost());
        }
        if (data.getTime() <= 0) {
            data.setTime(System.currentTimeMillis());
        }
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

    private void write(final PluginData data) throws IOException {
        setDefaults(data);
        final int len = writer.getSize();
        writer.write(data);
        
        if (writer.isFull()) {
            send(writer.getBytes(), len);
            writer.reset();
            writer.write(data);//redo XXX better way?
        }
        
        flush();
    }

    private void send(final byte[] buffer, final int len) throws IOException {
        final DatagramPacket packet = new DatagramPacket(buffer, len, server);
        if (server.getAddress().isMulticastAddress()) {
            getMulticastSocket().send(packet);
        } else {
            getSocket().send(packet);
        }
    }

    private void flush() throws IOException {
        if (writer.getSize() == 0) {
            return;
        }
        final byte[] buffer = writer.getBytes();
        send(buffer, buffer.length);
        writer.reset();
    }

    @Override
    public void send(final ValueList values) {
        try {
            setDefaults(values);
            write(values);
        } catch (IOException ex) {
            log.error("Unable to send value list", ex);
        }
    }

    @Override
    public void send(final Notification notification) {
        try {
            setDefaults(notification);
            write(notification);
        } catch (IOException ex) {
            log.error("Unable to send notifitcation", ex);
        }
    }
}
