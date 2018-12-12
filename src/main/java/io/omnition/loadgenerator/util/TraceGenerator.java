package io.omnition.loadgenerator.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import io.omnition.loadgenerator.model.topology.OperationModel;
import io.omnition.loadgenerator.model.topology.ServiceRoute;
import io.omnition.loadgenerator.model.topology.ServiceTier;
import io.omnition.loadgenerator.model.topology.Topology;
import io.omnition.loadgenerator.model.trace.KeyValue;
import io.omnition.loadgenerator.model.trace.Reference;
import io.omnition.loadgenerator.model.trace.Reference.RefType;
import io.omnition.loadgenerator.model.trace.Service;
import io.omnition.loadgenerator.model.trace.Span;
import io.omnition.loadgenerator.model.trace.Trace;

public class TraceGenerator {
    private final Random random = new Random();
    private final Trace trace = new Trace();
    private Topology topology;

    public static Trace generate(Topology topology, String rootServiceName, String rootRouteName, long startTimeMicros) {
        TraceGenerator gen = new TraceGenerator(topology);
        ServiceTier rootServiceTier = gen.topology.getServiceTier(rootServiceName);
        Span rootSpan = gen.createSpanForServiceRouteCall(rootServiceTier, rootRouteName, startTimeMicros);
        gen.trace.rootSpan = rootSpan;
        gen.trace.addRefs();
        return gen.trace;
    }

    private TraceGenerator(Topology topology) {
        this.topology = topology;
    }

    private Span createSpanForServiceRouteCall(ServiceTier serviceTier, String routeName, long startTimeMicros) {
        String instanceName = serviceTier.instances.get(
                random.nextInt(serviceTier.instances.size()));
        ServiceRoute route = serviceTier.getRoute(routeName);
        OperationModel om = serviceTier.getOperationModel(instanceName, routeName);

        // send tags of service and service instance
        Service service = new Service(serviceTier.serviceName, instanceName, new ArrayList<>());
        Span span = new Span();
        span.startTimeMicros = startTimeMicros;
        span.operationName = route.route;
        span.service = service;
        span.setHttpMethod("GET");
        span.setHttpUrl(String.format("http://" + serviceTier.serviceName + routeName));
        List<KeyValue> tags = serviceTier.tags.entrySet().stream()
                .map(t -> KeyValue.ofStringType(t.getKey(), t.getValue())).collect(Collectors.toList());
        span.tags.addAll(tags);

        final AtomicLong maxEndTime = new AtomicLong(startTimeMicros);
        if (random.nextInt(100) < om.errorPercent) {
            // inject error and terminate trace there
            span.markError();
            span.markRootCauseError();
            span.setHttpCode(om.errorCode);
        } else {
            // no error, make downstream calls
            route.downstreamCalls.forEach((s, r) -> {
                long childStartTimeMicros = startTimeMicros + TimeUnit.MILLISECONDS.toMicros(random.nextInt(om.maxLatencyMillis));
                ServiceTier childSvc = this.topology.getServiceTier(s);
                Span childSpan = createSpanForServiceRouteCall(childSvc, r, childStartTimeMicros);
                Reference ref = new Reference(RefType.CHILD_OF, span.id, childSpan.id);
                childSpan.refs.add(ref);
                maxEndTime.set(Math.max(maxEndTime.get(), childSpan.endTimeMicros));
                int childCode = childSpan.getHttpCodeOrDefault(200);
                if (childCode != 200) {
                    span.setHttpCode(childCode);
                    span.markError();
                }
            });
        }
        long ownDuration = TimeUnit.MILLISECONDS.toMicros((long)this.random.nextInt(om.maxLatencyMillis));
        span.endTimeMicros = maxEndTime.get() + ownDuration;
        trace.addSpan(span);
        return span;
    }
}
