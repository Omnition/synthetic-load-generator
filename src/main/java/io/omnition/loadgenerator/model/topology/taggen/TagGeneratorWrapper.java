package io.omnition.loadgenerator.model.topology.taggen;

import java.util.Collections;
import java.util.Map;

public class TagGeneratorWrapper implements TagGenerator {

    public MultiTagGenerator multi;
    public OverTimeTagGenerator overTime;

    @Override
    public Map<String, Object> generateTags() {
        if (multi != null) {
            return multi.generateTags();
        } else if (overTime != null) {
            return overTime.generateTags();
        }

        return Collections.emptyMap();
    }
}
