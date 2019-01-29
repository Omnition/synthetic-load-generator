#!/bin/sh

if [ -z "${JAEGER_COLLECTOR_URL}" ]; then JAEGER_COLLECTOR_URL=http://jaeger-collector:14268; fi
if [ -z "${TOPOLOGY_FILE}" ]; then TOPOLOGY_FILE=./topologies/hipster-shop.json; fi

if [ ! -z "${ZIPKINV1_JSON_URL}" ]; then
    PARAMS="$PARAMS --zipkinV1JsonUrl ${ZIPKINV1_JSON_URL}"
fi

if [ ! -z "${ZIPKINV2_JSON_URL}" ]; then
    PARAMS="$PARAMS --zipkinV2JsonUrl ${ZIPKINV2_JSON_URL}"
fi

if [ ! -z "${ZIPKINV1_THRIFT_URL}" ]; then
    PARAMS="$PARAMS --zipkinV1ThriftUrl ${ZIPKINV1_THRIFT_URL}"
fi

if [ ! -z "${ZIPKINV2_PROTO3_URL}" ]; then
    PARAMS="$PARAMS --zipkinV2Proto3Url ${ZIPKINV2_PROTO3_URL}"
fi

if [ -z "${PARAMS}" ]; then
    PARAMS="--jaegerCollectorUrl ${JAEGER_COLLECTOR_URL}"
fi

echo "using params: " ${PARAMS}

java -jar synthetic-load-generator.jar \
    --paramsFile ${TOPOLOGY_FILE} \
    $PARAMS

