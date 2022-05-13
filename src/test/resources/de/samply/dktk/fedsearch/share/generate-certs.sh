#!/usr/bin/env sh

# CA
openssl genrsa -out ca.key 2048
openssl req -x509 -sha256 -days 1825 -key ca.key -new -out ca.crt

# Broker
openssl genrsa -out broker.key 2048
openssl req -key broker.key -new -out broker.csr
openssl x509 -req -CA ca.crt -CAkey ca.key -in broker.csr -out broker.crt \
  -days 1825 -sha256 -CAcreateserial -extfile broker.ext

# Java Truststore
keytool -importcert -file ca.crt -keystore ca.jks -storepass password
