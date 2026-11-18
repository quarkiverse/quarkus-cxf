package io.quarkiverse.cxf.it.versions;

import jakarta.annotation.Resource;
import jakarta.jws.WebService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.handler.MessageContext;

@WebService(serviceName = "VersionEcho", targetNamespace = "urn:version-echo")
public class VersionEchoImpl implements VersionEcho {
    @Resource
    WebServiceContext context;

    @Override
    public String echo(String body) {
        final HttpServletRequest request = (HttpServletRequest) context.getMessageContext()
                .get(MessageContext.SERVLET_REQUEST);
        return request.getProtocol() + ":" + body.length();
    }
}
