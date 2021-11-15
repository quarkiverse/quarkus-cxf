package io.quarkiverse.cxf.transport;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Based on AbstractHttpServlet#HttpServletRequestXForwardedFilter
 * https://github.com/apache/cxf/blob/cxf-3.4.5/rt/transports/http/src/main/java/org/apache/cxf/transport/servlet/AbstractHTTPServlet.java#L462
 */
public class VertxHttpServletRequestXForwardedFilter extends HttpServletRequestWrapper {

    private String newProtocol;
    private String newRemoteAddr;

    private String newContextPath;
    private String newServletPath;
    private String newRequestUri;
    private StringBuffer newRequestUrl;

    VertxHttpServletRequestXForwardedFilter(HttpServletRequest request,
            String originalProto,
            String originalRemoteAddr,
            String originalPrefix,
            String originalHost,
            String originalPort) {
        super(request);
        this.newProtocol = originalProto;
        if (originalRemoteAddr != null) {
            newRemoteAddr = (originalRemoteAddr.split(",")[0]).trim();
        }
        newRequestUri = calculateNewRequestUri(request, originalPrefix);
        // Although per Mozilla documentation
        // (https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-Host)
        // it should contain one value, Apache's mod_proxy says the comma separated list could
        // be returned (http://httpd.apache.org/docs/2.2/mod/mod_proxy.html). We don't need
        // more than 2 components.
        String outermostHost = originalHost != null ? (originalHost.split(",", 2)[0]).trim() : originalHost;
        newRequestUrl = calculateNewRequestUrl(request,
                originalProto,
                originalPrefix,
                outermostHost,
                originalPort);
        newContextPath = calculateNewContextPath(request, originalPrefix);
        newServletPath = calculateNewServletPath(request, originalPrefix);
    }

    private static String calculateNewContextPath(HttpServletRequest request, String originalPrefix) {
        if (originalPrefix != null) {
            return originalPrefix;
        } else {
            return request.getContextPath();
        }
    }

    private static String calculateNewServletPath(HttpServletRequest request, String originalPrefix) {
        String servletPath = request.getServletPath();
        if (originalPrefix != null) {
            servletPath = request.getContextPath() + servletPath;
        }
        return servletPath;
    }

    private static String calculateNewRequestUri(HttpServletRequest request, String originalPrefix) {
        String requestUri = request.getRequestURI();
        if (originalPrefix != null) {
            requestUri = originalPrefix + requestUri;
        }
        return requestUri;
    }

    private static StringBuffer calculateNewRequestUrl(HttpServletRequest request,
            String originalProto,
            String originalPrefix,
            String originalHost,
            String originalPort) {
        URI uri = URI.create(request.getRequestURL().toString());

        StringBuffer sb = new StringBuffer();

        sb.append(originalProto != null ? originalProto : uri.getScheme())
                .append("://")
                .append(originalHost != null ? originalHost : uri.getHost())
                .append(originalPort != null && !"-1".equals(originalPort)
                        ? ":" + originalPort
                        : uri.getPort() != -1 ? ":" + uri.getPort() : "")
                .append(originalPrefix != null ? originalPrefix : "")
                .append(uri.getRawPath());

        String query = uri.getRawQuery();
        if (query != null) {
            sb.append('?').append(query);
        }

        return sb;
    }

    @Override
    public boolean isSecure() {
        if (newProtocol != null) {
            return "https".equals(newProtocol);
        }
        return super.isSecure();
    }

    @Override
    public StringBuffer getRequestURL() {
        return newRequestUrl;
    }

    @Override
    public String getRemoteAddr() {
        if (newRemoteAddr != null) {
            return newRemoteAddr;
        }
        return super.getRemoteAddr();
    }

    @Override
    public String getRequestURI() {
        return newRequestUri;
    }

    @Override
    public String getContextPath() {
        return newContextPath;
    }

    @Override
    public String getServletPath() {
        return newServletPath;
    }

}
