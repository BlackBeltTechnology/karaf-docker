package org.collectd.karaf.commands;

import java.util.List;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.collectd.api.ValueList;
import org.collectd.api.ValueType;
import org.collectd.protocol.Sender;

/**
 * Submit a collectd packet.
 */
@Command(scope = "collectd", name = "send", description = "Submit a Collectd packet.")
@Service
public class Send implements Action {

    @Argument(index = 0, name = "value", description = "Value", required = true, multiValued = true)
    private List<Number> values;

    @Option(name = "--plugin", description = "Plugin", required = true, multiValued = false)
    private String plugin;

    @Option(name = "--pluginInstance", description = "Plugin instance", required = false, multiValued = false)
    private String pluginInstance;

    @Option(name = "--type", description = "Type", required = false, multiValued = false)
    private String type;

    @Option(name = "--typeInstance", description = "Type", required = false, multiValued = false)
    private String typeInstance;
    
    @Option(name = "--valueType", description = "Value type", required = false, multiValued = false)
    private ValueType valueType;
    
    @Option(name = "--interval", description = "Interval", required = false, multiValued = false)
    private Long interval;
    
    @Reference
    private Sender sender;

    @Override
    public Object execute() {
        final ValueList valueList = new ValueList();
        
        valueList.setPlugin(plugin);
        valueList.setPluginInstance(pluginInstance);
        valueList.setType(type);
        valueList.setTypeInstance(typeInstance);
        
        for (final Number number : values) {
            final ValueList.ValueHolder valueHolder = new ValueList.ValueHolder();
            valueHolder.setValue(number);
            valueHolder.setType(valueType);
            valueList.getValues().add(valueHolder);
            valueList.setInterval(interval);
        }
        
        sender.send(valueList);
        return null;
    }
}
