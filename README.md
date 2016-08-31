# karaf-docker

[Apache Karaf](http://karaf.apache.org) (base) container

## Usage

* Start Apache Karaf instance and cleanup on shutdown

```docker run -it --rm blackbelt/karaf```

* Add the following arguments to expose ports:

```-p 8181:8181 -p 8101:8101 -p 5005:5005```

* Add the following arguments to enable Oracle JDK commercial features:

```--env EXTRA_JAVA_OPTS='-XX:+UnlockCommercialFeatures -XX:+FlightRecorder'```

* Add the following arguments for debug mode:

```--env KARAF_DEBUG=1 blackbelt/karaf```

* Add the following arguments to enable JMX (replace hostname 'localhost' with valid value):
 * JMX URL: service:jmx:rmi://localhost:44444/jndi/rmi://localhost:1099/karaf-root

```-p 1099:1099 -p 44444:44444 --env EXTRA_JAVA_OPTS=-Djava.rmi.server.hostname=localhost```

