FROM blackbelt/oraclejdk8
MAINTAINER József Börcsök "jozsef.borcsok@blackbelt.hu"

ARG KARAF_VERSION
ARG CELLAR_VERSION

ENV KARAF_HOME="/opt/karaf" \
    UID="${UID:-60000}"

ENV HAWTIO_VERSION="1.5.4" \
    COLLECTD_VERSION="1.0.1" \
    OSGI_ENCRYPTION_VERSION="1.0.2" 

RUN if [ -z "${KARAF_VERSION}" ]; then echo -e "\033[0;31mRequired build argument is missing: KARAF_VERSION\033[0m"; exit 1; fi

ENV TGZ_FILE_NAME=apache-karaf-${KARAF_VERSION}.tar.gz
ENV KARAF_TGZ_URL=https://www.apache.org/dist/karaf/${KARAF_VERSION}/${TGZ_FILE_NAME}
ENV KARAF_TGZ_ARCHIVE_URL=https://archive.apache.org/dist/karaf/${KARAF_VERSION}/${TGZ_FILE_NAME}

# download Apache Karaf and verify signature, see https://www.apache.org/dist/karaf/KEYS
RUN set -e \
    && apk add --no-cache --virtual=build-dependencies gnupg ca-certificates wget \
    && for key in \
        B0AD60200FE5031B \
        BFF2EE42C8282E76 \
    ; do \
        gpg --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys "$key" || \
        gpg --keyserver pgp.mit.edu --recv-keys "$key" || \
        gpg --keyserver keyserver.pgp.com --recv-keys "$key" || \
        gpg --keyserver ha.pool.sks-keyservers.net --recv-keys "$key" ; \
    done \
    && (wget -O "/tmp/${TGZ_FILE_NAME}" "${KARAF_TGZ_URL}" || wget -O "/tmp/${TGZ_FILE_NAME}" "${KARAF_TGZ_ARCHIVE_URL}" ) \
    && (wget -O "/tmp/${TGZ_FILE_NAME}.asc" "${KARAF_TGZ_URL}.asc" || wget -O "/tmp/${TGZ_FILE_NAME}.asc" "${KARAF_TGZ_ARCHIVE_URL}.asc" ) \
    && gpg --batch --verify "/tmp/${TGZ_FILE_NAME}.asc" "/tmp/${TGZ_FILE_NAME}" \
    && rm "/tmp/${TGZ_FILE_NAME}.asc" \
    && apk del build-dependencies

# install Apache Karaf
RUN set -e \
    && mkdir -p "/opt" \
    && tar -xzf "/tmp/${TGZ_FILE_NAME}" -C "/opt" \
    && mv /opt/apache-karaf* "${KARAF_HOME}" \
    && rm "${KARAF_HOME}/bin/"*.bat \
    && mkdir "/deploy" \
    && cp "${KARAF_HOME}/etc/org.apache.felix.fileinstall-deploy.cfg" "${KARAF_HOME}/etc/org.apache.felix.fileinstall-external.cfg" \
    && sed -i 's/^\(felix\.fileinstall\.dir\s*=\s*\).*$/\1\/deploy/' "${KARAF_HOME}/etc/org.apache.felix.fileinstall-external.cfg" \
    && ln -s "${KARAF_HOME}/bin/karaf" "/usr/bin/karaf" \
    && rm "/tmp/${TGZ_FILE_NAME}" \
    && mkdir "${KARAF_HOME}/data/log" \
    && mkdir "${KARAF_HOME}/data/mavenIndexer" \
    && adduser -HD -u "${UID}" -h "${KARAF_HOME}" -s "/bin/sh" -G "users" "karaf" \
    && chown -R "karaf" "${KARAF_HOME}" \
    && chown "karaf" "/deploy"

USER karaf
WORKDIR "${KARAF_HOME}"

# set Java options, add "-XX:+UnlockCommercialFeatures -XX:+FlightRecorder" options to EXTRA_JAVA_OPTS environement variable to unlock commercial features
# workaround for version>=4.0.5: replace /bin/bash back to /bin/sh (Bash is not part of the image)
RUN set -e \
    && echo -e "encryption.algorithm=PBEWithSHA1AndDESEDE\nencryption.passwordEnvName=ENCRYPTION_PASSWORD\nencryptor.alias=default" > "${KARAF_HOME}/etc/hu.blackbelt.karaf.jasypt.services.DefaultStringEncryptorConfig.cfg" \
    && echo 'export EXTRA_JAVA_OPTS="${EXTRA_JAVA_OPTS} -XX:+UseG1GC -XX:-UseAdaptiveSizePolicy -Duser.timezone=UTC -Duser.language=en -Duser.country=US"' >> "${KARAF_HOME}/bin/setenv" \
    && sed s/'#!\/bin\/bash'/'#!\/bin\/sh'/ -i "${KARAF_HOME}/bin/karaf"

# add feature repositories and install
# Hawtio is disabled by default because of Maven indexer
RUN set -e \
    && "${KARAF_HOME}/bin/start" \
    && echo -n "Waiting to start Karaf server in Docker image ..." \
    && connecting=1; while [ ${connecting} -ne 0 ]; do sleep 2; timeout -t 10 "${KARAF_HOME}/bin/client" -h localhost version; connecting=$?; done \
    && echo "feature:repo-add mvn:io.hawt/hawtio-karaf/${HAWTIO_VERSION}/xml/features; \
    feature:repo-add mvn:org.apache.karaf.cellar/apache-karaf-cellar/${CELLAR_VERSION}/xml/features; \
    feature:repo-add mvn:hu.blackbelt/collectd-feature/${COLLECTD_VERSION}/xml/karaf4-features; \
    feature:repo-add mvn:hu.blackbelt/osgi-encryption-karaf-feature/${OSGI_ENCRYPTION_VERSION}/xml/karaf4-features; \
    feature:install scr; \
    feature:install eventadmin; \
    system:shutdown -f" | "${KARAF_HOME}/bin/client" -h localhost -b

VOLUME ["/deploy"]
VOLUME ["${KARAF_HOME}/data/log"]
VOLUME ["${KARAF_HOME}/data/mavenIndexer"]

# JMX
EXPOSE 1099 44444

# Debugger
EXPOSE 5005

# SSH
EXPOSE 8101

# HTTP
EXPOSE 8181

ENTRYPOINT ["/usr/bin/karaf"]
