package io.omnition.loadgenerator.util;

import java.util.List;
import java.util.function.Consumer;

import io.omnition.loadgenerator.model.trace.Reference;
import io.omnition.loadgenerator.model.trace.Span;
import io.omnition.loadgenerator.model.trace.Trace;

public class TraceTraversal {
    public static void prePostOrder(Trace trace, Consumer<Span> preVisitConsumer, Consumer<Span> postVisitConsumer) {
        prePostOrder(trace, trace.rootSpan, preVisitConsumer, postVisitConsumer);
    }

    private static void prePostOrder(Trace trace, Span span, Consumer<Span> preVisitConsumer,
            Consumer<Span> postVisitConsumer) {
        preVisitConsumer.accept(span);
        List<Reference> outgoing = trace.spanIdToOutgoingRefs.get(span.id);
        if (outgoing != null) {
            outgoing.stream()
            .map(ref -> trace.spanIdToSpan.get(ref.toSpanId))
            .forEach(descendant -> prePostOrder(trace, descendant, preVisitConsumer, postVisitConsumer));
        }
        postVisitConsumer.accept(span);
    }
}
