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

    // Count of traces emited since summary log last printed
    private long emitsSinceLastSumLog;
    private long totalEmits;

    private final Logger logger;

    public SummaryLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * Count emitted traces and output summary if enough time elapsed since last
     * output. This function is thread safe and can be called from multipler trace
     * emitters concurrently.
     */
    public synchronized void logEmit(long emittedTraces) {
        // Count emits
        this.emitsSinceLastSumLog += emittedTraces;
        this.totalEmits += emittedTraces;

        // Check if it is time to output summary. Use nanoTime() as monotonic clock.
        long curNanoTime = System.nanoTime();
        long elapsedNano = curNanoTime - lastSumLogTimestampNano;
        if (elapsedNano >= SummaryLogger.SUM_LOG_PERIOD_NANOSEC) {
            // Calculate traces per second since last output
            double elapsedSec = (double) elapsedNano / 1_000_000_000; // Using ugly division since TimeUnit
                                                                      // does not support floating point operations.
            double tracePerSecond = elapsedSec > 0 ? (double) emitsSinceLastSumLog / elapsedSec : 0;

            logger.info(String.format("Emitted %d total, %d new traces, %.2f traces per second", totalEmits,
                    emitsSinceLastSumLog, tracePerSecond));

            // Reset counters to start measuring again
            lastSumLogTimestampNano = curNanoTime;
            emitsSinceLastSumLog = 0;
        }
    }
}
