package org.collectd.api;

@lombok.Getter
@lombok.Setter
@lombok.ToString
public class Notification extends PluginData {

    public static final int FAILURE = 1;
    public static final int WARNING = 2;
    public static final int OKAY = 4;

    private int severity;
    private String message;
}
