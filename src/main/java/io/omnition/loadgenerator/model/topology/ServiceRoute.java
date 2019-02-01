package io.omnition.loadgenerator.model.topology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceRoute {
    public String route;
    public Map<String, String> downstreamCalls = new HashMap<>();
    public List<TagSet> tagSets = new ArrayList<>();
    public int maxLatencyMillis;
}
