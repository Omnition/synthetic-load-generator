package io.omnition.loadgenerator.model.trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Trace {
    public Span rootSpan;
    public List<Span> spans = new ArrayList<>();
    public Map<UUID, Span> spanIdToSpan = new HashMap<>();
    public Map<UUID, List<Reference>> spanIdToOutgoingRefs = new HashMap<>();

    public void addSpan(Span span) {
        this.spans.add(span);
        this.spanIdToSpan.put(span.id, span);
    }

    public void addRefs() {
        for (Span span : spans) {
            for (Reference ref : span.refs) {
                this.spanIdToOutgoingRefs.computeIfAbsent(ref.fromSpanId, id -> new ArrayList<>())
                .add(ref);
            }
        }
    }
}
