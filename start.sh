#!/bin/sh

if [ -z "${JAEGER_COLLECTOR_URL}" ]; then JAEGER_COLLECTOR_URL=http://jaeger-collector:14268; fi
if [ -z "${TOPOLOGY_FILE}" ]; then TOPOLOGY_FILE=topologies/hipster-shop.json; fi

java -jar synthetic-load-generator.jar \
    --paramsFile ${TOPOLOGY_FILE} \
    --jaegerCollectorUrl ${JAEGER_COLLECTOR_URL}

