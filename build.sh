#!/bin/bash

PREFIX=${DOCKER_PREFIX:-blackbelt}
KARAF_VERSION="${KARAF_VERSION:-4.0.6}"
CELLAR_VERSION="${CELLAR_VERSION:-4.0.1}"

CWD=`dirname $0`

docker build -t "${PREFIX}/karaf" --build-arg KARAF_VERSION="${KARAF_VERSION}" --build-arg CELLAR_VERSION="${CELLAR_VERSION}" "${CWD}"
docker tag "${PREFIX}/karaf" "${PREFIX}/karaf:${KARAF_VERSION}"

if [ "x1" == "x${PUSH_DOCKER_IMAGE}" ]
then
    docker push "${PREFIX}/karaf"
fi
