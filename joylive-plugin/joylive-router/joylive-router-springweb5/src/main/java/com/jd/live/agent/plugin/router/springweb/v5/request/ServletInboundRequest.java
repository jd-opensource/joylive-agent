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
package com.jd.live.agent.plugin.router.springweb.v5.request;

import com.jd.live.agent.core.mcp.McpToolMethod;
import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.core.util.network.Ipv4;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpInboundRequest;
import com.jd.live.agent.governance.request.HeaderProvider;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * ServletHttpInboundRequest
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class ServletInboundRequest extends AbstractHttpInboundRequest<HttpServletRequest> {

    private final Object[] arguments;
    private final Object handler;
    private final Predicate<Class<?>> systemHanderPredicate;
    private final Predicate<String> systemPathPredicate;
    private final Predicate<String> mcpPathPredicate;

    public ServletInboundRequest(HttpServletRequest request,
                                 Object[] arguments,
                                 Object handler,
                                 GovernanceConfig config) {
        super(request);
        this.arguments = arguments;
        this.handler = handler;
        this.systemHanderPredicate = config.getServiceConfig()::isSystemHandler;
        this.systemPathPredicate = config.getServiceConfig()::isSystemPath;
        this.mcpPathPredicate = config.getMcpConfig()::isMcpPath;
        URI u = null;
        try {
            u = new URI(request.getRequestURI());
        } catch (URISyntaxException ignore) {
        }
        uri = u;
    }

    @Override
    public HttpMethod getHttpMethod() {
        return HttpMethod.ofNullable(request.getMethod());
    }

    @Override
    public String getClientIp() {
        return Ipv4.getClientIp(this::getHeader, request::getRemoteAddr);
    }

    @Override
    public boolean isSystem() {
        // handler is unwrapped from HandlerMethod
        if (handler != null && systemHanderPredicate != null && systemHanderPredicate.test(handler.getClass())) {
            return true;
        }
        if (handler != null && arguments != null && arguments.length == 3 && arguments[2] instanceof Object[]) {
            // ExceptionHandlerExceptionResolver for global @ExceptionHandler(Exception.class)
            Object[] args = (Object[]) arguments[2];
            if (args.length > 1 && args[0] instanceof Throwable && args[args.length - 1] instanceof HandlerMethod) {
                return true;
            }
        }
        if (systemPathPredicate != null && systemPathPredicate.test(getPath())) {
            return true;
        }
        if (isMcp()) {
            return true;
        }
        return super.isSystem();
    }

    public boolean isMcp() {
        return McpToolMethod.HANDLE_METHOD != null && mcpPathPredicate != null && mcpPathPredicate.test(getPath());
    }

    @Override
    protected String parseScheme() {
        String result = super.parseScheme();
        return result == null ? request.getScheme() : result;
    }

    @Override
    protected Address parseAddress() {
        Address result = super.parseAddress();
        if (result == null) {
            result = parseAddressByRequest();
        }
        return result;
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return request instanceof HeaderProvider
                ? ((HeaderProvider) request).getHeaders()
                : HttpUtils.parseHeader(request.getHeaderNames(), request::getHeaders);
    }

    @Override
    protected Map<String, List<String>> parseQueries() {
        return HttpUtils.parseQuery(request.getQueryString());
    }

    @Override
    protected Map<String, List<String>> parseCookies() {
        return HttpUtils.parseCookie(request.getCookies(), Cookie::getName, Cookie::getValue);
    }

    /**
     * Parses the address from the HTTP request.
     *
     * @return the parsed address, or null if the server name is invalid
     */
    protected Address parseAddressByRequest() {
        String serverName = request.getServerName();
        if (validateHost(serverName)) {
            int serverPort = request.getServerPort();
            return new Address(serverName, serverPort < 0 ? null : serverPort);
        }
        return null;
    }
}
