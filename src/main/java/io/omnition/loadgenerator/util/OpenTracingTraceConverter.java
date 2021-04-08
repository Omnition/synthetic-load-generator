package io.omnition.loadgenerator.util;

import io.omnition.loadgenerator.model.trace.KeyValue;
import io.omnition.loadgenerator.model.trace.Span;
import io.opentracing.Tracer;

import java.util.Map;
import java.util.UUID;

public final class OpenTracingTraceConverter {

    private OpenTracingTraceConverter() {}

    public static io.opentracing.Span createOTSpan(
        Tracer tracer, Span span, Map<UUID, io.opentracing.Span> parentSpans
    ) {
        Tracer.SpanBuilder otSpanBuilder = tracer.buildSpan(span.operationName)
                .withStartTimestamp(span.startTimeMicros);
        for (KeyValue tag : span.tags) {
            otSpanBuilder = addModelTag(tag, otSpanBuilder);
        }
        final Tracer.SpanBuilder finalSpanBuilder = otSpanBuilder;
        if (span.parentId != null) {
            finalSpanBuilder.addReference(
                io.opentracing.References.CHILD_OF,
                parentSpans.get(span.parentId).context()
            );
        }
        return finalSpanBuilder.start();
    }

    private static Tracer.SpanBuilder addModelTag(KeyValue tag, Tracer.SpanBuilder otSpanBld) {
        if (tag.valueType.equalsIgnoreCase(KeyValue.STRING_VALUE_TYPE)) {
            otSpanBld = otSpanBld.withTag(tag.key, tag.valueString);
        } else if (tag.valueType.equalsIgnoreCase(KeyValue.BOOLEAN_VALUE_TYPE)) {
            otSpanBld = otSpanBld.withTag(tag.key, tag.valueBool);
        } else if (tag.valueType.equalsIgnoreCase(KeyValue.LONG_VALUE_TYPE)) {
            otSpanBld = otSpanBld.withTag(tag.key, tag.valueLong);
        } // other types are ignored for now
        return otSpanBld;
    }
}
