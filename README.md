# karaf-docker

[Apache Karaf](http://karaf.apache.org) (base) container

## Build

build.sh script can be used to build, tag and push Docker image.

### Environment variables for configuration

* DOCKER_PREFIX: prefix of Docker image (default: blackbelt)
* KARAF_VERSION: Apache Karaf version number
* CELLAR_VERSION: Apache Cellar version number (for clustering)
* PUSH_DOCKER_IMAGE: push Docker image to registry (Docker Hub or private repository, 'docker login' command is required before)
* HAWTIO_VERSION: which version of hawtio feature repository should be added

## Usage

* Start Apache Karaf instance and cleanup on shutdown

```docker run -it --rm blackbelt/karaf```

* Add the following arguments to expose ports:

```-p 8181:8181 -p 8101:8101 -p 5005:5005```

* Add the following arguments to enable Oracle JDK commercial features:

```--env EXTRA_JAVA_OPTS='-XX:+UnlockCommercialFeatures -XX:+FlightRecorder'```

* Add the following arguments for debug mode:

```--env KARAF_DEBUG=1```

* Add the following arguments to enable JMX (replace hostname 'localhost' with valid value):
 * JMX URL: service:jmx:rmi://localhost:44444/jndi/rmi://localhost:1099/karaf-root

```-p 1099:1099 -p 44444:44444 --env EXTRA_JAVA_OPTS=-Djava.rmi.server.hostname=localhost```

* Publish JVM metrics using collectd protocol (replace monitor_influxdb_1 with InfluxDB Docker container name)
```--link monitor_influxdb_1:influxsrv --env EXTRA_JAVA_OPTS='-javaagent:/usr/lib/jvm/collectd/jcollectd.jar -Djcd.dest=udp://influxsrv:25826 -Djcd.mx.interval=10 -Djcd.tmpl=/usr/lib/jvm/collectd/javalang,/usr/lib/jvm/collectd/dbcp2,/usr/lib/jvm/collectd/dozer,/usr/lib/jvm/collectd/ehcache -Djcd.instance=karaf-root'```
