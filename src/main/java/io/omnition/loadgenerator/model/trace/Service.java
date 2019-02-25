package io.omnition.loadgenerator.model.trace;

import java.util.List;

public class Service {
    public String serviceName;
    public List<KeyValue> tags;

    public Service(String serviceName, List<KeyValue> tags) {
        this.serviceName = serviceName;
        this.tags = tags;
    }
}
