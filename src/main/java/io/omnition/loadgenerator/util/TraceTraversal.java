package io.omnition.loadgenerator.util;

import java.util.List;
import java.util.function.Consumer;

import io.omnition.loadgenerator.model.trace.Span;
import io.omnition.loadgenerator.model.trace.Trace;

public class TraceTraversal {
    public static void prePostOrder(Trace trace, Consumer<Span> preVisitConsumer, Consumer<Span> postVisitConsumer) {
        prePostOrder(trace, trace.rootSpan, preVisitConsumer, postVisitConsumer);
    }

    private static void prePostOrder(
        Trace trace,
        Span span,
        Consumer<Span> preVisitConsumer,
        Consumer<Span> postVisitConsumer
    ) {
        preVisitConsumer.accept(span);
        List<Span> outgoing = trace.spanIdToChildrenSpans.get(span.id);
        outgoing.forEach(descendent -> prePostOrder(trace, descendent, preVisitConsumer, postVisitConsumer));
        postVisitConsumer.accept(span);
    }
}
