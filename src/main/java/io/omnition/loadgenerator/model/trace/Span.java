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

    public boolean isErrorSpan() {
        return tags.stream()
            .anyMatch(kv -> (kv.key.equalsIgnoreCase(SpanConventions.HTTP_STATUS_CODE_KEY) && kv.valueLong != 200)
                || (kv.key.equalsIgnoreCase(SpanConventions.IS_ERROR_KEY) && kv.valueBool));
    }

    public void setHttpCode(int code) {
        KeyValue httpTag = tags.stream()
            .filter(kv -> kv.key.equalsIgnoreCase(SpanConventions.HTTP_STATUS_CODE_KEY))
            .findFirst().orElse(null);
        if (httpTag == null) {
            this.tags.add(KeyValue.ofLongType(SpanConventions.HTTP_STATUS_CODE_KEY, (long) code));
        } else {
            httpTag.valueLong = (long)code;
        }
    }

    public Integer getHttpCode() {
        return tags.stream()
            .filter(kv -> kv.key.equalsIgnoreCase(SpanConventions.HTTP_STATUS_CODE_KEY))
            .map(kv -> kv.valueLong.intValue())
            .findFirst().orElse(null);
    }

    public void setHttpUrlTag(String url) {
        this.tags.add(KeyValue.ofStringType(SpanConventions.HTTP_URL_KEY, url));
    }

    public void setHttpMethodTag(String method) {
        this.tags.add(KeyValue.ofStringType(SpanConventions.HTTP_METHOD_KEY, method));
    }
}
