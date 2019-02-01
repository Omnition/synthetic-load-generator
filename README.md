# Synthetic Load Generator

The synthetic load generator is a utility to generate synthetic operational
data (traces, metrics, logs, events) for a simulated microservice-based application.
The application is modeled through its topology and operation models for each
of the components within the topology.

## Building the Docker image

Invoke the Makefile:
```
make build
```

## Configuration

Configuration is done by JSON file. The json file should describe a complete topology by describing various services,
their routes, and the downstreamCalls that those routes make. `rootRoutes` indicate the root spans or ingress points
 into the topology, and can be configured with the number of traces per hour to send.

Different tags can also be added to each service or to each route. Tags added to a service will apply to all routes for
that service, whereas tags for a route will be added for that route only. The tags for a route supersede tags set for
a service. Services and routes can set tags probabilistically based on integer weights (default is 1). Additionally,
service can inherit tags from their direct caller by specifying the keys that should be inherited, which can be
useful for modeling region-locked flows. TagGenerators can also be added to add many tags with many values.

Simple Example JSON:
```json
{
  "topology" : {
    "services" : [
      {
        "serviceName" : "poke-mart",
        "instances" : [ "viridian-d847fdcf5-j6s2f", "pallet-79d8c8d6c8-9sbff" ],
        "tagSets" : [
          { "weight": 2, "tags": { "generation" : "v1", "region" : "kanto" }, "tagGenerators": [{"name": "pokemon-id-", "numTags": 32, "numVals": 152}] },
          { "tags": { "generation" : "v2", "region" : "johto" }}
        ],
        "routes" : [
          {
            "route" : "/product",
            "downstreamCalls" : { "pokemon-center" : "/Recover", "brock" : "/GetRecommendations" },
            "maxLatencyMillis": 200,
            "tagSets": [
              { "weight": 1, "tags": { "starter" : "charmander"}},
              { "weight": 1, "tags": { "starter" : "squirtle"}},
              { "weight": 1, "tags": { "starter" : "bulbasaur"}}
            ]
          }
        ]
      },
      {
        "serviceName" : "brock",
        "instances" : [ "pewter-a347fe1ce-g4sl1"],
        "tagSets" : [
          { "tags": { "uselss": true }, "inherit": ["region", "starter"]}
        ],
        "routes" : [
          {
            "route" : "/GetRecommendations",
            "downstreamCalls" : { },
            "maxLatencyMillis": 1000,
            "tagSets": [
              { "tags": { "loves" : "jenny"}},
              { "tags": { "loves" : "joy"}}
            ]
          }
        ]
      },
      {
        "serviceName" : "pokemon-center",
        "instances" : [ "cerulean-23kn9aajk-lk12d"],
        "tagSets" : [
          { "tags": { "generation" : "v1", "region" : "kanto"}, "inherit": ["starter"] }
        ],
        "routes" : [
          {
            "route" : "/Recover",
            "downstreamCalls" : { },
            "maxLatencyMillis": 300
          }
        ]
      }
    ]
  },
  "rootRoutes" : [
    {
      "service" : "poke-mart",
      "route" : "/product",
      "tracesPerHour" : 18000
    }
  ]
}
```

## Building and running locally

Building the JAR:
```
mvn package
```

Running locally with sample topology:
```
java -jar ./target/SyntheticLoadGenerator-1.0-SNAPSHOT-jar-with-dependencies.jar  --paramsFile ./topologies/hipster-shop.json --jaegerCollectorUrl http://localhost:14268
```

This assumes that you have the `jaeger-collector` component running and listening
on port 14268 for thrift/HTTP protocol/transport.
