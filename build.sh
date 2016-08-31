#!/bin/bash

PREFIX=${DOCKER_PREFIX:-blackbelt}
KARAF_VERSION="${KARAF_VERSION:-4.0.6}"
CELLAR_VERSION="${CELLAR_VERSION:-4.0.1}"

JCOLLECTD_VERSION="${JCOLLECTD_VERSION:-0.1-SNAPSHOT}"
JCOLLECTD_PATH="${JCOLLECTD_PATH:-http://www.joeb.hu}"

CWD=`dirname $0`
DOWNLOAD_DIR="${CWD}/download"

if [ ! -f "${DOWNLOAD_DIR}/jcollectd.jar" ]
then
    curl -o "${DOWNLOAD_DIR}/jcollectd.jar" "${JCOLLECTD_PATH}/jcollectd-${JCOLLECTD_VERSION}.jar"
fi

docker build -t "${PREFIX}/karaf" --build-arg KARAF_VERSION="${KARAF_VERSION}" --build-arg CELLAR_VERSION="${CELLAR_VERSION}" "${CWD}"
docker build -t "${PREFIX}/karaf:${KARAF_VERSION}" --build-arg KARAF_VERSION="${KARAF_VERSION}" --build-arg CELLAR_VERSION="${CELLAR_VERSION}" "${CWD}"

if [ "x1" == "x${PUSH_DOCKER_IMAGE}" ]
then
    docker push "${PREFIX}/karaf"
fi
