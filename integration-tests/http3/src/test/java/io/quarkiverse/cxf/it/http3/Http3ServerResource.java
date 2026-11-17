package io.quarkiverse.cxf.it.http3;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.certs.CertificateGenerator;
import io.smallrye.certs.CertificateRequest;
import io.smallrye.certs.Format;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerConfig;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.PfxOptions;
import io.vertx.core.net.ServerSSLOptions;

public class Http3ServerResource implements QuarkusTestResourceLifecycleManager {
    private static final String HOST = "server.test";
    private static final String PASSWORD = "secret";
    private static final Pattern PAYLOAD = Pattern.compile("<(?:\\w+:)?arg0>(x*)</(?:\\w+:)?arg0>");
    static final List<HttpVersion> DROPPED_REQUESTS = new CopyOnWriteArrayList<>();

    // Lifecycle-managed resources are initialized in start() and released in stop().
    private Vertx vertx;

    @Override
    public Map<String, String> start() {
        try {
            Path directory = Path.of("target/http3-certs").toAbsolutePath();
            Files.createDirectories(directory);
            new CertificateGenerator(directory, true).generate(new CertificateRequest()
                    .withName(HOST).withCN(HOST).withSubjectAlternativeName(HOST)
                    .withFormat(Format.PKCS12).withPassword(PASSWORD).withDuration(Duration.ofDays(2)));
            Path hosts = directory.resolve("hosts");
            Files.writeString(hosts, "127.0.0.1 localhost server.test wrong.test\n");
            vertx = Vertx.vertx(new VertxOptions().setEventLoopPoolSize(2));
            ServerSSLOptions ssl = new ServerSSLOptions().setUseAlpn(true).setSni(true)
                    .setKeyCertOptions(new PfxOptions().setPath(directory.resolve(HOST + "-keystore.p12").toString())
                            .setPassword(PASSWORD));
            HttpServer quic = startServer(ssl, -1, HttpVersion.HTTP_3);
            HttpServer tcp = startServer(ssl, quic.actualPort(), HttpVersion.HTTP_2, HttpVersion.HTTP_1_1);
            HttpServer mutual = startServer(new ServerSSLOptions(ssl).setClientAuth(ClientAuth.REQUIRED)
                    .setTrustOptions(new PfxOptions().setPath(directory.resolve(HOST + "-truststore.p12").toString())
                            .setPassword(PASSWORD)),
                    -1, HttpVersion.HTTP_3);
            Map<String, String> properties = new HashMap<>();
            properties.put("quarkus.vertx.resolver.hosts-path", hosts.toString());
            properties.put("quarkus.tls.http3.trust-store.p12.path", directory.resolve(HOST + "-truststore.p12").toString());
            properties.put("quarkus.tls.http3.trust-store.p12.password", PASSWORD);
            properties.put("quarkus.tls.http3-server.key-store.p12.path", directory.resolve(HOST + "-keystore.p12").toString());
            properties.put("quarkus.tls.http3-server.key-store.p12.password", PASSWORD);
            properties.put("quarkus.tls.mutual.trust-store.p12.path", directory.resolve(HOST + "-truststore.p12").toString());
            properties.put("quarkus.tls.mutual.trust-store.p12.password", PASSWORD);
            properties.put("quarkus.tls.mutual.key-store.p12.path", directory.resolve(HOST + "-keystore.p12").toString());
            properties.put("quarkus.tls.mutual.key-store.p12.password", PASSWORD);
            for (String name : List.of("direct", "discovery", "discoveryHttp1", "disabled", "noAdvertisement", "tcpOnly",
                    "untrusted",
                    "wrongHost", "cleartext", "redirect", "broken", "mutual", "missingClientCertificate")) {
                String prefix = "quarkus.cxf.client." + name;
                int port = switch (name) {
                    case "direct", "untrusted", "wrongHost", "cleartext", "redirect" -> quic.actualPort();
                    case "mutual", "missingClientCertificate" -> mutual.actualPort();
                    default -> tcp.actualPort();
                };
                String host = name.equals("wrongHost") ? "wrong.test" : HOST;
                String scheme = name.equals("cleartext") ? "http" : "https";
                String path = switch (name) {
                    case "noAdvertisement" -> "/plain";
                    case "redirect" -> "/redirect";
                    default -> "/inspect";
                };
                properties.put(prefix + ".client-endpoint-url", scheme + "://" + host + ":" + port + path);
                properties.put(prefix + ".service-interface", VersionService.class.getName());
                properties.put(prefix + ".connection-timeout", "3000");
                properties.put(prefix + ".receive-timeout", "5000");
                properties.put(prefix + ".chunking-threshold", "1024");
                if (!name.equals("untrusted")) {
                    properties.put(prefix + ".tls-configuration-name", name.equals("mutual") ? "mutual" : "http3");
                }
            }
            return Map.copyOf(properties);
        } catch (Exception e) {
            stop();
            throw new IllegalStateException("Could not start the HTTP/3 test servers", e);
        }
    }

    private HttpServer startServer(ServerSSLOptions ssl, int alternativePort, HttpVersion... versions) throws Exception {
        HttpServerConfig config = new HttpServerConfig().setHost("127.0.0.1").setPort(0).setVersions(versions);
        config.getTcpConfig().setSsl(true);
        return vertx.httpServerBuilder().with(config).with(ssl).build().requestHandler(request -> request.bodyHandler(body -> {
            if (request.path().equals("/drop")) {
                DROPPED_REQUESTS.add(request.version());
                request.connection().close();
                return;
            }
            if (request.path().equals("/redirect")) {
                request.response().setStatusCode(307).putHeader("Location", "http://" + HOST + ":80/inspect").end();
                return;
            }
            request.response().putHeader("Content-Type", "text/xml; charset=UTF-8");
            if (alternativePort > 0 && !request.path().equals("/plain")) {
                // Vert.x sends an Alt-Svc header over HTTP/1.1 and an ALTSVC frame over HTTP/2.
                request.response().writeAltSvc("h3=\":" + alternativePort + "\"; ma=60");
            }
            boolean clean = List.of("Connection", "X-Hop", "Keep-Alive", "Proxy-Connection", "Transfer-Encoding", "Upgrade")
                    .stream().noneMatch(request.headers()::contains);
            var payload = PAYLOAD.matcher(body.toString());
            int size = payload.find() ? payload.group(1).length() : 0;
            String result = request.version() + "|" + clean + "|" + request.connection().indicatedServerName() + "|" + size;
            request.response().end("""
                    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                        <soap:Body>
                            <ns:inspectResponse xmlns:ns="https://quarkiverse.io/cxf/http3">
                                <return>%s</return>
                            </ns:inspectResponse>
                        </soap:Body>
                    </soap:Envelope>
                    """.formatted(result));
        })).listen().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        if (vertx != null) {
            vertx.close().toCompletionStage().toCompletableFuture().join();
        }
    }
}
