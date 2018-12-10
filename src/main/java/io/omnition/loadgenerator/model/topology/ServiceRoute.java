package io.omnition.loadgenerator.model.topology;

import java.util.HashMap;
import java.util.Map;

public class ServiceRoute {
    public String route;
    public Map<String, String> downstreamCalls = new HashMap<>();
    public OperationModel operationModel;
}
