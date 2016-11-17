package org.collectd.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.collectd.model.Values;
import org.collectd.protocol.CollectdConstants;
import org.collectd.service.CollectdSender;
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
    private List<CollectdSender> senders = new ArrayList<>();

    @Override
    public void handleEvent(final Event event) {
        final Values valueList = new Values();

        valueList.setPlugin((String) event.getProperty("plugin"));
        valueList.setPluginInstance((String) event.getProperty("pluginInstance"));
        valueList.setType((String) event.getProperty("type"));
        valueList.setTypeInstance((String) event.getProperty("typeInstance"));

        final Byte valueType = parseValueType((String) event.getProperty("valueType"));
        final String intervalProperty = (String) event.getProperty("interval");
        if (intervalProperty != null) {
            valueList.setInterval(Long.parseLong(intervalProperty));
        }

        final Object value = event.getProperty("value");
        if (value instanceof Number) {
            final Values.ValueHolder valueHolder = new Values.ValueHolder(valueType, (Number) value);
            valueList.getItems().add(valueHolder);
        } else if (value instanceof Collection) {
            for (final Number n : (Collection<Number>) value) {
                final Values.ValueHolder valueHolder = new Values.ValueHolder(valueType, n);
                valueList.getItems().add(valueHolder);
            }
        }

        for (final CollectdSender sender : senders) {
            sender.send(valueList);
        }
    }

    private Byte parseValueType(final String value) {
        if (value == null) {
            return null;
        }

        switch (value) {
            case "COUNTER":
                return CollectdConstants.DATA_TYPE_COUNTER;
            case "GAUGE":
                return CollectdConstants.DATA_TYPE_GAUGE;
            case "DERIVE":
                return CollectdConstants.DATA_TYPE_DERIVE;
            case "ABSOLUTE":
                return CollectdConstants.DATA_TYPE_ABSOLUTE;
            default:
                log.warn("Invalid data type: " + value);
                return null;
        }
    }
}
