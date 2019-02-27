package io.omnition.loadgenerator.model.topology.taggen;

import io.omnition.loadgenerator.model.trace.KeyValue;

import java.util.Map;

public class TagGeneratorWrapper implements TagGenerator {

    public MultiTagGenerator multi;
    public OverTimeTagGenerator overTime;
    public ConditionalTagGenerator conditional;

    @Override
    public void addTagsTo(Map<String, KeyValue> tags) {
        if (multi != null) {
            multi.addTagsTo(tags);
        } else if (overTime != null) {
            overTime.addTagsTo(tags);
        } else if (conditional != null) {
            conditional.addTagsTo(tags);
        }
    }

}
