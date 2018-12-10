package io.omnition.loadgenerator.model.topology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceTier {
    public String serviceName;
    public Map<String, String> tags = new HashMap<>();
    public List<ServiceRoute> routes = new ArrayList<>();
    public List<String> instances = new ArrayList<>();

    public ServiceRoute getRoute(String routeName) {
        return this.routes.stream()
                .filter(r -> r.route.equalsIgnoreCase(routeName))
                .findFirst().get();
    }

    public OperationModel getOperationModel(String instanceName, String routeName) {
        return this.routes.stream().filter(r -> r.route.equals(routeName))
                .findFirst().get().operationModel;
    }
}
