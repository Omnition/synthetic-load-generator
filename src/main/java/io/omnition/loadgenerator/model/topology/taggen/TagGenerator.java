package io.omnition.loadgenerator.model.topology.taggen;

import io.omnition.loadgenerator.model.trace.KeyValue;

import java.util.Map;

public interface TagGenerator {
    void addTagsTo(Map<String, KeyValue> tags);
}
