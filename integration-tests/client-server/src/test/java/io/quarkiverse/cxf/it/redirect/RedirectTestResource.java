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

package io.quarkiverse.cxf.it.redirect;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class RedirectTestResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        final Map<String, String> props = new LinkedHashMap<>();
        final String tempDir = "target/RedirectTestResource-" + UUID.randomUUID().toString();
        try {
            Files.createDirectories(Path.of(tempDir));
            props.put("qcxf.retransmitCacheDir", tempDir);
            return props;
        } catch (IOException e) {
            throw new RuntimeException("Could not create " + tempDir, e);
        }
    }

    @Override
    public void stop() {
    }
}
