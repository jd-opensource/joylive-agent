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
package com.jd.live.agent.plugin.router.springweb.v6.request;

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

    private final HttpServletRequest unwrapped;

    private final HeaderProviderRegistry registry;

    private MultiMap<String, String> headers;

    public JakartaRequest(HttpServletRequest request, HttpServletRequest unwrapped, HeaderProviderRegistry registry) {
        super(request);
        this.unwrapped = unwrapped;
        this.registry = registry;
    }

    @Override
    public String getRequestId() {
        return unwrapped.getRequestId();
    }

    @Override
    public String getProtocolRequestId() {
        return unwrapped.getProtocolRequestId();
    }

    @Override
    public ServletConnection getServletConnection() {
        return unwrapped.getServletConnection();
    }

    @Override
    public void setCharacterEncoding(Charset encoding) {
        unwrapped.setCharacterEncoding(encoding);
    }

    @Override
    public HttpServletMapping getHttpServletMapping() {
        return unwrapped.getHttpServletMapping();
    }

    @Override
    @Deprecated
    public PushBuilder newPushBuilder() {
        return unwrapped.newPushBuilder();
    }

    @Override
    public Map<String, String> getTrailerFields() {
        return unwrapped.getTrailerFields();
    }

    @Override
    public boolean isTrailerFieldsReady() {
        return unwrapped.isTrailerFieldsReady();
    }

    @Override
    public String getAuthType() {
        return unwrapped.getAuthType();
    }

    @Override
    public Cookie[] getCookies() {
        return unwrapped.getCookies();
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
        return unwrapped.getMethod();
    }

    @Override
    public String getPathInfo() {
        return unwrapped.getPathInfo();
    }

    @Override
    public String getPathTranslated() {
        return unwrapped.getPathTranslated();
    }

    @Override
    public String getContextPath() {
        return unwrapped.getContextPath();
    }

    @Override
    public String getQueryString() {
        return unwrapped.getQueryString();
    }

    @Override
    public String getRemoteUser() {
        return unwrapped.getRemoteUser();
    }

    @Override
    public boolean isUserInRole(String role) {
        return unwrapped.isUserInRole(role);
    }

    @Override
    public Principal getUserPrincipal() {
        return unwrapped.getUserPrincipal();
    }

    @Override
    public String getRequestedSessionId() {
        return unwrapped.getRequestedSessionId();
    }

    @Override
    public String getRequestURI() {
        return unwrapped.getRequestURI();
    }

    @Override
    public StringBuffer getRequestURL() {
        return unwrapped.getRequestURL();
    }

    @Override
    public String getServletPath() {
        return unwrapped.getServletPath();
    }

    @Override
    public HttpSession getSession(boolean create) {
        return unwrapped.getSession(create);
    }

    @Override
    public HttpSession getSession() {
        return unwrapped.getSession();
    }

    @Override
    public String changeSessionId() {
        return unwrapped.changeSessionId();
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return unwrapped.isRequestedSessionIdValid();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return unwrapped.isRequestedSessionIdFromCookie();
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return unwrapped.isRequestedSessionIdFromURL();
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        return unwrapped.authenticate(response);
    }

    @Override
    public void login(String username, String password) throws ServletException {
        unwrapped.login(username, password);
    }

    @Override
    public void logout() throws ServletException {
        unwrapped.logout();
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return unwrapped.getParts();
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        return unwrapped.getPart(name);
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        return unwrapped.upgrade(handlerClass);
    }

    @Override
    public Object getAttribute(String name) {
        return unwrapped.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return unwrapped.getAttributeNames();
    }

    @Override
    public String getCharacterEncoding() {
        return unwrapped.getCharacterEncoding();
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        unwrapped.setCharacterEncoding(env);
    }

    @Override
    public int getContentLength() {
        return unwrapped.getContentLength();
    }

    @Override
    public long getContentLengthLong() {
        return unwrapped.getContentLengthLong();
    }

    @Override
    public String getContentType() {
        return unwrapped.getContentType();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return unwrapped.getInputStream();
    }

    @Override
    public String getParameter(String name) {
        return unwrapped.getParameter(name);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return unwrapped.getParameterNames();
    }

    @Override
    public String[] getParameterValues(String name) {
        return unwrapped.getParameterValues(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return unwrapped.getParameterMap();
    }

    @Override
    public String getProtocol() {
        return unwrapped.getProtocol();
    }

    @Override
    public String getScheme() {
        return unwrapped.getScheme();
    }

    @Override
    public String getServerName() {
        return unwrapped.getServerName();
    }

    @Override
    public int getServerPort() {
        return unwrapped.getServerPort();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return unwrapped.getReader();
    }

    @Override
    public String getRemoteAddr() {
        return unwrapped.getRemoteAddr();
    }

    @Override
    public String getRemoteHost() {
        return unwrapped.getRemoteHost();
    }

    @Override
    public void setAttribute(String name, Object o) {
        unwrapped.setAttribute(name, o);
    }

    @Override
    public void removeAttribute(String name) {
        unwrapped.removeAttribute(name);
    }

    @Override
    public Locale getLocale() {
        return unwrapped.getLocale();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return unwrapped.getLocales();
    }

    @Override
    public boolean isSecure() {
        return unwrapped.isSecure();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return unwrapped.getRequestDispatcher(path);
    }

    @Override
    public int getRemotePort() {
        return unwrapped.getRemotePort();
    }

    @Override
    public String getLocalName() {
        return unwrapped.getLocalName();
    }

    @Override
    public String getLocalAddr() {
        return unwrapped.getLocalAddr();
    }

    @Override
    public int getLocalPort() {
        return unwrapped.getLocalPort();
    }

    @Override
    public ServletContext getServletContext() {
        return unwrapped.getServletContext();
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return unwrapped.startAsync();
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return unwrapped.startAsync(servletRequest, servletResponse);
    }

    @Override
    public boolean isAsyncStarted() {
        return unwrapped.isAsyncStarted();
    }

    @Override
    public boolean isAsyncSupported() {
        return unwrapped.isAsyncSupported();
    }

    @Override
    public AsyncContext getAsyncContext() {
        return unwrapped.getAsyncContext();
    }

    @Override
    public DispatcherType getDispatcherType() {
        return unwrapped.getDispatcherType();
    }

    @Override
    public MultiMap<String, String> getHeaders() {
        if (headers == null) {
            // direct access the underlying
            // org.apache.catalina.connector.RequestFacade
            // org.apache.catalina.connector.Request
            HeaderProviderFactory factory = registry.getFactory(unwrapped.getClass());
            HeaderProvider provider = factory != null ? factory.create(unwrapped) : null;
            headers = provider != null
                    ? provider.getHeaders()
                    : HttpUtils.parseHeader(unwrapped.getHeaderNames(), unwrapped::getHeaders);
        }
        return headers;
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
        }
        HttpServletRequest unwrapped = request instanceof HttpServletRequest ? (HttpServletRequest) request : hsr;
        hsr = new JakartaRequest(hsr, unwrapped, registry);
        arguments[index] = hsr;
        return hsr;
    }
}
