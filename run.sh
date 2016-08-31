#!/bin/bash

docker run -it --rm -p 8181:8181 -p 8101:8101 -p 5005:5005 -p 1099:1099 -p 44444:44444 --env EXTRA_JAVA_OPTS='-XX:+UnlockCommercialFeatures -XX:+FlightRecorder' --env KARAF_DEBUG=1 --env EXTRA_JAVA_OPTS=-Djava.rmi.server.hostname=localhost blackbelt/karaf

