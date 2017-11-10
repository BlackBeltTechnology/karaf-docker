#!/bin/sh

# copy secrets to HOST_SECRETS_DIR for Karaf containers (not Swarm services)

app="${1:-sandbox}"
offset="${2:-1000}"

HOSTNAME_DNS="`hostname -A | cut -d' ' -f 1`"
HOSTNAME_LOCAL="`hostname -f`"
HOSTNAME="${HOSTNAME_DNS:-$HOSTNAME_LOCAL}"

DAEMON='0'
KARAF_DEBUG='1'

RMI_HOSTNAME="${HOSTNAME}"
DOCKER_HOSTNAME="docker-${HOSTNAME}"
HOST_SECRETS_DIR='/opt/secret'
SECRETS_DIR='/run/secrets'
COLLECTD_HOST='239.192.74.66'
COLLECTD_PORT='25826'
COLLECTD_INSTANCE="karaf-${HOSTNAME}"
COLLECTD_INTERVAL=1000
COLLECTD_OPTS="-javaagent:/usr/lib/jvm/collectd/collectd.jar=/usr/lib/jvm/collectd/javalang-collectd.xml,/usr/lib/jvm/collectd/dbcp2-collectd.xml,/usr/lib/jvm/collectd/dozer-collectd.xml,/usr/lib/jvm/collectd/ehcache-collectd.xml -Dcollectd.host=${COLLECTD_HOST} -Dcollectd.port=${COLLECTD_PORT} -Dcollectd.interval=${COLLECTD_INTERVAL} -Dcollectd.instance=${COLLECTD_INSTANCE}"
EXTRA_JAVA_OPTS="-Dorg.slf4j.simpleLogger.log.org.collectd=INFO -XX:+UnlockCommercialFeatures -XX:+FlightRecorder ${COLLECTD_OPTS} -Djava.rmi.server.hostname=${RMI_HOSTNAME}"

RMI_SERVER_PORT_BASE="10044"
RMI_SERVER_PORT="$((${RMI_SERVER_PORT_BASE} + ${offset}))"

KARAF_HTTP_PORT="$((10080 + ${offset}))"
KARAF_DEBUGGER_PORT="$((10005 + ${offset}))"
KARAF_SSH_PORT="$((10022 + ${offset}))"
KARAF_RMI_REGISTRY_PORT="$((10099 + ${offset}))"
KARAF_RMI_SERVER_PORT="${RMI_SERVER_PORT:-44444}"

#DNS_OPTS="--dns 134.65.0.1"

DOCKER_IMAGE_NAME="blackbelt/karaf"

# add the following option to mount Maven local repository: -v "${HOME}/.m2:/opt/karaf/.m2"

if [ "${DAEMON}" == "1" ]
then
    DAEMON_OPTS="-d --name karaf-${app}"
fi

echo docker run -it --rm ${DAEMON_OPTS} \
	--hostname "${DOCKER_HOSTNAME}" \
	-p "${KARAF_HTTP_PORT}:8181" \
	-p "${KARAF_SSH_PORT}:8101" \
	-p "${KARAF_DEBUGGER_PORT}:5005" \
	-p "${KARAF_RMI_REGISTRY_PORT}:1099" \
	-p "${KARAF_RMI_SERVER_PORT}:${KARAF_RMI_SERVER_PORT}" \
	-v "${HOST_SECRETS_DIR}:${SECRETS_DIR}" ${DNS_OPTS} \
	--env "KARAF_DEBUG=${KARAF_DEBUG}" \
	--env "SECRETS=${SECRETS_DIR}" \
	--env "EXTRA_JAVA_OPTS=${EXTRA_JAVA_OPTS}" \
	--env "RMI_SERVER_PORT=${RMI_SERVER_PORT}" \
	"${DOCKER_IMAGE_NAME}"

NUMBER_OF_NODES=1
echo docker service create -td --name ${app} ${DNS_OPTS} --replicas ${NUMBER_OF_NODES} \
	-p "${KARAF_HTTP_PORT}:8181" \
        -p "${KARAF_SSH_PORT}:8101" \
        -p "${KARAF_DEBUGGER_PORT}:5005" \
        -p "${KARAF_RMI_REGISTRY_PORT}:1099" \
        -p "${KARAF_RMI_SERVER_PORT}:${KARAF_RMI_SERVER_PORT}" \
	--env "KARAF_DEBUG=${KARAF_DEBUG}" \
        --env "SECRETS=${SECRETS_DIR}" \
        --env "EXTRA_JAVA_OPTS=${EXTRA_JAVA_OPTS}" \
        --env "RMI_SERVER_PORT=${RMI_SERVER_PORT}" \
	"${DOCKER_IMAGE_NAME}"

