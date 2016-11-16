package org.collectd.protocol;

import org.collectd.api.ValueList;
import org.collectd.api.Notification;

public interface Sender {
    
    public static final String PROTOCOL_KEY = "protocol";
    
    void send(ValueList values);

    void send(Notification notification);
}
