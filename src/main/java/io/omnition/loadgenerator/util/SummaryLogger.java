package io.omnition.loadgenerator.util;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

/**
 * SummaryLogger maintains global count of emitted traces and periodically
 * outputs emitted counters and speed. Intended to be used as a single instance
 * per app.
 */
public class SummaryLogger {
    // How often to print summary log. 1 second.
    private static final long SUM_LOG_PERIOD_NANOSEC = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);

    // Last time summary log was printed, in monotinic nanoseconds
    private long lastSumLogTimestampNano;

    // Helper class to store emit counters (last since logged and total).
    class EmitCounters {
        public long emitsSinceLastSumLog;
        public long totalEmits;

        public void add(long counter) {
            emitsSinceLastSumLog += counter;
            totalEmits += counter;
        }

        public void copyFrom(EmitCounters from) {
            emitsSinceLastSumLog = from.emitsSinceLastSumLog;
            totalEmits = from.totalEmits;
        }

        public void resetLast() {
            emitsSinceLastSumLog = 0;
        }

        public double calcRate(double elapsedSec) {
            return elapsedSec > 0 ? (double) emitsSinceLastSumLog / elapsedSec : 0;
        }

        public String getPrintable(double elapsedSec) {
            return String.format("%d total, %d new, %.2f per second", totalEmits, emitsSinceLastSumLog,
                    calcRate(elapsedSec));
        }
    }

    // Count of traces emited since summary log last printed
    private EmitCounters traceCounters = new EmitCounters();
    private EmitCounters spanCounters = new EmitCounters();

    private final Logger logger;

    public SummaryLogger(Logger logger) {
        this.logger = logger;
    }

    public void flush() {
        logEmit(0, 0, true);
    }

    /**
     * Count emitted traces and output summary if enough time elapsed since last
     * output. This function is thread safe and can be called from multipler trace
     * emitters concurrently.
     */
    public void logEmit(long emittedTraces, long emittedSpans) {
        logEmit(emittedTraces, emittedSpans, false);
    }

    public void logEmit(long emittedTraces, long emittedSpans, boolean flushLog) {
        // Keep data that will be logged in local variables that will live after the
        // synchornized block.
        boolean shouldEmitLog = false;
        double elapsedSec = 0;
        EmitCounters traceCounters = new EmitCounters();
        EmitCounters spanCounters = new EmitCounters();

        // Check if logging is needed and obtain data to log.
        synchronized (this) {
            // Count trace and spans emitted.
            this.traceCounters.add(emittedTraces);
            this.spanCounters.add(emittedSpans);

            boolean forceLog = false;
            if (flushLog && (this.traceCounters.emitsSinceLastSumLog!=0 || this.spanCounters.emitsSinceLastSumLog!=0)) {
                // We were requested to flush the log. We must do it if any of the counters are changed since last log.
                forceLog = true;
            }

            // Copy counters to local variables to be used outside synchronized block.
            traceCounters.copyFrom(this.traceCounters);
            spanCounters.copyFrom(this.spanCounters);

            // Check if it is time to output summary. Use nanoTime() as monotonic clock.
            long curNanoTime = System.nanoTime();
            long elapsedNano = curNanoTime - lastSumLogTimestampNano;

            if (elapsedNano >= SummaryLogger.SUM_LOG_PERIOD_NANOSEC || forceLog) {
                // Calculate elapsed time in seconds since last output.
                // Using ugly division since TimeUnit does not support floating point
                // operations.
                elapsedSec = (double) elapsedNano / 1_000_000_000;

                // Reset counters to start measuring next cycle.
                lastSumLogTimestampNano = curNanoTime;
                this.traceCounters.resetLast();
                this.spanCounters.resetLast();

                shouldEmitLog = true;
            }
        }

        // Emit the log outsize synchronized block to avoid contention of other callers
        // on logging.
        if (shouldEmitLog) {
            logger.info(String.format("Emitted Traces: " + traceCounters.getPrintable(elapsedSec) + ", " + "Spans:  "
                    + spanCounters.getPrintable(elapsedSec)));
        }
    }
}
