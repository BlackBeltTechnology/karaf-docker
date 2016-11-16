package org.collectd.api;

import java.util.ArrayList;
import java.util.List;

@lombok.Getter
@lombok.Setter
@lombok.ToString
public class ValueList extends PluginData {

    private List<ValueHolder> values = new ArrayList<>();
    private Long interval;

    @lombok.Getter
    @lombok.Setter
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ValueHolder {

        private Number value;
        private ValueType type;
    }
}
