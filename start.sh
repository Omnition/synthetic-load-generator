#!/bin/sh

JAEGER_COLLECTOR_URL?=http://jaeger-collector:14268
TOPOLOGY_FILE?=topologies/hipster-shop.json

java -jar synthetic-load-generator.jar \
    --paramsFile ${TOPOLOGY_FILE} \
    --jaegerCollectorUrl ${JAEGER_COLLECTOR_URL}

