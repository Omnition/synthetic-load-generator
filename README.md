# Synthetic Load Generator

The synthetic load generator is a utility to generate synthetic operational
data (traces, metrics, logs, events) for a simulated microservice-based application.
The application is modeled through its topology and operation models for each
of the components within the topology.

## Building and running

Building:
```
mvn package
```

Running with sample topology:
```
java -jar ./target/SyntheticLoadGenerator-1.0-SNAPSHOT-jar-with-dependencies.jar  --paramsFile ./topologies/hipster-shop.json --jaegerCollectorUrl http://localhost:14268
```

This assumes that you have the `jaeger-collector` component running and listening
on port 14268 for thrift/HTTP protocol/transport.
