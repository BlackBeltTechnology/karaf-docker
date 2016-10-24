$ docker run -it --rm -p 8181:8181 -v /tmp/deploy:/deploy -e "ENCRYPTION_PASSWORD=EncryptionPassword" blackbelt/karaf
 
> feature:install jasypt-encryption
> feature:install pax-jdbc-pool-dbcp2 pax-jdbc-config jdbc pax-jdbc-postgresql

