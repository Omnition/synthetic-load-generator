package io.omnition.loadgenerator.model.trace;

import java.util.List;

public class Service {
    public String serviceName;
    public String instanceName;
    public List<KeyValue> tags;

    public Service(String serviceName, String instanceName, List<KeyValue> tags) {
        this.serviceName = serviceName;
        this.instanceName = instanceName;
        this.tags = tags;
    }
}
