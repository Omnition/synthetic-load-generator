package io.omnition.loadgenerator.util;

import io.omnition.loadgenerator.model.topology.ServiceRoute;
import io.omnition.loadgenerator.model.topology.ServiceTier;
import io.omnition.loadgenerator.model.topology.TagSet;
import io.omnition.loadgenerator.model.topology.Topology;
import io.omnition.loadgenerator.model.topology.taggen.TagGeneratorWrapper;
import io.omnition.loadgenerator.model.trace.KeyValue;
import io.omnition.loadgenerator.model.trace.Reference;
import io.omnition.loadgenerator.model.trace.Reference.RefType;
import io.omnition.loadgenerator.model.trace.Service;
import io.omnition.loadgenerator.model.trace.Span;
import io.omnition.loadgenerator.model.trace.Trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class TraceGenerator {
    private Topology topology;
    private ITraceEmitter emitter;

    public TraceGenerator(ITraceEmitter emitter, Topology topology) {
        this.emitter = emitter;
        this.topology = topology;
    }

    String generate(String rootServiceName, String rootRouteName, long startTimeMicros) {
        ServiceTier rootService = topology.getServiceTier(rootServiceName);
        Trace trace = new Trace();
        trace.rootSpan = createSpanForServiceRouteCall(
            trace, null, rootService, rootRouteName, startTimeMicros
        );
        trace.addRefs();
        String traceID = emitter.emit(trace);
        emitLogsForTrace(traceID, trace);
        return traceID;
    }

    private Span createSpanForServiceRouteCall(
        Trace trace,
        Map<String, KeyValue> parentTags,
        ServiceTier serviceTier,
        String routeName,
        long startTimeMicros
    ) {
        ServiceRoute route = serviceTier.getRoute(routeName);

        // send tags of serviceTier and serviceTier instance
        Service service = new Service(serviceTier.serviceName, new ArrayList<>());
        Span span = new Span();
        span.startTimeMicros = startTimeMicros;
        span.operationName = route.route;
        span.service = service;

        // Setup base tags
        span.setHttpMethodTag("GET");
        span.setHttpUrlTag("http://" + serviceTier.serviceName + routeName);
        // Get additional tags for this route, and update with any inherited tags
        TagSet routeTags = serviceTier.getTagSet(routeName);
        HashMap<String, KeyValue> tagsToSet = new HashMap<>(routeTags.getKeyValueMap());
        if (parentTags != null && routeTags.inherit != null) {
            for (String inheritTagKey : routeTags.inherit) {
                KeyValue value = parentTags.get(inheritTagKey);
                if (value != null) {
                    tagsToSet.put(inheritTagKey, value);
                }
            }
        }
        for (TagGeneratorWrapper tagGenerator : routeTags.tagGenerators) {
            tagGenerator.addTagsTo(tagsToSet);
        }
        if (tagsToSet.get(SpanConventions.HTTP_STATUS_CODE_KEY) == null) {
            tagsToSet.put(
                SpanConventions.HTTP_STATUS_CODE_KEY,
                KeyValue.ofLongType(SpanConventions.HTTP_STATUS_CODE_KEY, 200L)
            );
        }

        // Set the additional tags on the span
        List<KeyValue> spanTags = tagsToSet.values().stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        span.tags.addAll(spanTags);

        final AtomicLong maxEndTime = new AtomicLong(startTimeMicros);
        if (span.isErrorSpan()) {
            // inject root cause error and terminate trace there
            span.markRootCauseError();
        } else {
            // no error, make downstream calls
            route.downstreamCalls.forEach((s, r) -> {
                long childStartTimeMicros = startTimeMicros + TimeUnit.MILLISECONDS.toMicros(routeTags.randomLatency());
                ServiceTier childSvc = this.topology.getServiceTier(s);
                Span childSpan = createSpanForServiceRouteCall(trace, tagsToSet, childSvc, r, childStartTimeMicros);
                Reference ref = new Reference(RefType.CHILD_OF, span.id, childSpan.id);
                childSpan.refs.add(ref);
                maxEndTime.set(Math.max(maxEndTime.get(), childSpan.endTimeMicros));
                if (childSpan.isErrorSpan()) {
                    Integer httpCode = childSpan.getHttpCode();
                    if (httpCode != null) {
                        span.setHttpCode(httpCode);
                    }
                    span.markError();
                }
            });
        }
        long ownDuration = TimeUnit.MILLISECONDS.toMicros(routeTags.randomLatency());
        span.endTimeMicros = maxEndTime.get() + ownDuration;
        trace.addSpan(span);
        return span;
    }

    private void emitLogsForTrace(String traceId, Trace trace) {
        for (Span span : trace.spans) {
            Map<String, KeyValue> kvMap = span.tags.stream().collect(Collectors.toMap(kv -> kv.key, kv -> kv, (kv1, kv2) -> kv1));
            topology.getServiceTier(span.service.serviceName).logMessages(traceId, kvMap, span.isErrorSpan());
        }
    }

}
