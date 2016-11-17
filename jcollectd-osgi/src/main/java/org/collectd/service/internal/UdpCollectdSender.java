package org.collectd.service.internal;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import lombok.extern.slf4j.Slf4j;
import org.collectd.model.Notification;
import org.collectd.model.PluginData;
import org.collectd.model.Values;
import org.collectd.protocol.CollectdConstants;
import org.collectd.protocol.UdpPacketWriter;
import org.collectd.service.CollectdSender;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Collectd UDP sender service.
 */
@Component(immediate = true, property = CollectdSender.PROTOCOL_KEY + "=udp")
@Slf4j
public class UdpCollectdSender implements CollectdSender {

    @SuppressWarnings("checkstyle:JavadocMethod")
    @ObjectClassDefinition(name = "Collectd sender configuration")
    public @interface Config {

        @AttributeDefinition(required = false, name = "Collectd server host name")
        String stats_collectd_host() default CollectdConstants.DEFAULT_IPV4_ADDRESS;

        @AttributeDefinition(required = false, name = "Collectd server port number")
        int stats_collectd_port() default CollectdConstants.DEFAULT_UDP_PORT;

        @AttributeDefinition(required = false, name = "Client hostname")
        String stats_collectd_clientHost();

        @AttributeDefinition(required = false, name = "Packet size")
        int stats_collectd_packetSize() default CollectdConstants.DEFAULT_PACKET_SIZE;
    }

    private UdpPacketWriter writer;
    private String host;

    /**
     * Initialize OSGi component.
     * 
     * @param config configuration options
     */
    @Activate
    @Modified
    public void startOsgiComponent(final Config config) {
        final InetSocketAddress server = new InetSocketAddress(config.stats_collectd_host(), config.stats_collectd_port());
        final int packetSize = config.stats_collectd_packetSize();
        
        host = config.stats_collectd_clientHost();
        writer = new UdpPacketWriter(server, packetSize);
    }

    /**
     * Cleanup OSGi component.
     */
    @Deactivate
    public void stopOsgiComponent() {
        host = null;
        writer = null;
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

    /**
     * Send numeric values (metrics) to collectd.
     * 
     * @param values numeric values
     */
    @Override
    public void send(final Values values) {
        setDefaults(values);
        try {
            writer.write(values);
        } catch (IOException ex) {
            log.error("Unable to send value list", ex);
        }
    }

    /**
     * Send notification to collectd.
     * 
     * @param notification notification
     */
    @Override
    public void send(final Notification notification) {
        setDefaults(notification);
        try {
            writer.write(notification);
        } catch (IOException ex) {
            log.error("Unable to send notifitcation", ex);
        }
    }
}
