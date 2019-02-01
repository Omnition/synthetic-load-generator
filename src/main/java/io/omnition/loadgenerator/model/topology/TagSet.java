package io.omnition.loadgenerator.model.topology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TagSet {
    private Integer weight;
    public Map<String, Object> tags = new HashMap<>();
    public List<String> inherit = new ArrayList<>();

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
}
