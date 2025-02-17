/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkiverse.cxf.it.client.tls;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.certs.CertificateGenerator;
import io.smallrye.certs.CertificateRequest;
import io.smallrye.certs.Format;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyStore;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClientTlsTestResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        final Map<String, String> props = new LinkedHashMap<>();
        final Path tempDir = Path.of("target/classes/certs");
        try {
            Files.createDirectories(tempDir);
            generateCerts(tempDir);
            configureTempCaCert(tempDir);
            props.put("javax.net.ssl.trustStore", tempDir + "/cacerts");
            return props;
        } catch (IOException e) {
            throw new RuntimeException("Could not create " + tempDir, e);
        }
    }

    private void generateCerts(Path tempDir) {
        // Generate self-signed certificate
        // We do not use the junit 5 plugin to avoid having to annotate all the tests to make sure the certs are
        // generated before the tests are run
        CertificateGenerator generator = new CertificateGenerator(tempDir, false);
        CertificateRequest cr = new CertificateRequest()
                .withName("localhost")
                .withFormat(Format.PKCS12)
                .withPassword("changeit")
                .withDuration(Duration.ofDays(2))
                .withCN("localhost");
        try {
            generator.generate(cr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void configureTempCaCert(Path tempDir) {

        Path systemCacertsPath = Path.of(System.getProperty("java.home"), "/lib/security/cacerts");
        Path tempCacertsPath = Path.of(tempDir + "/cacerts");
        String cacertsPassword = "changeit";

        try {
            Files.copy(systemCacertsPath, tempCacertsPath, StandardCopyOption.REPLACE_EXISTING);

            KeyStore cacerts = KeyStore.getInstance(KeyStore.getDefaultType());
            FileInputStream cacertsInput = new FileInputStream(tempCacertsPath.toString());
            cacerts.load(cacertsInput, cacertsPassword.toCharArray());

            Path p12FilePath = Path.of(tempDir + "/localhost-truststore.p12");
            String p12Password = "changeit";

            KeyStore p12Store = KeyStore.getInstance("PKCS12");
            FileInputStream p12Input = new FileInputStream(p12FilePath.toFile());
            p12Store.load(p12Input, p12Password.toCharArray());

            java.security.cert.Certificate cert = p12Store.getCertificate("localhost");
            cacerts.setCertificateEntry("localhost", cert);

            FileOutputStream cacertsOutput = new FileOutputStream(tempCacertsPath.toString());
            cacerts.store(cacertsOutput, cacertsPassword.toCharArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
    }
}
