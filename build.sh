#!/bin/bash

PREFIX=${DOCKER_PREFIX:-blackbelt}
KARAF_VERSION="${KARAF_VERSION:-4.0.6}"

CWD=`dirname $0`

docker build -t "${PREFIX}/karaf" --build-arg KARAF_VERSION="${KARAF_VERSION}" "${CWD}"
docker build -t "${PREFIX}/karaf:${KARAF_VERSION}" --build-arg KARAF_VERSION="${KARAF_VERSION}" "${CWD}"

if [ "x1" == "x${PUSH_DOCKER_IMAGE}" ]
then
    docker push "${PREFIX}/karaf"
fi
