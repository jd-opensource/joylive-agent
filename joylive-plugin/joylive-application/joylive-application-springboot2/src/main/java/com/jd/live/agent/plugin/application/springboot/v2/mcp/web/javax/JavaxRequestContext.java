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
package com.jd.live.agent.plugin.application.springboot.v2.mcp.web.javax;

import com.jd.live.agent.core.parser.JsonSchemaParser;
import com.jd.live.agent.core.parser.ObjectConverter;
import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.governance.mcp.McpRequestContext.AbstractRequestContext;
import com.jd.live.agent.governance.mcp.McpToolMethod;
import com.jd.live.agent.governance.mcp.McpVersion;
import com.jd.live.agent.governance.openapi.OpenApi;
import lombok.Builder;
import lombok.Getter;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.jd.live.agent.core.util.StringUtils.isEmpty;
import static com.jd.live.agent.core.util.http.HttpUtils.parseCookie;
import static com.jd.live.agent.core.util.http.HttpUtils.parseHeader;

@Getter
public class JavaxRequestContext extends AbstractRequestContext {

    private final WebRequest webRequest;

    private final HttpServletRequest httpRequest;

    private final HttpServletResponse httpResponse;

    private final LazyObject<Map<String, List<String>>> headers;

    private final LazyObject<Map<String, List<String>>> cookies;

    @Builder
    public JavaxRequestContext(Map<String, McpToolMethod> methods,
                               Map<String, McpToolMethod> paths,
                               ObjectConverter converter,
                               JsonSchemaParser jsonSchemaParser,
                               McpVersion version,
                               Supplier<OpenApi> openApi,
                               WebRequest webRequest,
                               HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) {
        super(methods, paths, converter, jsonSchemaParser, version, openApi);
        this.webRequest = webRequest;
        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;
        this.headers = LazyObject.of(() -> parseHeader(httpRequest.getHeaderNames(), v -> httpRequest.getHeaders(v)));
        this.cookies = LazyObject.of(() -> parseCookie(httpRequest.getCookies(), c -> c.getName(), c -> c.getValue()));
    }

    @Override
    public Object getHeader(String name) {
        return name == null ? null : getHeaders().get(name);
    }

    @Override
    public Map<String, ? extends Object> getHeaders() {
        return headers.get();
    }

    @Override
    public Map<String, ? extends Object> getCookies() {
        return cookies.get();
    }

    @Override
    public Object getCookie(String name) {
        return name == null ? null : getCookies().get(name);
    }

    @Override
    public void addCookie(String name, String value) {
        if (!isEmpty(name) && !isEmpty(value)) {
            httpResponse.addCookie(new Cookie(name, value));
        }
    }

    @Override
    public Object getSessionAttribute(String name) {
        return httpRequest.getSession().getAttribute(name);
    }

    @Override
    public Object getRequestAttribute(String name) {
        return httpRequest.getAttribute(name);
    }
}
