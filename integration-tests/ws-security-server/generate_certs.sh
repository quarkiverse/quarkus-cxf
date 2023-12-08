#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

echo "*** Creating tmp folder for related files ****"
mkdir tmp

echo "*** Generate keys ***"
openssl genrsa -out tmp/alice.key 2048
openssl genrsa -out tmp/bob.key 2048

echo "*** Certificate authority ***"
openssl genrsa -out tmp/cxfca.key 2048
openssl req -x509 -new -subj '/O=apache.org/OU=eng (NOT FOR PRODUCTION)/CN=cxfca' -key tmp/cxfca.key -nodes -out tmp/cxfca.pem -days 10000
openssl req -new -subj '/O=apache.org/OU=eng (NOT FOR PRODUCTION)/CN=cxfca' -x509 -key tmp/cxfca.key -days 10000 -out tmp/cxfca.crt

echo "*** Generate certificates ***"
openssl req -new -subj '/O=apache.org/OU=eng (NOT FOR PRODUCTION)/CN=alice' -key tmp/alice.key -out tmp/alice.csr
openssl x509 -req -in tmp/alice.csr -CA tmp/cxfca.pem -CAkey tmp/cxfca.key -CAcreateserial -days 10000 -out alice.crt
openssl req -new -subj '/O=apache.org/OU=eng (NOT FOR PRODUCTION)/CN=bob' -key tmp/bob.key -out tmp/bob.csr
openssl x509 -req -in tmp/bob.csr -CA tmp/cxfca.pem -CAkey tmp/cxfca.key -CAcreateserial -days 10000 -out bob.crt

echo "*** Export keystores ***"
openssl pkcs12 -export -in alice.crt -inkey tmp/alice.key -certfile tmp/cxfca.crt -name "alice" -out alice.p12 -passout pass:password -keypbe aes-256-cbc -certpbe aes-256-cbc
openssl pkcs12 -export -in bob.crt -inkey tmp/bob.key -certfile tmp/cxfca.crt -name "bob" -out bob.p12 -passout pass:password -keypbe aes-256-cbc -certpbe aes-256-cbc

keytool -import -trustcacerts -alias bob -file bob.crt -noprompt -keystore alice.p12 -storepass password
keytool -import -trustcacerts -alias alice -file alice.crt -noprompt -keystore bob.p12 -storepass password
