package io.omnition.loadgenerator.model.trace;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.omnition.loadgenerator.util.SpanConventions;

public class Span {
    public final UUID id = UUID.randomUUID();
    public Service service;
    public Long startTimeMicros;
    public Long endTimeMicros;
    public String operationName;
    public List<KeyValue> tags = new ArrayList<>();
    public List<Reference> refs = new ArrayList<>();

    public void markError() {
        this.tags.add(KeyValue.ofBooleanType(SpanConventions.IS_ERROR_KEY, true));
    }

    public void markRootCauseError() {
        this.tags.add(KeyValue.ofBooleanType(SpanConventions.IS_ROOT_CAUSE_ERROR_KEY, true));
    }

    public void setHttpCode(int code) {
        this.tags.add(KeyValue.ofLongType(SpanConventions.HTTP_STATUS_CODE_KEY, (long)code));
    }

    public int getHttpCodeOrDefault(int defaultCode) {
        long code = tags.stream()
                .filter(kv -> kv.key.equalsIgnoreCase(SpanConventions.HTTP_STATUS_CODE_KEY))
                .map(kv -> kv.valueLong)
                .findFirst().orElse((long)defaultCode);
        return (int)code;
    }

    public void setHttpUrlTag(String url) {
        this.tags.add(KeyValue.ofStringType(SpanConventions.HTTP_URL_KEY, url));
    }

    public void setHttpMethodTag(String method) {
        this.tags.add(KeyValue.ofStringType(SpanConventions.HTTP_METHOD_KEY, method));
    }
}
