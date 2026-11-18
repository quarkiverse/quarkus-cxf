package io.quarkiverse.cxf.it.versions;

import jakarta.jws.WebService;

@WebService(targetNamespace = "urn:version-echo")
public interface VersionEcho {
    String echo(String body);
}
