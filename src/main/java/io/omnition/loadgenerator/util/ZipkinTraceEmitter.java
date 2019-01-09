package io.omnition.loadgenerator.util;

import brave.Tracer;
import io.omnition.loadgenerator.model.trace.Service;
import io.omnition.loadgenerator.model.trace.Trace;
import io.opentracing.Span;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import zipkin2.reporter.Sender;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class BraveTraceEmitter implements ITraceEmitter {

    private final Map<String, Tracer> serviceNameToTracer = new HashMap<>();
    private String collectorURL;

    public BraveTraceEmitter(String collectorURL) {
        this.collectorURL = collectorURL;
    }

    @Override
    public String emit(Trace trace) {
        final MutableObject<String> traceId = new MutableObject<>(null);
        final Map<UUID, Span> convertedSpans = new HashMap<>();
        Consumer<io.omnition.loadgenerator.model.trace.Span> createOtSpan = span -> {
            boolean extract = StringUtils.isEmpty(traceId.getValue());
            Tracer tracer = getTracer(span.service);
            io.opentracing.Span otSpan = OpenTracingTraceConverter.createOTSpan(
                    tracer, span, convertedSpans
            );
            convertedSpans.put(span.id, otSpan);
            if (extract) {
                traceId.setValue();
            }
        };
        Consumer<io.omnition.loadgenerator.model.trace.Span> closeOtSpan = span -> {
            // mark span as closed
            convertedSpans.get(span.id).finish(span.endTimeMicros);
        };
        TraceTraversal.prePostOrder(trace, createOtSpan, closeOtSpan);
        return traceId.getValue();
    }

    private Tracer getTracer(Service service) {
        return this.serviceNameToTracer.computeIfAbsent(service.serviceName,
                s -> createBraveTracer(collectorURL, service));
    }

    private static Tracer createBraveTracer(String collectorUrl, Service svc) {
        Sender sender = OkHttpSender.create("http://127.0.0.1:9411/api/v2/spans");
        spanReporter = AsyncReporter.create(sender);

    }


}
