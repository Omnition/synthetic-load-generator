package io.omnition.loadgenerator.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.log4j.Logger;

import io.jaegertracing.Tracer;
import io.jaegertracing.Tracer.Builder;
import io.jaegertracing.exceptions.SenderException;
import io.jaegertracing.reporters.RemoteReporter;
import io.jaegertracing.samplers.ConstSampler;
import io.jaegertracing.senders.HttpSender;
import io.omnition.loadgenerator.model.trace.KeyValue;
import io.omnition.loadgenerator.model.trace.Service;
import io.omnition.loadgenerator.model.trace.Span;
import io.omnition.loadgenerator.model.trace.Trace;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapInjectAdapter;

public class JaegerTraceEmitter implements ITraceEmitter {
    private final static Logger logger = Logger.getLogger(JaegerTraceEmitter.class);
    private final Map<String, TracerWrapper> serviceNameToTracer = new HashMap<>();
    private final String collectorUrl;
    private final int flushIntervalMillis;

    public JaegerTraceEmitter(String collectorUrl, int flushIntervalMillis) {
        this.collectorUrl = collectorUrl;
        this.flushIntervalMillis = flushIntervalMillis;
    }

    public void close() {
        serviceNameToTracer.forEach((name, tracer) -> {
            try {
                tracer.sender.flush();
                tracer.tracer.close();
            } catch (SenderException e) {
                logger.warn(String.format("Error when flushing trace for service %s", name), e);
                throw new IllegalArgumentException(e);
            }
        });
    }

    public String emit(Trace trace) {
        final MutableObject<String> traceId = new MutableObject<String>(null);
        final Map<UUID, io.opentracing.Span> spanIdToOtSpan = new HashMap<>();
        Consumer<Span> createOtSpan = span -> {
            boolean extract = StringUtils.isEmpty(traceId.getValue());
            String tid = this.createOtSpanForModelSpan(span, spanIdToOtSpan, extract);
            if (extract) {
                traceId.setValue(tid);
            }
        };
        Consumer<Span> closeOtSpan = span -> {
            // mark span as closed
            spanIdToOtSpan.get(span.id).finish(span.endTimeMicros);
        };
        TraceTraversal.prePostOrder(trace, createOtSpan, closeOtSpan);
        return traceId.getValue();
    }

    private String createOtSpanForModelSpan(
            Span span, Map<UUID, io.opentracing.Span> spanIdToOtSpan, boolean extractTraceId) {
        Tracer tracer = getTracer(span.service).tracer;
        SpanBuilder otSpanBld = tracer.buildSpan(span.operationName)
                .withStartTimestamp(span.startTimeMicros);
        for (KeyValue tag : span.tags) {
            this.addModelTag(tag, otSpanBld);
        }
        span.refs.forEach(ref -> {
            switch (ref.refType) {
            case CHILD_OF:
                otSpanBld.addReference(io.opentracing.References.CHILD_OF,
                        spanIdToOtSpan.get(ref.fromSpanId).context());
                break;
            case FOLLOWS_FROM:
                otSpanBld.addReference(io.opentracing.References.FOLLOWS_FROM,
                        spanIdToOtSpan.get(ref.fromSpanId).context());
                break;
            default:
                break;
            }
        });
        io.opentracing.Span otSpan = otSpanBld.start();
        spanIdToOtSpan.put(span.id, otSpan);

        if (extractTraceId) {
            return this.extractTraceId(tracer, otSpan);
        } else {
            return null;
        }
    }

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

    private SpanBuilder addModelTag(KeyValue tag, SpanBuilder otSpanBld) {
        if (tag.valueType.equalsIgnoreCase(KeyValue.STRING_VALUE_TYPE)) {
            otSpanBld = otSpanBld.withTag(tag.key, tag.valueString);
        } else if (tag.valueType.equalsIgnoreCase(KeyValue.BOOLEAN_VALUE_TYPE)) {
            otSpanBld = otSpanBld.withTag(tag.key, tag.valueBool);
        } else if (tag.valueType.equalsIgnoreCase(KeyValue.LONG_VALUE_TYPE)) {
            otSpanBld = otSpanBld.withTag(tag.key, tag.valueLong);
        } // other types are ignored for now
        return otSpanBld;
    }

    private TracerWrapper getTracer(Service service) {
        return this.serviceNameToTracer.computeIfAbsent(service.serviceName,
                s -> new TracerWrapper(this.collectorUrl, service, this.flushIntervalMillis));
    }

    private static final class TracerWrapper {
        public HttpSender sender;
        public RemoteReporter reporter;
        public Tracer tracer;

        public TracerWrapper(String collectorUrl, Service svc, int flushIntervalMillis) {
            sender = new HttpSender.Builder(collectorUrl + "/api/traces").build();
            reporter = new RemoteReporter.Builder().withSender(sender)
                    .withMaxQueueSize(100000)
                    .withFlushInterval(flushIntervalMillis)
                    .build();
            Builder bld = new Builder(svc.serviceName).withReporter(reporter)
                    .withSampler(new ConstSampler(true));
            for (KeyValue kv : svc.tags) {
                bld.withTag(kv.key, kv.valueString);
            }
            tracer = bld.build();
        }
    }
}
