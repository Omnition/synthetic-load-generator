package io.omnition.loadgenerator.model.topology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ServiceTier {
    public String serviceName;
    public List<TagSet> tagSets = new ArrayList<>();
    public List<ServiceRoute> routes = new ArrayList<>();
    public List<String> instances = new ArrayList<>();

    private ConcurrentMap<String, TreeMap<Integer, TagSet>> mergedTagSets = new ConcurrentHashMap<>();
    private Random random = new Random();

    public ServiceRoute getRoute(String routeName) {
        return this.routes.stream()
                .filter(r -> r.route.equalsIgnoreCase(routeName))
                .findFirst().get();
    }

    public TagSet getTagSet(String routeName) {
        mergedTagSets.computeIfAbsent(routeName, this::generateMergedTagSets);
        TreeMap<Integer, TagSet> routeSets = mergedTagSets.get(routeName);
        return routeSets.higherEntry(random.nextInt(routeSets.lastKey())).getValue();
    }

    private TreeMap<Integer, TagSet> generateMergedTagSets(String routeName) {
        TreeMap<Integer, TagSet> treeMap = new TreeMap<>();
        int total = 0;
        ServiceRoute route = routes.stream().filter((r) -> r.route.equals(routeName)).findFirst().get();

        // If we have to merge, merge, otherwise just set the service set.
        if (route.tagSets != null && route.tagSets.size() > 0) {
            for (TagSet routeSet : route.tagSets) {
                if (tagSets.isEmpty()) {
                    total += routeSet.getWeight();
                    treeMap.put(total, routeSet);
                } else {
                    for (TagSet serviceSet : tagSets) {
                        TagSet mergedSet = new TagSet();
                        mergedSet.tags = new HashMap<>(serviceSet.tags);
                        mergedSet.inherit = new ArrayList<>(serviceSet.inherit);
                        mergedSet.tagGenerators = new ArrayList<>(serviceSet.tagGenerators);

                        mergedSet.tags.putAll(routeSet.tags);
                        mergedSet.inherit.addAll(routeSet.inherit);
                        mergedSet.tagGenerators.addAll(routeSet.tagGenerators);
                        mergedSet.setWeight(routeSet.getWeight() * serviceSet.getWeight());
                        total += mergedSet.getWeight();
                        treeMap.put(total, mergedSet);
                    }
                }
            }
        } else {
            for (TagSet serviceSet : tagSets) {
                total += serviceSet.getWeight();
                treeMap.put(total, serviceSet);
            }
        }
        return treeMap;
    }
}
