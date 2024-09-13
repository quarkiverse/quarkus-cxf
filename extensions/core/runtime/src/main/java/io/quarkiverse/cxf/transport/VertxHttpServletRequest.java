package io.quarkiverse.cxf.transport;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;

import org.apache.cxf.common.util.UrlUtils;

import io.quarkiverse.cxf.transport.servlet.DateUtils;
import io.quarkiverse.cxf.transport.servlet.LocaleUtils;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.VertxInputStream;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;

public class VertxHttpServletRequest implements HttpServletRequest {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final String SSL_CIPHER_SUITE_ATTRIBUTE = "jakarta.servlet.request.cipher_suite";
    private static final String SSL_PEER_CERT_CHAIN_ATTRIBUTE = "jakarta.servlet.request.X509Certificate";

    private final RoutingContext context;
    private final VertxInputStream in;
    private final HttpServerRequest request;
    private final String contextPath;
    private final String servletPath;
    private final Map<String, Object> attributes;
    private Cookie[] cookies;
    private String characterEncoding;

    public VertxHttpServletRequest(RoutingContext context, String contextPath, String servletPath) {
        this.request = context.request();
        this.contextPath = contextPath;
        this.servletPath = servletPath;
        this.attributes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.context = context;
        in = new VertxInputStream(context, 10000);

        final SSLSession sslSession = this.request.connection().sslSession();
        if (sslSession != null) {
            this.attributes.put(SSL_CIPHER_SUITE_ATTRIBUTE, sslSession.getCipherSuite());
            try {
                this.attributes.put(SSL_PEER_CERT_CHAIN_ATTRIBUTE, sslSession.getPeerCertificates());
            } catch (SSLPeerUnverifiedException e) {
                // do nothing
            }
        }

    }

    @Override
    public AsyncContext getAsyncContext() {
        throw new UnsupportedOperationException("Unsupported method " + getClass().getName() + ".getAsyncContext()");
    }

    @Override
    public DispatcherType getDispatcherType() {
        throw new UnsupportedOperationException("Unsupported method " + getClass().getName() + ".getDispatcherType()");
    }

    @Override
    public String getRequestId() {
        throw new UnsupportedOperationException("Unsupported method " + getClass().getName() + ".getRequestId()");
    }

    @Override
    public String getProtocolRequestId() {
        throw new UnsupportedOperationException("Unsupported method " + getClass().getName() + ".getProtocolRequestId()");
    }

    @Override
    public ServletConnection getServletConnection() {
        throw new UnsupportedOperationException("Unsupported method " + getClass().getName() + ".getServletConnection()");
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return attributes.isEmpty() ? Collections.emptyEnumeration() : Collections.enumeration(attributes.keySet());
    }

    @Override
    public String getCharacterEncoding() {
        if (characterEncoding == null) {
            characterEncoding = getCharacterEncodingFromHeader();
        }
        return characterEncoding;
    }

    @Override
    public int getContentLength() {
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
                throw new UnsupportedOperationException(getClass().getName() + ".setReadListener(ReadListener)");
            }
        };

    }

    @Override
    public String getLocalAddr() {
        SocketAddress adr = request.localAddress();
        if (adr != null) {
            return adr.hostAddress();
        }
        return null;
    }

    @Override
    public String getLocalName() {
        SocketAddress adr = request.localAddress();
        if (adr != null) {
            return adr.host();
        }
        return null;
    }

    @Override
    public int getLocalPort() {
        SocketAddress adr = request.localAddress();
        if (adr != null) {
            return adr.port();
        }
        return -1;
    }

    @Override
    public Locale getLocale() {
        return getLocales().nextElement();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        final List<String> acceptLanguage = request.headers().getAll("Accept-Language");
        List<Locale> ret = LocaleUtils.getLocalesFromHeader(acceptLanguage);
        if (ret.isEmpty()) {
            return Collections.enumeration(Collections.singletonList(Locale.getDefault()));
        }
        return Collections.enumeration(ret);
    }

    @Override
    public String getParameter(String name) {
        return request.getParam(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        MultiMap params = request.params();
        if (params.isEmpty()) {
            return Collections.emptyMap();
        }
        LinkedHashMap<String, String[]> result = new LinkedHashMap<>();
        for (String key : params.names()) {
            result.put(key, getParameterValues(key));
        }
        return result;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(request.params().names());
    }

    @Override
    public String[] getParameterValues(String name) {
        List<String> list = request.params().getAll(name);
        if (list != null) {
            return (String[]) list.toArray();
        }
        return EMPTY_STRING_ARRAY;
    }

    @Override
    public String getProtocol() {
        return request.version().alpnName();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        final Charset charset = /* getCharacterEncoding() != null ? Charset.forName(characterEncoding) : */ StandardCharsets.UTF_8;
        return new BufferedReader(new InputStreamReader(in, charset));
    }

    @Override
    public String getRemoteAddr() {
        final SocketAddress remoteAddress = request.remoteAddress();
        if (remoteAddress != null) {
            return remoteAddress.hostAddress();
        }
        return null;
    }

    @Override
    public String getRemoteHost() {
        final SocketAddress remoteAddress = request.remoteAddress();
        if (remoteAddress != null) {
            return remoteAddress.host();
        }
        return null;
    }

    @Override
    public int getRemotePort() {
        final SocketAddress remoteAddress = request.remoteAddress();
        if (remoteAddress != null) {
            return remoteAddress.port();
        }
        return -1;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        throw new UnsupportedOperationException("Unsupported method " + getClass().getName() + ".getRequestDispatcher(String)");
    }

    @Override
    public String getScheme() {
        return request.scheme();
    }

    @Override
    public String getServerName() {
        return request.authority().host();
    }

    @Override
    public int getServerPort() {
        return request.authority().port();
    }

    @Override
    public ServletContext getServletContext() {
        //        if (servletContext == null) {
        //            servletContext = new VertxServletContext(contextPath);
        //        }
        //        return servletContext;
        throw new UnsupportedOperationException("Unsupported method " + getClass().getName() + ".getServletContext()");
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public boolean isSecure() {
        return request.isSSL();
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public void setAttribute(String name, Object o) {
        attributes.put(name, o);
    }

    private String getCharacterEncodingFromHeader() {
        String contentType = request.getHeader("Content-Type");
        if (contentType == null) {
            return null;
        }
        return extractQuotedValueFromHeader(contentType, "charset");
    }

    /**
     * Extracts a quoted value from a header that has a given key. For instance if the header is
     * <p>
     * Copied from io.undertow.httpcore.HttpHeaderNames.extractQuotedValueFromHeader(String, String)
     * <p>
     * content-disposition=form-data; name="my field"
     * and the key is name then "my field" will be returned without the quotes.
     *
     *
     * @param header The header
     * @param key The key that identifies the token to extract
     * @return The token, or null if it was not found
     */
    static String extractQuotedValueFromHeader(final String header, final String key) {

        int keypos = 0;
        int pos = -1;
        boolean whiteSpace = true;
        boolean inQuotes = false;
        for (int i = 0; i < header.length() - 1; ++i) { // -1 because we need room for the = at the end
            // TODO: a more efficient matching algorithm
            char c = header.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    inQuotes = false;
                }
            } else {
                if (key.charAt(keypos) == c && (whiteSpace || keypos > 0)) {
                    keypos++;
                    whiteSpace = false;
                } else if (c == '"') {
                    keypos = 0;
                    inQuotes = true;
                    whiteSpace = false;
                } else {
                    keypos = 0;
                    whiteSpace = c == ' ' || c == ';' || c == '\t';
                }
                if (keypos == key.length()) {
                    if (header.charAt(i + 1) == '=') {
                        pos = i + 2;
                        break;
                    } else {
                        keypos = 0;
                    }
                }
            }

        }
        if (pos == -1) {
            return null;
        }

        int end;
        int start = pos;
        if (header.charAt(start) == '"') {
            start++;
            for (end = start; end < header.length(); ++end) {
                char c = header.charAt(end);
                if (c == '"') {
                    break;
                }
            }
            return header.substring(start, end);

        } else {
            // no quotes
            for (end = start; end < header.length(); ++end) {
                char c = header.charAt(end);
                if (c == ' ' || c == '\t' || c == ';') {
                    break;
                }
            }
            return header.substring(start, end);
        }
    }

    @Override
    public void setCharacterEncoding(final String env) throws UnsupportedEncodingException {
        throw new UnsupportedOperationException("Unsupported method " + getClass().getName() + ".setCharacterEncoding(String)");
    }

    @Override
    public AsyncContext startAsync() {
        throw new UnsupportedOperationException("Unsupported method " + getClass().getName() + ".startAsync()");
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) {
        throw new UnsupportedOperationException(
                "Unsupported method " + getClass().getName() + ".startAsync(ServletRequest, ServletResponse)");
    }

    @Override
    public boolean authenticate(HttpServletResponse servletResponse) throws IOException, ServletException {
        throw new UnsupportedOperationException(
                "Unsupported method " + getClass().getName() + ".authenticate(HttpServletResponse)");
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
        if (cookies == null) {
            Set<io.vertx.core.http.Cookie> vertxCookies = request.cookies();
            if (vertxCookies.isEmpty()) {
                return null;
            }
            int count = vertxCookies.size();
            Cookie[] value = new Cookie[count];
            int i = 0;
            for (io.vertx.core.http.Cookie cookie : vertxCookies) {
                try {
                    Cookie c = new Cookie(cookie.getName(), cookie.getValue());
                    if (cookie.getDomain() != null) {
                        c.setDomain(cookie.getDomain());
                    }
                    c.setHttpOnly(cookie.isHttpOnly());
                    if (cookie.getMaxAge() >= 0) {
                        c.setMaxAge((int) cookie.getMaxAge());
                    }
                    if (cookie.getPath() != null) {
                        c.setPath(cookie.getPath());
                    }
                    c.setSecure(cookie.isSecure());
                    value[i++] = c;
                } catch (IllegalArgumentException e) {
                    // Ignore bad cookie
                }
            }
            if (i < count) {
                Cookie[] shrunkCookies = new Cookie[i];
                System.arraycopy(value, 0, shrunkCookies, 0, i);
                value = shrunkCookies;
            }
            this.cookies = value;
        }
        return cookies;
    }

    @Override
    public long getDateHeader(String name) {
        String header = request.getHeader(name);
        if (header == null) {
            return -1;
        }
        Date date = DateUtils.parseDate(header);
        if (date == null) {
            throw new IllegalArgumentException(String.format("Header %s cannot be converted to a date", header));
        }
        return date.getTime();
    }

    @Override
    public String getHeader(String name) {
        return request.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(request.headers().names());
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (request.headers().contains(name)) {
            return Collections.enumeration(request.headers().getAll(name));
        }
        return Collections.emptyEnumeration();
    }

    @Override
    public int getIntHeader(String name) {
        String v = getHeader(name);
        return v == null ? -1 : Integer.parseInt(v);
    }

    @Override
    public String getMethod() {
        return request.method().name();
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        throw new UnsupportedOperationException("Unsupported method " + getClass().getName() + ".getPart(String)");
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        throw new UnsupportedOperationException("Unsupported method " + getClass().getName() + ".getParts()");
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
        /*
         * From jakarta.servlet.ServletContext.getRealPath(String) Javadoc:
         * This method returns <code>null</code> if the servlet container is unable to translate the given
         * <i>virtual</i> path to a <i>real</i> path.
         *
         * We always return null, because our dummy Servlet implementation is not supposed to be used for serving
         * static resources.
         */
        return null;
    }

    @Override
    public String getQueryString() {
        return request.query();
    }

    @Override
    public String getRemoteUser() {
        Principal userPrincipal = getUserPrincipal();
        return userPrincipal != null ? userPrincipal.getName() : null;
    }

    @Override
    public String getRequestURI() {
        return request.uri();
    }

    @Override
    public StringBuffer getRequestURL() {
        String url = request.absoluteURI();
        int index = url.indexOf("?");
        if (index > -1) {
            url = url.substring(0, index);
        }
        return new StringBuffer(url);
    }

    @Override
    public String getRequestedSessionId() {
        /*
         * We do not support sessions as SOAP Services should typically be stateless.
         * We cannot throw UnsupportedOperationException, because org.apache.cxf.transport.http.HttpServletRequestSnapshot
         * is calling this method to make a copy/snapshot of the original request
         */
        return null;
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    @Override
    public HttpSession getSession(boolean create) {
        if (!create) {
            return null;
        }
        throw new UnsupportedOperationException("Unsupported method " + getClass().getName() + ".getSession(boolean)");
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
        throw new UnsupportedOperationException(
                "Unsupported method " + getClass().getName() + ".isRequestedSessionIdFromCookie()");
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        throw new UnsupportedOperationException(
                "Unsupported method " + getClass().getName() + ".isRequestedSessionIdFromURL()");
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        /*
         * We do not support sessions as SOAP Services should typically be stateless.
         * We cannot throw UnsupportedOperationException, because org.apache.cxf.transport.http.HttpServletRequestSnapshot
         * is calling this method to make a copy/snapshot of the original request
         */
        return false;
    }

    @Override
    public boolean isUserInRole(String role) {
        if (role == null) {
            return false;
        }
        // according to the servlet spec this aways returns false
        if (role.equals("*")) {
            return false;
        }
        SecurityIdentity user = CurrentIdentityAssociation.current();
        if (role.equals("**")) {
            return !user.isAnonymous();
        }
        return user.hasRole(role);
    }

    @Override
    public void login(String username, String password) throws ServletException {
        throw new UnsupportedOperationException("Unsupported method " + getClass().getName() + ".login(String, String)");
    }

    @Override
    public void logout() throws ServletException {
        throw new UnsupportedOperationException("Unsupported method " + getClass().getName() + ".logout()");
    }

    @Override
    public long getContentLengthLong() {
        String v = getHeader("Content-Length");
        return v == null ? -1 : Long.parseLong(v);
    }

    @Override
    public String changeSessionId() {
        throw new UnsupportedOperationException("Unsupported method " + getClass().getName() + ".changeSessionId()");
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> arg0) throws IOException, ServletException {
        throw new UnsupportedOperationException("Unsupported method " + getClass().getName() + ".upgrade(Class<T>)");
    }

}
