/**
 * Performs the following steps:
 * * Reserve a random port and stores it in the `quarkus.http.port` Maven property
 * * Replace %quarkus.http.port% with the reserved random port in the file passed via `qcxf.hyperfoil.yaml` Maven property
 */
import java.nio.file.Files
import java.nio.file.Path
import java.net.ServerSocket

final String rawHyperfoilYamlPath = project.properties["qcxf.hyperfoil.yaml"];
if (rawHyperfoilYamlPath == null || rawHyperfoilYamlPath.isEmpty()) {
    throw new IllegalArgumentException('Maven property qcxf.hyperfoil.yaml must be set')
}
final Path hyperfoilYamlPath = Path.of(rawHyperfoilYamlPath)
if (!Files.isRegularFile(hyperfoilYamlPath)) {
    throw new IllegalArgumentException('Maven property qcxf.hyperfoil.yaml value ' + project.properties["qcxf.hyperfoil.yaml"] + ' does not exist')
}

// find a random port and set the quarkus.http.port Maven property
int port
try (ServerSocket socket = new ServerSocket(0)) {
    port = socket.localPort
}
project.properties["quarkus.http.port"] = String.valueOf(port)

// Replace the port in hyperfoilYamlPath
def newContent = hyperfoilYamlPath.getText('UTF-8')
newContent = newContent.replace('%quarkus.http.port%', String.valueOf(port))
def destPath = project.basedir.toPath().resolve('target/' + hyperfoilYamlPath.getFileName().toString())
if (!Files.exists(destPath) || !newContent.equals(destPath.getText('UTF-8'))) {
    if (!Files.exists(destPath.getParent())) {
        Files.createDirectories(destPath.getParent())
    }
    destPath.setText(newContent.toString(), 'UTF-8')
    println('Updated ' + destPath.toString())
} else {
    println(destPath.toString() + ' is up to date')
}

// Start the application under test
def javaBin = System.getProperty("java.home") + "/bin/java"
def cmd = [javaBin, '-Dquarkus.http.port='+port, "-jar", 'target/' + project.artifactId + '-'+ project.version +'.jar']
println('Starting the application under test:')
println(cmd.stream().collect(java.util.stream.Collectors.joining(' ')))
def process = new ProcessBuilder(cmd).start()
def consumeStream = { InputStream stream ->
    def executor = java.util.concurrent.Executors.newSingleThreadExecutor()
    executor.submit {
        stream.eachLine { line ->
            println "server: $line"
        }
    }
}
consumeStream(process.inputStream)
consumeStream(process.errorStream)

// Wait till the service is fully started
def healthUrl = 'http://localhost:' + port +'/q/health/ready'
org.awaitility.Awaitility.await().atMost(30, java.util.concurrent.TimeUnit.SECONDS).until(() -> {
    try {
        def resp = new java.net.URL(healthUrl).openConnection()
        return resp.getResponseCode() == 200
    } catch (Exception e) {
        return false;
    }
});
println('The service under tests is ready: ' + healthUrl)
