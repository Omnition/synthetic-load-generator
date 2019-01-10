#!/bin/sh

if [ -z "${JAEGER_COLLECTOR_URL}" ]; then JAEGER_COLLECTOR_URL=http://jaeger-collector:14268; fi
if [ -z "${TOPOLOGY_FILE}" ]; then TOPOLOGY_FILE=topologies/hipster-shop.json; fi

# PARAMS="--jaegerCollectorUrl ${JAEGER_COLLECTOR_URL}"

if [ ! -z "${ZIPKINV1_COLLECTOR_URL}" ]; then
    PARAMS="$PARAMS --zipkinV1CollectorUrl ${ZIPKINV1_COLLECTOR_URL}"
fi

if [ ! -z "${ZIPKINV2_COLLECTOR_URL}" ]; then
    PARAMS="$PARAMS --zipkinV2CollectorUrl ${ZIPKINV2_COLLECTOR_URL}"
fi

echo "using params: " $PARAMS

java -jar synthetic-load-generator.jar \
    --paramsFile ${TOPOLOGY_FILE} \
    $PARAMS

