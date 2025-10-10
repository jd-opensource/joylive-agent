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
package com.jd.live.agent.plugin.transmission.servlet.jakarta.request;

import com.jd.live.agent.core.util.CollectionUtils;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.core.util.map.MultiMap;
import com.jd.live.agent.governance.request.HeaderProvider;
import com.jd.live.agent.governance.request.HeaderProviderFactory;
import com.jd.live.agent.governance.request.HeaderProviderRegistry;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * A wrapper class for HttpServletRequest that delegates all method calls to the underlying request object.
 */
public class JakartaRequest extends HttpServletRequestWrapper implements HeaderProvider {

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

    private HttpServletRequest request;

    private final HeaderProviderRegistry registry;

    private MultiMap<String, String> headers;

    public JakartaRequest(HttpServletRequest request, HeaderProviderRegistry registry) {
        super(request);
        this.request = request;
        this.registry = registry;
    }

    @Override
    public String getRequestId() {
        return request.getRequestId();
    }

    @Override
    public String getProtocolRequestId() {
        return request.getProtocolRequestId();
    }

    @Override
    public ServletConnection getServletConnection() {
        return request.getServletConnection();
    }

    @Override
    public void setCharacterEncoding(Charset encoding) {
        request.setCharacterEncoding(encoding);
    }

    @Override
    public HttpServletMapping getHttpServletMapping() {
        return request.getHttpServletMapping();
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public PushBuilder newPushBuilder() {
        return request.newPushBuilder();
    }

    @Override
    public Map<String, String> getTrailerFields() {
        return request.getTrailerFields();
    }

    @Override
    public boolean isTrailerFieldsReady() {
        return request.isTrailerFieldsReady();
    }

    @Override
    public String getAuthType() {
        return request.getAuthType();
    }

    @Override
    public Cookie[] getCookies() {
        return request.getCookies();
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
        return request.getMethod();
    }

    @Override
    public String getPathInfo() {
        return request.getPathInfo();
    }

    @Override
    public String getPathTranslated() {
        return request.getPathTranslated();
    }

    @Override
    public String getContextPath() {
        return request.getContextPath();
    }

    @Override
    public String getQueryString() {
        return request.getQueryString();
    }

    @Override
    public String getRemoteUser() {
        return request.getRemoteUser();
    }

    @Override
    public boolean isUserInRole(String role) {
        return request.isUserInRole(role);
    }

    @Override
    public Principal getUserPrincipal() {
        return request.getUserPrincipal();
    }

    @Override
    public String getRequestedSessionId() {
        return request.getRequestedSessionId();
    }

    @Override
    public String getRequestURI() {
        return request.getRequestURI();
    }

    @Override
    public StringBuffer getRequestURL() {
        return request.getRequestURL();
    }

    @Override
    public String getServletPath() {
        return request.getServletPath();
    }

    @Override
    public HttpSession getSession(boolean create) {
        return request.getSession(create);
    }

    @Override
    public HttpSession getSession() {
        return request.getSession();
    }

    @Override
    public String changeSessionId() {
        return request.changeSessionId();
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return request.isRequestedSessionIdValid();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return request.isRequestedSessionIdFromCookie();
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return request.isRequestedSessionIdFromURL();
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        return request.authenticate(response);
    }

    @Override
    public void login(String username, String password) throws ServletException {
        request.login(username, password);
    }

    @Override
    public void logout() throws ServletException {
        request.logout();
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return request.getParts();
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        return request.getPart(name);
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        return request.upgrade(handlerClass);
    }

    @Override
    public Object getAttribute(String name) {
        return request.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return request.getAttributeNames();
    }

    @Override
    public String getCharacterEncoding() {
        return request.getCharacterEncoding();
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        request.setCharacterEncoding(env);
    }

    @Override
    public int getContentLength() {
        return request.getContentLength();
    }

    @Override
    public long getContentLengthLong() {
        return request.getContentLengthLong();
    }

    @Override
    public String getContentType() {
        return request.getContentType();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return request.getInputStream();
    }

    @Override
    public String getParameter(String name) {
        return request.getParameter(name);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return request.getParameterNames();
    }

    @Override
    public String[] getParameterValues(String name) {
        return request.getParameterValues(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return request.getParameterMap();
    }

    @Override
    public String getProtocol() {
        return request.getProtocol();
    }

    @Override
    public String getScheme() {
        return request.getScheme();
    }

    @Override
    public String getServerName() {
        return request.getServerName();
    }

    @Override
    public int getServerPort() {
        return request.getServerPort();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return request.getReader();
    }

    @Override
    public String getRemoteAddr() {
        return request.getRemoteAddr();
    }

    @Override
    public String getRemoteHost() {
        return request.getRemoteHost();
    }

    @Override
    public void setAttribute(String name, Object o) {
        request.setAttribute(name, o);
    }

    @Override
    public void removeAttribute(String name) {
        request.removeAttribute(name);
    }

    @Override
    public Locale getLocale() {
        return request.getLocale();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return request.getLocales();
    }

    @Override
    public boolean isSecure() {
        return request.isSecure();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return request.getRequestDispatcher(path);
    }

    @Override
    public int getRemotePort() {
        return request.getRemotePort();
    }

    @Override
    public String getLocalName() {
        return request.getLocalName();
    }

    @Override
    public String getLocalAddr() {
        return request.getLocalAddr();
    }

    @Override
    public int getLocalPort() {
        return request.getLocalPort();
    }

    @Override
    public ServletContext getServletContext() {
        return request.getServletContext();
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return request.startAsync();
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return request.startAsync(servletRequest, servletResponse);
    }

    @Override
    public boolean isAsyncStarted() {
        return request.isAsyncStarted();
    }

    @Override
    public boolean isAsyncSupported() {
        return request.isAsyncSupported();
    }

    @Override
    public AsyncContext getAsyncContext() {
        return request.getAsyncContext();
    }

    @Override
    public DispatcherType getDispatcherType() {
        return request.getDispatcherType();
    }

    @Override
    public MultiMap<String, String> getHeaders() {
        if (request instanceof HeaderProvider) {
            return ((HeaderProvider) request).getHeaders();
        }
        if (headers == null) {
            // direct access the underlying
            // org.apache.catalina.connector.RequestFacade
            // org.apache.catalina.connector.Request
            HeaderProviderFactory factory = registry.getFactory(request.getClass());
            HeaderProvider provider = factory != null ? factory.create(request) : null;
            headers = provider != null
                    ? provider.getHeaders()
                    : HttpUtils.parseHeader(request.getHeaderNames(), request::getHeaders);
        }
        return headers;
    }

    @Override
    public void setRequest(ServletRequest request) {
        super.setRequest(request);
        this.request = request instanceof HttpServletRequest ? (HttpServletRequest) request : this.request;
    }

    /**
     * Replaces the HttpServletRequest object at the specified index in the arguments array with a JakartaRequest wrapper,
     * if it is not already a JakartaRequest instance.
     *
     * @param arguments the array of arguments
     * @param index     the index of the HttpServletRequest object to replace
     * @param registry  the registry of header providers
     * @return the replaced HttpServletRequest object
     */
    public static HttpServletRequest wrap(final Object[] arguments, final int index, HeaderProviderRegistry registry) {
        HttpServletRequest hsr = (HttpServletRequest) arguments[index];
        if (hsr instanceof HeaderProvider) {
            return hsr;
        }
        ServletRequest request = hsr;
        while (request instanceof ServletRequestWrapper) {
            request = ((ServletRequestWrapper) request).getRequest();
            if (request instanceof HeaderProvider && request instanceof HttpServletRequest) {
                return (HttpServletRequest) request;
            }
        }
        // fixed for ShiroHttpServletRequest
        hsr = new JakartaRequest(hsr, registry);
        arguments[index] = hsr;
        return hsr;
    }
}
