#!/bin/bash

set -e
set -x

invocationDir="$(pwd)"
workDir="target/openssl-work"
destinationDir="target/classes"
keySize=2048
days=10000
extFile="$(pwd)/v3.ext"
encryptionAlgo="aes-256-cbc"

if [[ -n "${JAVA_HOME}" ]] ; then
  keytool="$JAVA_HOME/bin/keytool"
elif ! [[ -x "$(command -v keytool)" ]] ; then
  echo 'Error: Either add keytool to PATH or set JAVA_HOME' >&2
  exit 1
else
  keytool="keytool"
fi

if ! [[ -x "$(command -v openssl)" ]] ; then
  echo 'Error: openssl is not installed.' >&2
  exit 1
fi

mkdir -p "$workDir"
mkdir -p "$destinationDir"

# Certificate authority
openssl genrsa -out "$workDir/cxfca.key" $keySize
openssl req -x509 -new -subj '/O=apache.org/OU=eng (NOT FOR PRODUCTION)/CN=cxfca' -key "$workDir/cxfca.key" -nodes -out "$workDir/cxfca.pem" -days $days -extensions v3_req
openssl req -new -subj '/O=apache.org/OU=eng (NOT FOR PRODUCTION)/CN=cxfca' -x509 -key "$workDir/cxfca.key" -days $days -out "$workDir/cxfca.crt"

for actor in alice bob localhost; do
  # Generate keys
  openssl genrsa -out "$workDir/$actor.key" $keySize

  # Generate certificates
  openssl req -new -subj "/O=apache.org/OU=eng (NOT FOR PRODUCTION)/CN=$actor" -key "$workDir/$actor.key"  -out "$workDir/$actor.csr"
  openssl x509 -req -in "$workDir/$actor.csr" -extfile "$extFile" -CA "$workDir/cxfca.pem" -CAkey "$workDir/cxfca.key" -CAcreateserial -days $days -out "$workDir/$actor.crt"

  # Export keystores
  openssl pkcs12 -export -in "$workDir/$actor.crt" -inkey "$workDir/$actor.key" -certfile "$workDir/cxfca.crt" -name "$actor" -out "$destinationDir/$actor-keystore.pkcs12" -passout pass:"${actor}-keystore-password" -keypbe "$encryptionAlgo" -certpbe "$encryptionAlgo"
done


# Truststores
"$keytool" -import -file "$workDir/localhost.crt" -alias localhost -noprompt -keystore "$destinationDir/client-truststore.pkcs12" -storepass "client-truststore-password"
"$keytool" -import -file "$workDir/cxfca.crt"     -alias cxfca     -noprompt -keystore "$destinationDir/client-truststore.pkcs12" -storepass "client-truststore-password"

# We use conbined key- and truststores for alice and bob
"$keytool" -import -file "$workDir/bob.crt"       -alias bob       -noprompt -keystore "$destinationDir/alice-keystore.pkcs12"  -storepass "alice-keystore-password"
"$keytool" -import -file "$workDir/localhost.crt" -alias localhost -noprompt -keystore "$destinationDir/alice-keystore.pkcs12"  -storepass "alice-keystore-password"
#"$keytool" -import -file "$workDir/cxfca.crt"     -alias cxfca     -noprompt -keystore "$destinationDir/alice-keystore.pkcs12"  -storepass "alice-keystore-password"

"$keytool" -import -file "$workDir/alice.crt"     -alias alice     -noprompt -keystore "$destinationDir/bob-keystore.pkcs12"    -storepass "bob-keystore-password"
"$keytool" -import -file "$workDir/localhost.crt" -alias localhost -noprompt -keystore "$destinationDir/bob-keystore.pkcs12"    -storepass "bob-keystore-password"
#"$keytool" -import -file "$workDir/cxfca.crt"     -alias cxfca     -noprompt -keystore "$destinationDir/bob-keystore.pkcs12"    -storepass "bob-keystore-password"
