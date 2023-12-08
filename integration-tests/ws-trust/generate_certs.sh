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
openssl genrsa -out tmp/myclient.key 2048
openssl genrsa -out tmp/myservice.key 2048
openssl genrsa -out tmp/mysts.key 2048

echo "*** Certificate authority ***"
openssl genrsa -out tmp/cxfca.key 2048
openssl req -x509 -new -subj '/O=apache.org/OU=eng (NOT FOR PRODUCTION)/CN=cxfca' -key tmp/cxfca.key -nodes -out tmp/cxfca.pem -days 10000
openssl req -new -subj '/O=apache.org/OU=eng (NOT FOR PRODUCTION)/CN=cxfca' -x509 -key tmp/cxfca.key -days 10000 -out tmp/cxfca.crt

echo "*** Generate certificates ***"
openssl req -new -subj '/O=apache.org/OU=eng (NOT FOR PRODUCTION)/CN=myservice' -key tmp/myservice.key -out tmp/myservice.csr
openssl x509 -req -in tmp/myservice.csr -CA tmp/cxfca.pem -CAkey tmp/cxfca.key -CAcreateserial -days 10000 -out tmp/myservice.crt
openssl req -new -subj '/O=apache.org/OU=eng (NOT FOR PRODUCTION)/CN=mysts' -key tmp/mysts.key -out tmp/mysts.csr
openssl x509 -req -in tmp/mysts.csr -CA tmp/cxfca.pem -CAkey tmp/cxfca.key -CAcreateserial -days 10000 -out tmp/mysts.crt
openssl req -new -subj '/O=apache.org/OU=eng (NOT FOR PRODUCTION)/CN=myclient' -key tmp/myclient.key -out tmp/myclient.csr
openssl x509 -req -in tmp/myclient.csr -CA tmp/cxfca.pem -CAkey tmp/cxfca.key -CAcreateserial -days 10000 -out tmp/myclient.crt

echo "*** Export keystores ***"
openssl pkcs12 -export -in tmp/myservice.crt -inkey tmp/myservice.key -certfile tmp/cxfca.crt -name "myservicekey" -out servicestore.p12 -passout pass:skpass -keypbe aes-256-cbc -certpbe aes-256-cbc
keytool -import -trustcacerts -alias mystskey -file tmp/mysts.crt -noprompt -keystore servicestore.p12  -storepass skpass

openssl pkcs12 -export -in tmp/mysts.crt -inkey tmp/mysts.key -certfile tmp/cxfca.crt -name "mystskey" -out stsstore.p12 -passout pass:stsspass -keypbe aes-256-cbc -certpbe aes-256-cbc
keytool -import -trustcacerts -alias myservicekey -file tmp/myservice.crt -noprompt -keystore stsstore.p12 -storepass stsspass
keytool -import -trustcacerts -alias myclientkey -file tmp/myclient.crt -noprompt -keystore stsstore.p12 -storepass stsspass

openssl pkcs12 -export -in tmp/myclient.crt -inkey tmp/myclient.key -certfile tmp/cxfca.crt -name "myclientkey" -out clientstore.p12 -passout pass:cspass -keypbe aes-256-cbc -certpbe aes-256-cbc
keytool -import -trustcacerts -alias myservicekey -file tmp/myservice.crt -noprompt -keystore clientstore.p12 -storepass cspass
keytool -import -trustcacerts -alias mystskey -file tmp/mysts.crt -noprompt -keystore clientstore.p12 -storepass cspass

