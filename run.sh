#!/bin/bash

LINK="--link monitoringdocker_influxdb_1:influxsrv"

docker run --rm -it -p 8181:8181 -p 8101:8101 -p 5005:5005 -p 44444:44444 -p 1099:1099 $LINK --env EXTRA_JAVA_OPTS='-XX:+UnlockCommercialFeatures -XX:+FlightRecorder -Djava.rmi.server.hostname=localhost -javaagent:/usr/lib/jvm/collectd/collectd.jar=/usr/lib/jvm/collectd/javalang-collectd.xml,/usr/lib/jvm/collectd/dbcp2-collectd.xml,/usr/lib/jvm/collectd/dozer-collectd.xml,/usr/lib/jvm/collectd/ehcache-collectd.xml -Dcollectd.host=influxsrv -Dcollectd.port=25826 -Dcollectd.interval=10 -Dcollectd.instance=karaf-root -Dcollectd.client=localhost' --env KARAF_DEBUG=1 blackbelt/karaf

