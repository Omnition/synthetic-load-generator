package io.omnition.loadgenerator.util;

import brave.Tracing;
import brave.opentracing.BraveTracer;
import brave.propagation.B3Propagation;
import brave.propagation.ExtraFieldPropagation;
import brave.propagation.Propagation;
import io.omnition.loadgenerator.model.trace.Service;
import io.omnition.loadgenerator.model.trace.Trace;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapInjectAdapter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.log4j.Logger;
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

    private static final Logger logger = Logger.getLogger(JaegerTraceEmitter.class);
    private static final String V2_API = "/api/v2/spans";
    private static final String V1_API = "/api/v1/spans";

    private final Map<String, BraveTracer> serviceNameToTracer = new HashMap<>();

    private String collectorURL;
    private boolean useZipkinV2;

    public ZipkinTraceEmitter(String collectorURL, boolean useZipkinV2) {
        this.collectorURL = collectorURL;
        this.useZipkinV2 = useZipkinV2;
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
            String encodedTraceId = URLDecoder.decode(baggage.get("uber-trace-id"), "UTF-8");
            return encodedTraceId.split(":")[0];
        } catch (UnsupportedEncodingException e) {
            logger.error(e);
            return null;
        }
    }

    private BraveTracer getTracer(Service service) {
        return this.serviceNameToTracer.computeIfAbsent(service.serviceName,
                s -> createBraveTracer(collectorURL, service));
    }

    private BraveTracer createBraveTracer(String collectorUrl, Service svc) {
        String queryPath = V2_API;
        if (!useZipkinV2) {
            queryPath = V1_API;
        }
        Sender sender = OkHttpSender.create(collectorUrl + queryPath);
        Reporter<zipkin2.Span> spanReporter = AsyncReporter.create(sender);
        Propagation.Factory propagationFactory = ExtraFieldPropagation.newFactoryBuilder(B3Propagation.FACTORY)
                .build();
        Tracing braveTracing = Tracing.newBuilder()
                .localServiceName(svc.serviceName)
                .propagationFactory(propagationFactory)
                .spanReporter(spanReporter)
                .build();
        return BraveTracer.create(braveTracing);
    }

}
