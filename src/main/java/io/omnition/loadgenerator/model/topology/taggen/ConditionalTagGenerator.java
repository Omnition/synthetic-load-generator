package io.omnition.loadgenerator.model.topology.taggen;

import io.omnition.loadgenerator.model.trace.KeyValue;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConditionalTagGenerator implements TagGenerator {

    public String name;
    public String checkTagKey;
    public List<MatchGroup> matchers;
    public Object fallthrough;

    @Override
    public void addTagsTo(Map<String, KeyValue> tags) {
        for (MatchGroup matchGroup : matchers) {
            if (tags.get(checkTagKey) != null && Objects.equals(tags.get(checkTagKey).valueString, matchGroup.key)) {
                tags.put(name, KeyValue.FromObject(name, matchGroup.val));
                return;
            }
        }
        if (fallthrough != null) {
            tags.put(name, KeyValue.FromObject(name, fallthrough));
        }
    }

    public static class MatchGroup {
        public String key;
        public Object val;
    }
}
