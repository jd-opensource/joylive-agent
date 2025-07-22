/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.plugin.transmission.servlet.javax.request;

import com.jd.live.agent.core.util.CollectionUtils;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.core.util.map.MultiMap;
import com.jd.live.agent.governance.request.HeaderProvider;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * A wrapper class for HttpServletRequest that delegates all method calls to the underlying request object.
 * This class supports Zuul by inheriting from ServletRequestWrapper while implementing HttpServletRequest
 * and HeaderProvider interfaces.
 */
public class JavaxRequest extends ServletRequestWrapper implements HttpServletRequest, HeaderProvider {

    // HTTP date formats
    private static final String DATE_RFC5322 = "EEE, dd MMM yyyy HH:mm:ss z";
    private static final String DATE_OBSOLETE_RFC850 = "EEEEE, dd-MMM-yy HH:mm:ss zzz";
    private static final String DATE_OBSOLETE_ASCTIME = "EEE MMMM d HH:mm:ss yyyy";

    private static final ZoneId GMT_ZONE = TimeZone.getTimeZone("GMT").toZoneId();
    private static final DateTimeFormatter[] DATE_FORMATS = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern(DATE_RFC5322, Locale.US).withZone(GMT_ZONE),
            DateTimeFormatter.ofPattern(DATE_OBSOLETE_RFC850, Locale.US).withZone(GMT_ZONE),
            DateTimeFormatter.ofPattern(DATE_OBSOLETE_ASCTIME, Locale.US).withZone(GMT_ZONE)
    };

    private String queryString;

    private String pathInfo;

    private String scheme;

    private String protocol;

    private String contentType;

    private String requestURI;

    private String contextPath;

    private MultiMap<String, String> headers;

    public JavaxRequest(HttpServletRequest request) {
        super(request);
    }

    /**
     * Retrieves the original HttpServletRequest from the current request.
     * <p>
     * This method traverses through any ServletRequestWrapper instances
     * to find the original request object.
     * </p>
     *
     * @return HttpServletRequest
     */
    private HttpServletRequest getOriginalRequest() {
        ServletRequest req = super.getRequest();
        while (req instanceof ServletRequestWrapper) {
            req = ((ServletRequestWrapper) req).getRequest();
        }
        return (HttpServletRequest) req;
    }

    @Override
    public String getAuthType() {
        return getOriginalRequest().getAuthType();
    }

    @Override
    public Cookie[] getCookies() {
        return getOriginalRequest().getCookies();
    }

    @Override
    public long getDateHeader(String name) {
        String value = this.getHeader(name);
        if (value == null) {
            return -1L;
        } else {
            long result = -1;
            for (int i = 0; (result == -1) && (i < DATE_FORMATS.length); i++) {
                result = LocalDateTime.parse(value, DATE_FORMATS[i]).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            if (result != -1L) {
                return result;
            } else {
                throw new IllegalArgumentException(value);
            }
        }
    }

    @Override
    public String getHeader(String name) {
        MultiMap<String, String> headers = getHeaders();
        return headers == null ? null : headers.getFirst(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        MultiMap<String, String> headers = getHeaders();
        return CollectionUtils.toEnumeration(headers == null ? null : headers.get(name));
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        MultiMap<String, String> headers = getHeaders();
        return CollectionUtils.toEnumeration(headers == null ? null : headers.keySet());
    }

    @Override
    public int getIntHeader(String name) {
        String value = this.getHeader(name);
        return value == null ? -1 : Integer.parseInt(value);
    }

    @Override
    public String getMethod() {
        return getOriginalRequest().getMethod();
    }

    @Override
    public String getPathInfo() {
        if (pathInfo == null) {
            pathInfo = getOriginalRequest().getPathInfo();
        }
        return pathInfo;
    }

    @Override
    public String getPathTranslated() {
        return getOriginalRequest().getPathTranslated();
    }

    @Override
    public String getContextPath() {
        if (contextPath == null) {
            contextPath = getOriginalRequest().getContextPath();
        }
        return contextPath;
    }

    @Override
    public String getQueryString() {
        if (queryString == null) {
            queryString = getOriginalRequest().getQueryString();
        }
        return queryString;
    }

    @Override
    public String getRemoteUser() {
        return getOriginalRequest().getRemoteUser();
    }

    @Override
    public boolean isUserInRole(String role) {
        return getOriginalRequest().isUserInRole(role);
    }

    @Override
    public Principal getUserPrincipal() {
        return getOriginalRequest().getUserPrincipal();
    }

    @Override
    public String getRequestedSessionId() {
        return getOriginalRequest().getRequestedSessionId();
    }

    @Override
    public String getRequestURI() {
        if (requestURI == null) {
            requestURI = getOriginalRequest().getRequestURI();
        }
        return requestURI;
    }

    @Override
    public StringBuffer getRequestURL() {
        return getOriginalRequest().getRequestURL();
    }

    @Override
    public String getServletPath() {
        return getOriginalRequest().getServletPath();
    }

    @Override
    public HttpSession getSession(boolean create) {
        return getOriginalRequest().getSession(create);
    }

    @Override
    public HttpSession getSession() {
        return getOriginalRequest().getSession();
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return getOriginalRequest().isRequestedSessionIdValid();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return getOriginalRequest().isRequestedSessionIdFromCookie();
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return getOriginalRequest().isRequestedSessionIdFromURL();
    }

    @Override
    @Deprecated
    public boolean isRequestedSessionIdFromUrl() {
        return getOriginalRequest().isRequestedSessionIdFromUrl();
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        return getOriginalRequest().authenticate(response);
    }

    @Override
    public void login(String username, String password) throws ServletException {
        getOriginalRequest().login(username, password);
    }

    @Override
    public void logout() throws ServletException {
        getOriginalRequest().logout();
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return getOriginalRequest().getParts();
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        return getOriginalRequest().getPart(name);
    }

    @Override
    public Object getAttribute(String name) {
        return getOriginalRequest().getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return getOriginalRequest().getAttributeNames();
    }

    @Override
    public String getCharacterEncoding() {
        return getOriginalRequest().getCharacterEncoding();
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        getOriginalRequest().setCharacterEncoding(env);
    }

    @Override
    public int getContentLength() {
        return getOriginalRequest().getContentLength();
    }

    @Override
    public long getContentLengthLong() {
        return getOriginalRequest().getContentLengthLong();
    }

    @Override
    public String getContentType() {
        if (contentType == null) {
            contentType = getOriginalRequest().getContentType();
        }
        return contentType;
    }

    @Override
    public String changeSessionId() {
        return getOriginalRequest().changeSessionId();
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        return getOriginalRequest().upgrade(handlerClass);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return getOriginalRequest().getInputStream();
    }

    @Override
    public String getParameter(String name) {
        return getOriginalRequest().getParameter(name);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return getOriginalRequest().getParameterNames();
    }

    @Override
    public String[] getParameterValues(String name) {
        return getOriginalRequest().getParameterValues(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return getOriginalRequest().getParameterMap();
    }

    @Override
    public String getProtocol() {
        if (protocol == null) {
            protocol = getOriginalRequest().getProtocol();
        }
        return protocol;
    }

    @Override
    public String getScheme() {
        if (scheme == null) {
            scheme = getOriginalRequest().getScheme();
        }
        return scheme;
    }

    @Override
    public String getServerName() {
        return getOriginalRequest().getServerName();
    }

    @Override
    public int getServerPort() {
        return getOriginalRequest().getServerPort();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return getOriginalRequest().getReader();
    }

    @Override
    public String getRemoteAddr() {
        return getOriginalRequest().getRemoteAddr();
    }

    @Override
    public String getRemoteHost() {
        return getOriginalRequest().getRemoteHost();
    }

    @Override
    public void setAttribute(String name, Object o) {
        getOriginalRequest().setAttribute(name, o);
    }

    @Override
    public void removeAttribute(String name) {
        getOriginalRequest().removeAttribute(name);
    }

    @Override
    public Locale getLocale() {
        return getOriginalRequest().getLocale();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return getOriginalRequest().getLocales();
    }

    @Override
    public boolean isSecure() {
        return getOriginalRequest().isSecure();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return getOriginalRequest().getRequestDispatcher(path);
    }

    @Override
    @Deprecated
    public String getRealPath(String path) {
        return getOriginalRequest().getRealPath(path);
    }

    @Override
    public int getRemotePort() {
        return getOriginalRequest().getRemotePort();
    }

    @Override
    public String getLocalName() {
        return getOriginalRequest().getLocalName();
    }

    @Override
    public String getLocalAddr() {
        return getOriginalRequest().getLocalAddr();
    }

    @Override
    public int getLocalPort() {
        return getOriginalRequest().getLocalPort();
    }

    @Override
    public ServletContext getServletContext() {
        return getOriginalRequest().getServletContext();
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return getOriginalRequest().startAsync();
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return getOriginalRequest().startAsync(servletRequest, servletResponse);
    }

    @Override
    public boolean isAsyncStarted() {
        return getOriginalRequest().isAsyncStarted();
    }

    @Override
    public boolean isAsyncSupported() {
        return getOriginalRequest().isAsyncSupported();
    }

    @Override
    public AsyncContext getAsyncContext() {
        return getOriginalRequest().getAsyncContext();
    }

    @Override
    public DispatcherType getDispatcherType() {
        return getOriginalRequest().getDispatcherType();
    }


    @Override
    public MultiMap<String, String> getHeaders() {
        if (headers == null) {
            HttpServletRequest originalRequest = getOriginalRequest();
            // direct access the underlying
            // org.apache.catalina.connector.RequestFacade
            // org.apache.catalina.connector.Request
            HttpHeaderParser parser = HttpHeaderParsers.create(originalRequest.getClass());
            headers = parser != null
                    ? parser.parse(originalRequest)
                    : HttpUtils.parseHeader(originalRequest.getHeaderNames(), originalRequest::getHeaders);
        }
        return headers;
    }

    /**
     * Replaces the HttpServletRequest object at the specified index in the arguments array with a JavaxRequest wrapper,
     * if it is not already a JavaxRequest instance.
     *
     * @param arguments the array of arguments
     * @param index     the index of the HttpServletRequest object to replace
     * @return the replaced HttpServletRequest object
     */
    public static HttpServletRequest replace(final Object[] arguments, final int index) {
        HttpServletRequest hsr = (HttpServletRequest) arguments[index];
        if (hsr instanceof HeaderProvider) {
            return hsr;
        } else {
            Object request = hsr;
            while (request instanceof ServletRequestWrapper) {
                request = ((HttpServletRequestWrapper) request).getRequest();
                if (request instanceof HeaderProvider) {
                    return hsr;
                }
            }
        }
        arguments[index] = new JavaxRequest(hsr);
        return hsr;
    }

}