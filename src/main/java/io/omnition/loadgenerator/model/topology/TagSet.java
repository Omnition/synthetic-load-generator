package io.omnition.loadgenerator.model.topology;

import io.omnition.loadgenerator.model.topology.taggen.TagGeneratorWrapper;
import io.omnition.loadgenerator.model.trace.KeyValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class TagSet {
    private static Random random = new Random();

    private Integer weight;

    public Map<String, Object> tags = new HashMap<>();
    public List<TagGeneratorWrapper> tagGenerators = new ArrayList<>();
    public List<String> inherit = new ArrayList<>();
    public Integer maxLatency;
    public Integer minLatency;

    public void setWeight(int weight) {
        if (weight <= 0) {
            throw new IllegalArgumentException("Weight must be greater than 0");
        }
        this.weight = weight;
    }

    public int getWeight() {
        if (weight == null) {
            return 1;
        }
        return weight;
    }

    public Map<String, KeyValue> getKeyValueMap() {
        return tags.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> KeyValue.FromObject(e.getKey(), e.getValue())));
    }

    public int randomLatency() {
        if (maxLatency == null) {
            throw new IllegalArgumentException("No maxLatency set");
        }
        int min = 0;
        if (minLatency != null) {
            min = minLatency;
        }
        if (maxLatency < min) {
            throw new IllegalArgumentException("maxLatency must be greater than minLatency");
        }
        return random.nextInt(maxLatency - min) + min;
    }
}
