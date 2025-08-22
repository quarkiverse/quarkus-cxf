package io.quarkiverse.cxf.it.extensors;

import jakarta.jws.WebService;

import io.quarkiverse.cxf.annotation.CXFEndpoint;
import io.quarkiverse.cxf.it.server.extensors.model.ExtensorsService;

@WebService(targetNamespace = "http://test.deployment.cxf.quarkiverse.io/", name = "ExtensorsService")
@CXFEndpoint("/ExtensorsService")
public class ExtensorsServiceImpl implements ExtensorsService {

    @Override
    public String hello(String name) {
        return "Hello " + name;
    }

}
