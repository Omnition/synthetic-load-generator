package io.omnition.loadgenerator.util;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import io.omnition.loadgenerator.model.topology.Topology;
import io.omnition.loadgenerator.model.trace.Trace;

public class ScheduledTraceGenerator {
    private static final Logger logger = Logger.getLogger(ScheduledTraceGenerator.class);
    private static final long GRACEFUL_SHUTDOWN_TIME_SEC = 5;
    private static final int NUM_THREADS = 5;
    private final ScheduledExecutorService scheduler;

    private final int tracesPerHour;
    private final Topology topology;
    private final String service;
    private final String route;
    private final ITraceEmitter emitter;

    public ScheduledTraceGenerator(
            Topology topology, String service, String route, int tracesPerHour, ITraceEmitter emitter) {
        this.scheduler = Executors.newScheduledThreadPool(NUM_THREADS);
        this.tracesPerHour = tracesPerHour;
        this.topology = topology;
        this.service = service;
        this.route = route;
        this.emitter = emitter;
    }

    public void start() {
        logger.info(String.format("Starting trace generation for service %s, route %s, %d traces/hr",
                this.service, this.route, this.tracesPerHour));
        scheduler.scheduleAtFixedRate(() -> emitOneTrace(), TimeUnit.SECONDS.toMillis(1),
                TimeUnit.HOURS.toMillis(1) / this.tracesPerHour, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(GRACEFUL_SHUTDOWN_TIME_SEC, TimeUnit.SECONDS)) {
                logger.error("Executor did not terminate in the specified time.");
                List<Runnable> droppedTasks = scheduler.shutdownNow(); // optional **
                logger.error("Executor was abruptly shut down. " + droppedTasks.size()
                        + " tasks will not be executed.");
            } else {
                logger.info("Graceful shutdown completed");
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted while waiting for termination", e);
        }
    }

    public void awaitTermination() throws InterruptedException {
        scheduler.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

    private void emitOneTrace() {
        try {
            long now = TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis());
            Trace trace = TraceGenerator.generate(this.topology, this.service, this.route, now);
            String traceId = this.emitter.emit(trace);
            logger.info(String.format("Emitted traceId %s for service %s route %s",
                    traceId, this.service, this.route));
        } catch (Exception e) {
            logger.error(String.format("Error emit trace for service %s route %s, reason: %s",
                    this.service, this.route, e), e);
        }
    }
}
