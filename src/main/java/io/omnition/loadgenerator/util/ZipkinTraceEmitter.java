package io.omnition.loadgenerator.util;

import brave.Tracing;
import brave.opentracing.BraveTracer;
import brave.propagation.B3Propagation;
import brave.propagation.Propagation;
import brave.sampler.Sampler;
import io.omnition.loadgenerator.model.trace.Service;
import io.omnition.loadgenerator.model.trace.Trace;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapInjectAdapter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.codec.Encoding;
import zipkin2.codec.SpanBytesEncoder;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.okhttp3.OkHttpSender;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ZipkinTraceEmitter implements ITraceEmitter {

    private static final Logger logger = LoggerFactory.getLogger(ZipkinTraceEmitter.class);
    private static final String V2_API = "/api/v2/spans";
    private static final String V1_API = "/api/v1/spans";

    private final Map<String, BraveTracer> serviceNameToTracer = new HashMap<>();

    private String collectorURL;
    private SpanBytesEncoder spanBytesEncoder;

    public ZipkinTraceEmitter(String collectorURL, SpanBytesEncoder spanBytesEncoder) {
        this.collectorURL = collectorURL;
        this.spanBytesEncoder = spanBytesEncoder;
    }

    @Override
    public String emit(Trace trace) {
        final MutableObject<String> traceId = new MutableObject<>(null);
        final Map<UUID, Span> convertedSpans = new HashMap<>();
        Consumer<io.omnition.loadgenerator.model.trace.Span> createOtSpan = span -> {
            boolean extract = StringUtils.isEmpty(traceId.getValue());
            BraveTracer tracer = getTracer(span.service);
            io.opentracing.Span otSpan = OpenTracingTraceConverter.createOTSpan(
                    tracer, span, convertedSpans
            );
            convertedSpans.put(span.id, otSpan);
            if (extract) {
                traceId.setValue(extractTraceID(tracer, otSpan));
            }
        };
        Consumer<io.omnition.loadgenerator.model.trace.Span> closeOtSpan = span -> {
            // mark span as closed
            convertedSpans.get(span.id).finish(span.endTimeMicros);
        };
        TraceTraversal.prePostOrder(trace, createOtSpan, closeOtSpan);
        return traceId.getValue();
    }

    private String extractTraceID(Tracer tracer, Span otSpan) {
        HashMap<String, String> baggage = new HashMap<>();
        TextMapInjectAdapter map = new TextMapInjectAdapter(baggage);
        tracer.inject(otSpan.context(), Format.Builtin.HTTP_HEADERS, map);
        try {
            String encodedTraceId = URLDecoder.decode(baggage.get("X-B3-TraceId"), "UTF-8");
            return encodedTraceId.split(":")[0];
        } catch (UnsupportedEncodingException e) {
            logger.error("Could not get traceID from zipkin", e);
            return null;
        }
    }

    private BraveTracer getTracer(Service service) {
        return this.serviceNameToTracer.computeIfAbsent(service.serviceName,
                s -> createBraveTracer(collectorURL, service));
    }

    private BraveTracer createBraveTracer(String collectorUrl, Service svc) {
        Encoding encoding = Encoding.JSON;
        String queryPath = V2_API;
        switch (this.spanBytesEncoder) {
            case JSON_V1:
                queryPath = V1_API;
                break;
            case THRIFT:
                encoding = Encoding.THRIFT;
                queryPath = V1_API;
                break;
            case JSON_V2: 
                break;
            case PROTO3:
                encoding = Encoding.PROTO3;
                break;
        }

        Sender sender = OkHttpSender.newBuilder().encoding(encoding).endpoint(collectorUrl + queryPath).build();
        Reporter<zipkin2.Span> spanReporter = AsyncReporter.builder(sender).build(this.spanBytesEncoder);
        Propagation.Factory propagationFactory = B3Propagation.FACTORY;
        Tracing.Builder braveTracingB = Tracing.newBuilder()
                .localServiceName(svc.serviceName)
                .propagationFactory(propagationFactory)
                .spanReporter(spanReporter)
                .sampler(Sampler.ALWAYS_SAMPLE);
        Tracing braveTracing = braveTracingB.build();
        return BraveTracer.create(braveTracing);
    }

}
