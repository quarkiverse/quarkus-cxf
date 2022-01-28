package io.quarkiverse.cxf.transport;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ReadListener;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.apache.cxf.common.util.UrlUtils;
import org.jboss.logging.Logger;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.VertxInputStream;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class VertxHttpServletRequest implements HttpServletRequest {
    private static final Logger LOG = Logger.getLogger(VertxHttpServletRequest.class);
    protected final RoutingContext context;
    private final VertxInputStream in;
    private final HttpServerRequest request;
    protected final HttpServerResponse response;
    private final String contextPath;
    private final String servletPath;
    private final Map<String, Object> attributes;

    public VertxHttpServletRequest(RoutingContext context, String contextPath, String servletPath) {
        this.request = context.request();
        this.response = context.response();
        this.contextPath = contextPath;
        this.servletPath = servletPath;
        this.attributes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.context = context;
        in = new VertxInputStream(context, 10000);
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        LOG.trace("getDispatcherType()");
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public String getCharacterEncoding() {
        LOG.trace("getCharacterEncoding()");
        return null;
    }

    @Override
    public int getContentLength() {
        LOG.trace("getContentLength()");
        return getIntHeader("Content-Length");
    }

    @Override
    public String getContentType() {
        return request.getHeader("Content-Type");
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return in.read();
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return in.read(b, off, len);
            }

            @Override
            public boolean isFinished() {
                try {
                    return (in.available() == -1);
                } catch (IOException e) {
                    // when closed it is finished
                    return true;
                }
            }

            @Override
            public boolean isReady() {
                // not closed or not finished
                return !isFinished();
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                throw new UnsupportedOperationException();
            }
        };

    }

    @Override
    public String getLocalAddr() {
        LOG.trace("getLocalAddr()");
        try {
            return new URL(getRequestURI()).getHost();
        } catch (MalformedURLException e) {
            LOG.trace("getLocalAddr error", e);
            return null;
        }
    }

    @Override
    public String getLocalName() {
        LOG.trace("getLocalName()");
        try {
            return new URL(request.absoluteURI()).getHost();
        } catch (MalformedURLException e) {
            LOG.trace("getLocalName error", e);
            return null;
        }
    }

    @Override
    public int getLocalPort() {
        LOG.trace("getLocalPort()");
        try {
            return new URL(request.absoluteURI()).getPort();
        } catch (MalformedURLException e) {
            LOG.trace("getLocalPort error", e);
            return 0;
        }
    }

    @Override
    public Locale getLocale() {
        LOG.trace("getLocale()");
        return null;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        LOG.trace("getLocales()");
        return null;
    }

    @Override
    public String getParameter(String name) {
        if (LOG.isTraceEnabled()) {
            LOG.tracef("getParameter({0})", name);
        }
        return null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        LOG.trace("getParameterMap()");
        return Collections.emptyMap();
    }

    @Override
    public Enumeration<String> getParameterNames() {
        LOG.trace("getParameterNames()");
        return null;
    }

    @Override
    public String[] getParameterValues(String name) {
        if (LOG.isTraceEnabled()) {
            LOG.tracef("getParameterValues({0})", name);
        }
        return new String[0];
    }

    @Override
    public String getProtocol() {
        LOG.trace("getProtocol");
        try {
            return new URL(request.absoluteURI()).getProtocol();
        } catch (MalformedURLException e) {
            LOG.trace("getProtocol error", e);
            return null;
        }
    }

    @Override
    public BufferedReader getReader() throws IOException {
        LOG.trace("getReader");
        return new BufferedReader(new InputStreamReader(in, UTF_8));
    }

    @Override
    @Deprecated
    public String getRealPath(String path) {
        LOG.trace("getRealPath");
        return null;
    }

    @Override
    public String getRemoteAddr() {
        LOG.trace("getRemoteAddr");
        return request.remoteAddress().host();
    }

    @Override
    public String getRemoteHost() {
        LOG.trace("getRemoteHost");
        return request.remoteAddress().host();
    }

    @Override
    public int getRemotePort() {
        LOG.trace("getRemotePort");
        return request.remoteAddress().port();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        LOG.trace("getRequestDispatcher");
        return null;
    }

    @Override
    public String getScheme() {
        LOG.trace("getScheme");
        return request.scheme();
    }

    @Override
    public String getServerName() {
        return getLocalName();
    }

    @Override
    public int getServerPort() {
        LOG.trace("getServerPort");
        return getLocalPort();
    }

    @Override
    public ServletContext getServletContext() {
        LOG.trace("getServletContext");
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        LOG.trace("isAsyncStarted");
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        LOG.trace("isAsyncSupported");
        return false;
    }

    @Override
    public boolean isSecure() {
        LOG.trace("isSecure");
        return request.isSSL();
    }

    @Override
    public void removeAttribute(String name) {
        LOG.trace("removeAttribute");
        attributes.remove(name);
    }

    @Override
    public void setAttribute(String name, Object o) {
        LOG.trace("setAttribute");
        attributes.put(name, o);
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        LOG.trace("setCharacterEncoding");
        // ignore as we stick to utf-8.
    }

    @Override
    public AsyncContext startAsync() {
        LOG.trace("startAsync");
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) {
        LOG.trace("startAsync");
        return null;
    }

    @Override
    public boolean authenticate(HttpServletResponse servletResponse) throws IOException, ServletException {
        LOG.trace("authenticate");
        return false;
    }

    @Override
    public String getAuthType() {
        String authorizationValue = request.getHeader("Authorization");
        if (authorizationValue == null) {
            return null;
        } else {
            return authorizationValue.split(" ")[0].trim();
        }
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public Cookie[] getCookies() {
        LOG.trace("getCookies");
        return new Cookie[0];
    }

    @Override
    public long getDateHeader(String name) {
        LOG.trace("getDateHeader");
        return 0;
    }

    @Override
    public String getHeader(String name) {
        LOG.trace("getHeader");
        return request.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        LOG.trace("getHeaderNames");
        return Collections.enumeration(request.headers().names());
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        LOG.trace("getHeaders");
        if (request.headers().contains(name)) {
            return Collections.enumeration(request.headers().getAll(name));
        }
        return Collections.enumeration(Arrays.asList());
    }

    @Override
    public int getIntHeader(String name) {
        LOG.trace("getIntHeader");
        String v = getHeader(name);
        return v == null ? -1 : Integer.parseInt(v);
    }

    @Override
    public String getMethod() {
        LOG.trace("getMethod");
        return request.method().name();
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        LOG.trace("getPart");
        return null;
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        LOG.trace("getParts");
        return Collections.emptyList();
    }

    @Override
    public String getPathInfo() {
        String path = request.path();
        if (contextPath != null && !contextPath.equals("/") && contextPath.length() < path.length()) {
            path = path.substring(contextPath.length());
        }
        if (servletPath != null && !servletPath.equals("/") && servletPath.length() < path.length()) {
            path = path.substring(servletPath.length());
        }
        return UrlUtils.urlDecode(path);
    }

    @Override
    public String getPathTranslated() {
        return null;
    }

    @Override
    public String getQueryString() {
        LOG.trace("getQueryString");
        return request.query();
    }

    @Override
    public String getRemoteUser() {
        LOG.trace("getRemoteUser");
        return null;
    }

    @Override
    public String getRequestURI() {
        if (LOG.isTraceEnabled()) {
            LOG.trace("getRequestURI " + request.uri());
        }
        return request.uri();
    }

    @Override
    public StringBuffer getRequestURL() {
        String absoluteUri = request.absoluteURI();
        String urlWithoutParams = absoluteUri.replaceFirst("\\?.*$", "");
        return new StringBuffer(urlWithoutParams);
    }

    @Override
    public String getRequestedSessionId() {
        LOG.trace("getRequestedSessionId");
        return null;
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

    @Override
    public HttpSession getSession() {
        LOG.trace("getSession");
        return null;
    }

    @Override
    public HttpSession getSession(boolean create) {
        LOG.trace("getSession");
        return null;
    }

    @Override
    public Principal getUserPrincipal() {
        QuarkusHttpUser user = (QuarkusHttpUser) context.user();
        if (user == null || user.getSecurityIdentity().isAnonymous()) {
            return null;
        }
        return user.getSecurityIdentity().getPrincipal();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        LOG.trace("isRequestedSessionIdFromCookie");
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        LOG.trace("isRequestedSessionIdFromURL");
        return false;
    }

    @Override
    @Deprecated
    public boolean isRequestedSessionIdFromUrl() {
        LOG.trace("isRequestedSessionIdFromUrl");
        return false;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        LOG.trace("isRequestedSessionIdValid");
        return false;
    }

    @Override
    public boolean isUserInRole(String role) {
        SecurityIdentity user = CurrentIdentityAssociation.current();
        if (role.equals("**")) {
            return !user.isAnonymous();
        }
        return user.hasRole(role);
    }

    @Override
    public void login(String username, String password) throws ServletException {
        LOG.trace("login");
    }

    @Override
    public void logout() throws ServletException {
        LOG.trace("logout");
    }

    @Override
    public long getContentLengthLong() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String changeSessionId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> arg0) throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }
}
