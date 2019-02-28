package io.omnition.loadgenerator.model.topology;

import io.omnition.loadgenerator.model.trace.KeyValue;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Log {
    public String msg;
    public String errorMsg;
    public List<String> tagsToInclude;

    public Map<String, String> getContext(String traceId, Map<String, KeyValue> tags) {
        Map<String, String> map = new HashMap<>();
        map.put("traceId", traceId);

        for (String tagToInclude : tagsToInclude) {
            KeyValue tagVal = tags.get(tagToInclude);
            if (tagVal != null) {
                if (tagVal.valueBool != null) {
                    map.put(tagToInclude, tagVal.valueBool.toString());
                } else if (tagVal.valueString != null) {
                    map.put(tagToInclude, tagVal.valueString);
                } else if (tagVal.valueLong != null) {
                    map.put(tagToInclude, tagVal.valueLong.toString());
                }
            }
        }

        return map;
    }
}
