#!/bin/bash

PREFIX=${DOCKER_PREFIX:-blackbelt}
KARAF_VERSION="${KARAF_VERSION:-4.0.7}"
CELLAR_VERSION="${CELLAR_VERSION:-4.0.1}"

CWD=`dirname $0`
ARTIFACTS_DIR="${CWD}/artifacts"

MAVEN_COMMAND='mvn'
MAVEN_MAJOR=3
MAVEN_VERSION=3.3.9

JASYPT_SUPPORT_BRANCH='develop'
JASYPT_SUPPORT_SOURCES='https://github.com/BlackBeltTechnology/karaf-jasypt-support/archive/develop.zip'

function check_command {
    which "$1" > /dev/null 2>&1
    if [ "$?" -ne 0 ]
    then
        wget -O /tmp/apache-maven.tar.gz https://www.apache.org/dist/maven/maven-${MAVEN_MAJOR}/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz
        tar xf /tmp/apache-maven.tar.gz -C /tmp
        MAVEN_COMMAND="`ls -ad /tmp/apache-maven*/`bin/mvn"
    fi
}

check_command mvn

mkdir -p "${ARTIFACTS_DIR}"

set -e

wget -O "${CWD}/karaf-jasypt-support.zip" "${JASYPT_SUPPORT_SOURCES}"

rm -Rf "${CWD}/karaf-jasypt-support-${JASYPT_SUPPORT_BRANCH}"/
unzip "${CWD}/karaf-jasypt-support.zip" -d "${CWD}"
rm -f "${CWD}/karaf-jasypt-support.zip"

${MAVEN_COMMAND} clean install -Dmaven.test.skip=true -f "${CWD}/karaf-jasypt-support-${JASYPT_SUPPORT_BRANCH}/pom.xml"
rm -Rf /tmp/apache-maven*

cp ${CWD}/karaf-jasypt-support-${JASYPT_SUPPORT_BRANCH}/target/karaf-jasypt-support-*.jar "${ARTIFACTS_DIR}/karaf-jasypt-support.jar"

docker build -t "${PREFIX}/karaf" --build-arg KARAF_VERSION="${KARAF_VERSION}" --build-arg CELLAR_VERSION="${CELLAR_VERSION}" "${CWD}"
docker tag "${PREFIX}/karaf" "${PREFIX}/karaf:${KARAF_VERSION}"

if [ "x1" == "x${PUSH_DOCKER_IMAGE}" ]
then
    docker push "${PREFIX}/karaf"
fi

