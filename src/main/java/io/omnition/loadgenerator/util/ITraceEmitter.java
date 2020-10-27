package io.omnition.loadgenerator.util;

import io.omnition.loadgenerator.model.trace.Trace;

public interface ITraceEmitter {

    String emit(Trace trace);

    void close();
}
