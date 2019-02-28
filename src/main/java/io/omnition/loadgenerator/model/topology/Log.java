package io.omnition.loadgenerator.model.topology;

import io.omnition.loadgenerator.model.trace.KeyValue;
import org.slf4j.MDC;

import java.util.List;
import java.util.Map;

public class Log {
    public String msg;
    public String errorMsg;
    public List<String> tagsToInclude;

    public void setContext(String traceId, Map<String, KeyValue> tags) {
        MDC.put("traceId", traceId);

        for (String tagToInclude : tagsToInclude) {
            KeyValue tagVal = tags.get(tagToInclude);
            if (tagVal != null) {
                if (tagVal.valueBool != null) {
                    MDC.put(tagToInclude, tagVal.valueBool.toString());
                } else if (tagVal.valueString != null) {
                    MDC.put(tagToInclude, tagVal.valueString);
                } else if (tagVal.valueLong != null) {
                    MDC.put(tagToInclude, tagVal.valueLong.toString());
                }
            }
        }
    }
}
