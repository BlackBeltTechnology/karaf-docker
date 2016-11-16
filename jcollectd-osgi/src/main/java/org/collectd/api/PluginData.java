package org.collectd.api;

@lombok.Getter
@lombok.Setter
public class PluginData {

    private long time;
    private String host;
    private String plugin;
    private String pluginInstance;
    private String type;
    private String typeInstance;
}
