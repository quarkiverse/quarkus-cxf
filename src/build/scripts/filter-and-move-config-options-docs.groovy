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

/**
 * Remove the docs of all config options starting with quarkus.cxf.internal
 */
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files


final Path inputFile = Paths.get(properties['inputFile'])
final Path outputDirectory = Paths.get(properties['outputDirectory'])

final String content = inputFile.getText('UTF-8')

final String propPrefix = 'a| [[quarkus-cxf_quarkus.cxf.internal'
final int start = content.indexOf(propPrefix)
if (start < 0) {
    throw new IllegalStateException('Could not find ' + propPrefix + ' in '+ inputFile);
}

final int lastStart = content.lastIndexOf(propPrefix)

final String nextPrefix = '\n\na|'
final int end = content.indexOf(nextPrefix, lastStart)
if (end < 0) {
    throw new IllegalStateException('Could not find ' + nextPrefix + ' in '+ inputFile);
}

String newContentString = content.substring(0, start) + content.substring(end + 2)

if (!Files.exists(outputDirectory)) {
    Files.createDirectories(outputDirectory)
}

final Path outputFile = outputDirectory.resolve(inputFile.getFileName())
if (!Files.exists(outputFile) || !newContentString.equals(outputFile.getText('UTF-8'))) {
    println 'Updated ' + outputFile
    Files.write(outputFile, newContentString.getBytes('UTF-8'))
} else {
    println 'No change in ' + outputFile
}
