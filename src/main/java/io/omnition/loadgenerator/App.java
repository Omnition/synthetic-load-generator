package io.omnition.loadgenerator;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.omnition.loadgenerator.util.ITraceEmitter;
import io.omnition.loadgenerator.util.ZipkinTraceEmitter;
import org.apache.log4j.Logger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;

import io.omnition.loadgenerator.LoadGeneratorParams.RootServiceRoute;
import io.omnition.loadgenerator.util.JaegerTraceEmitter;
import io.omnition.loadgenerator.util.ScheduledTraceGenerator;

public class App {
    private final static Logger logger = Logger.getLogger(App.class);

    @Parameter(names = "--paramsFile", description = "Name of the file containing the topology params", required = true)
    private String topologyFile;

    @Parameter(names = "--jaegerCollectorUrl", description = "URL of the jaeger collector", required = false)
    private String jaegerCollectorUrl = null;

    @Parameter(names = "--zipkinV1CollectorUrl", description = "URL of the zipkinV1 collector", required = false)
    private String zipkinV1CollectorUrl = null;

    @Parameter(names = "--zipkinV2CollectorUrl", description = "URL of the zipkinV2 collector", required = false)
    private String zipkinV2CollectorUrl = null;

    @Parameter(names = "--flushIntervalMillis", description = "How often to flush traces", required = false)
    private long flushIntervalMillis = TimeUnit.SECONDS.toMillis(5);

    @Parameter(names = { "--help", "-h" }, help = true)
    private boolean help;

    private List<ScheduledTraceGenerator> scheduledTraceGenerators = new ArrayList<>();
    private final CountDownLatch latch = new CountDownLatch(1);

    public static void main(String[] args) {
        App app = new App();
        JCommander.newBuilder().addObject(app).build().parse(args);
        try {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        app.shutdown();
                    } catch (Exception e) {
                        logger.error("Error shutting down: " + e, e);
                    }
                }
            });
            app.init();
            app.start();
        } catch (Exception e) {
            logger.error("Error running load generator: " + e, e);
            System.exit(1);
        }
    }

    public void init() throws Exception {
        File f = new File(this.topologyFile);
        if(!f.exists() || f.isDirectory()) {
            logger.error("Invalid topology file specified: " + this.topologyFile);
            throw new FileNotFoundException(this.topologyFile);
        }

        String json = new String(Files.readAllBytes(Paths.get(this.topologyFile)), "UTF-8");
        Gson gson = new Gson();
        LoadGeneratorParams params = gson.fromJson(json, LoadGeneratorParams.class);
        logger.info("Params: " + gson.toJson(params));
        List<ITraceEmitter> emitters = getTraceEmitters();
        for (ITraceEmitter emitter : emitters) {
            for (RootServiceRoute route : params.rootRoutes) {
                this.scheduledTraceGenerators.add(new ScheduledTraceGenerator(
                        params.topology, route.service, route.route,
                        route.tracesPerHour, emitter));
            }
        }
    }

    public void start() throws Exception {
        for (ScheduledTraceGenerator gen : this.scheduledTraceGenerators) {
            gen.start();
        }
        latch.await();
    }

    public void shutdown() throws Exception {
        for (ScheduledTraceGenerator gen : this.scheduledTraceGenerators) {
            gen.shutdown();
        }
        for (ScheduledTraceGenerator gen : this.scheduledTraceGenerators) {
            gen.awaitTermination();
        }
        latch.countDown();
    }

    private List<ITraceEmitter> getTraceEmitters() {
        List<ITraceEmitter> emitters = new ArrayList<>(3);
        if (jaegerCollectorUrl != null) {
            emitters.add(new JaegerTraceEmitter(jaegerCollectorUrl, (int) flushIntervalMillis));
        }
        if (zipkinV1CollectorUrl != null) {
            emitters.add(new ZipkinTraceEmitter(zipkinV1CollectorUrl, false));
        }
        if (zipkinV2CollectorUrl != null) {
            emitters.add(new ZipkinTraceEmitter(zipkinV2CollectorUrl, true));
        }
        if (emitters.size() == 0) {
            logger.error("No emitters specified.");
            throw new IllegalArgumentException("No emitters specified");
        }

        return emitters;
    }
}
