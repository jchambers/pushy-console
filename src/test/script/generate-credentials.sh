#!/bin/sh

# Generate a new, self-signed root CA
openssl req -config openssl-custom.cnf -extensions v3_ca -new -x509 -days 36500 -nodes -subj "/CN=PushyTestRoot" -newkey rsa:2048 -sha512 -out ca.pem -keyout ca.key

# Generate a multi-topic client certificate and pack it and its private key into a PKCS#12 keystore
openssl req -new -keyout apns-client.key -nodes -newkey rsa:2048 -subj "/CN=Apple Push Services: com.eatthepath.pushy/UID=com.eatthepath.pushy" | \
    openssl x509 -extfile ./apns-extensions.cnf -extensions apns_multi_topic_client_extensions -req -CAkey ca.key -CA ca.pem -days 36500 -set_serial 1 -sha512 -out apns-client.pem

openssl pkcs12 -export -in apns-client.pem -inkey apns-client.key -out apns-client.p12 -password pass:pushy-test

# Generate a private key for token authentication testing
openssl ecparam -name prime256v1 -genkey -noout | openssl pkcs8 -topk8 -nocrypt -out APNsAuthKey_KEYIDKEYID.p8

# Clean up intermediate files
rm *.key *.pem
