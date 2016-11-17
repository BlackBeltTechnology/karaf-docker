package org.collectd.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Numeric values data type.
 */
@lombok.Getter
@lombok.Setter
@lombok.ToString
public class Values extends PluginData {

    private List<ValueHolder> items = new ArrayList<>();
    private Long interval;

    @lombok.Getter
    @lombok.Setter
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ValueHolder {

        private ValueType type;
        private Number value;
    }
}
