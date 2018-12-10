package io.omnition.loadgenerator.model.topology;

import java.util.ArrayList;
import java.util.List;

public class Topology {
    public List<ServiceTier> services = new ArrayList<>();

    public ServiceTier getServiceTier(String serviceName) {
        return this.services.stream().filter(s -> s.serviceName.equalsIgnoreCase(serviceName))
                .findFirst().get();
    }
}
