package io.quarkiverse.cxf.transport;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class VertxHttpServletResponse implements HttpServletResponse {
    protected final RoutingContext context;
    private final HttpServerRequest request;
    protected final HttpServerResponse response;
    private VertxServletOutputStream os;
    private PrintWriter printWriter;

    public VertxHttpServletResponse(RoutingContext context) {
        this.request = context.request();
        this.response = context.response();
        this.context = context;
        this.os = new VertxServletOutputStream(request, response);
    }

    @Override
    public void addCookie(Cookie cookie) {

    }

    @Override
    public boolean containsHeader(String name) {
        return response.headers().contains(name);
    }

    @Override
    public String encodeURL(String url) {
        return null;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return null;
    }

    @Override
    @Deprecated
    public String encodeUrl(String url) {
        return null;
    }

    @Override
    @Deprecated
    public String encodeRedirectUrl(String url) {
        return null;
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {

    }

    @Override
    public void sendError(int sc) throws IOException {

    }

    @Override
    public void sendRedirect(String location) throws IOException {

    }

    @Override
    public void setDateHeader(String name, long date) {

    }

    @Override
    public void addDateHeader(String name, long date) {

    }

    @Override
    public void setHeader(String name, String value) {
        response.headers().set(name, value);
    }

    @Override
    public void addHeader(String name, String value) {
        response.headers().add(name, value);
    }

    @Override
    public void setIntHeader(String name, int value) {
        response.headers().set(name, Integer.toBinaryString(value));
    }

    @Override
    public void addIntHeader(String name, int value) {
        response.headers().add(name, Integer.toBinaryString(value));
    }

    @Override
    public void setStatus(int sc) {
        response.setStatusCode(sc);
    }

    @Override
    @Deprecated
    public void setStatus(int sc, String sm) {
        response.setStatusCode(sc);
        response.setStatusMessage(sm);
    }

    @Override
    public int getStatus() {
        return response.getStatusCode();
    }

    @Override
    public String getHeader(String name) {
        return response.headers().get(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return response.headers().getAll(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return response.headers().names();
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public String getContentType() {
        return response.headers().get("Content-Type");
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return os;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (printWriter == null) {
            printWriter = new PrintWriter(os);
        }
        return printWriter;
    }

    @Override
    public void setCharacterEncoding(String charset) {

    }

    @Override
    public void setContentLength(int len) {
        response.headers().set("Content-Length", Integer.toString(len));
    }

    @Override
    public void setContentLengthLong(long len) {
        response.headers().set("Content-Length", Long.toString(len));
    }

    @Override
    public void setContentType(String type) {
        response.headers().set("Content-Type", type);
    }

    @Override
    public void setBufferSize(int size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getBufferSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flushBuffer() throws IOException {
        if (printWriter != null) {
            printWriter.close();
        } else {
            os.close();
        }
    }

    @Override
    public void resetBuffer() {
        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
            }
        }
        os = new VertxServletOutputStream(request, response);
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {

    }

    @Override
    public void setLocale(Locale loc) {

    }

    @Override
    public Locale getLocale() {
        return null;
    }

    public void end() {
        try {
            if (printWriter != null) {
                printWriter.close();
            } else {
                os.close();
            }
        } catch (IOException e) {
        }
    }
}
