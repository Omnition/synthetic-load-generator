package io.omnition.loadgenerator;

import java.util.ArrayList;
import java.util.List;

import io.omnition.loadgenerator.model.topology.Topology;

public class LoadGeneratorParams {
    public Topology topology;
    public List<RootServiceRoute> rootRoutes = new ArrayList<>();

    public class RootServiceRoute {
        public String service;
        public String route;
        public int tracesPerHour;
    }
}
