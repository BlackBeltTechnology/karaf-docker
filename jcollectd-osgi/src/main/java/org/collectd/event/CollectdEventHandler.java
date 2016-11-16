package org.collectd.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.collectd.api.ValueList;
import org.collectd.api.ValueType;
import org.collectd.protocol.Sender;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

@Component(immediate = true, property = {
    EventConstants.EVENT_TOPIC + "=" + "org/collectd/Event/SEND",
    EventConstants.EVENT_DELIVERY + "=" + EventConstants.DELIVERY_ASYNC_UNORDERED})
@Slf4j
public class CollectdEventHandler implements EventHandler {

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY)
    private List<Sender> senders = new ArrayList<>();

    @Override
    public void handleEvent(final Event event) {
        final ValueList valueList = new ValueList();

        valueList.setPlugin((String) event.getProperty("plugin"));
        valueList.setPluginInstance((String) event.getProperty("pluginInstance"));
        valueList.setType((String) event.getProperty("type"));
        valueList.setTypeInstance((String) event.getProperty("typeInstance"));

        final String valueTypeProperty = (String) event.getProperty("valueType");
        final ValueType valueType = valueTypeProperty != null ? ValueType.valueOf(valueTypeProperty) : null;

        final String intervalProperty = (String) event.getProperty("interval");
        if (intervalProperty != null) {
            valueList.setInterval(Long.parseLong(intervalProperty));
        }

        final Object value = event.getProperty("value");
        if (value instanceof Number) {
            final ValueList.ValueHolder valueHolder = new ValueList.ValueHolder((Number) value, valueType);
            valueList.getValues().add(valueHolder);
        } else if (value instanceof Collection) {
            for (final Number n : (Collection<Number>) value) {
                final ValueList.ValueHolder valueHolder = new ValueList.ValueHolder(n, valueType);
                valueList.getValues().add(valueHolder);
            }
        }

        for (final Sender sender : senders) {
            sender.send(valueList);
        }
    }
}
