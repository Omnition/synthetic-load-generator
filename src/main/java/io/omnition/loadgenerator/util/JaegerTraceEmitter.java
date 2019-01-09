package io.omnition.loadgenerator.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import io.jaegertracing.reporters.Reporter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.log4j.Logger;

import io.jaegertracing.Tracer;
import io.jaegertracing.Tracer.Builder;
import io.jaegertracing.reporters.RemoteReporter;
import io.jaegertracing.samplers.ConstSampler;
import io.jaegertracing.senders.HttpSender;
import io.omnition.loadgenerator.model.trace.KeyValue;
import io.omnition.loadgenerator.model.trace.Service;
import io.omnition.loadgenerator.model.trace.Span;
import io.omnition.loadgenerator.model.trace.Trace;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapInjectAdapter;

public class JaegerTraceEmitter implements ITraceEmitter {

    private final static Logger logger = Logger.getLogger(JaegerTraceEmitter.class);

    private final Map<String, Tracer> serviceNameToTracer = new HashMap<>();
    private final String collectorUrl;
    private final int flushIntervalMillis;

    public JaegerTraceEmitter(String collectorUrl, int flushIntervalMillis) {
        this.collectorUrl = collectorUrl;
        this.flushIntervalMillis = flushIntervalMillis;
    }

    public void close() {
        serviceNameToTracer.forEach((name, tracer) -> tracer.close());
    }

    public String emit(Trace trace) {
        final MutableObject<String> traceId = new MutableObject<>(null);
        final Map<UUID, io.opentracing.Span> convertedSpans = new HashMap<>();
        Consumer<Span> createOtSpan = span -> {
            boolean extract = StringUtils.isEmpty(traceId.getValue());
            Tracer tracer = getTracer(span.service);
            io.opentracing.Span otSpan = OpenTracingTraceConverter.createOTSpan(
                tracer, span, convertedSpans
            );
            convertedSpans.put(span.id, otSpan);
            if (extract) {
                traceId.setValue(extractTraceId(tracer, otSpan));
            }
        };
        Consumer<Span> closeOtSpan = span -> {
            // mark span as closed
            convertedSpans.get(span.id).finish(span.endTimeMicros);
        };
        TraceTraversal.prePostOrder(trace, createOtSpan, closeOtSpan);
        return traceId.getValue();
    }

    /**
     * This extracts the jaeger-header traceID from an opentracing span.
     *
     * @param tracer tracer to use to extract the jaeger header trace id
     * @param otSpan span from which to extract the traceID
     * @return string traceID or null if could not decode
     */
    private String extractTraceId(Tracer tracer, io.opentracing.Span otSpan) {
        HashMap<String, String> baggage = new HashMap<>();
        TextMapInjectAdapter map = new TextMapInjectAdapter(baggage);
        tracer.inject(otSpan.context(), Format.Builtin.HTTP_HEADERS, map);
        try {
            String encodedTraceId = URLDecoder.decode(baggage.get("uber-trace-id"), "UTF-8");
            return encodedTraceId.split(":")[0];
        } catch (UnsupportedEncodingException e) {
            logger.error(e);
            return null;
        }
    }

    private Tracer getTracer(Service service) {
        return this.serviceNameToTracer.computeIfAbsent(service.serviceName,
                s -> createJaegerTracer(collectorUrl, service, flushIntervalMillis));
    }

    private static Tracer createJaegerTracer(String collectorUrl, Service svc, int flushIntervalMillis) {
        HttpSender sender = new HttpSender.Builder(collectorUrl + "/api/traces").build();
        Reporter reporter = new RemoteReporter.Builder().withSender(sender)
                .withMaxQueueSize(100000)
                .withFlushInterval(flushIntervalMillis)
                .build();
        Builder bld = new Builder(svc.serviceName).withReporter(reporter)
                .withSampler(new ConstSampler(true));
        for (KeyValue kv : svc.tags) {
            bld.withTag(kv.key, kv.valueString);
        }
        return bld.build();
    }

}
