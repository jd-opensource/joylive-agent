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
package com.jd.live.agent.plugin.router.springcloud.v3.request;

import com.jd.live.agent.core.util.cache.UnsafeLazyObject;
import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpInboundRequest;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * ServletHttpInboundRequest
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class ServletInboundRequest extends AbstractHttpInboundRequest<HttpServletRequest> {

    public ServletInboundRequest(HttpServletRequest request) {
        super(request);
        URI u = null;
        try {
            u = new URI(request.getRequestURI());
        } catch (URISyntaxException ignore) {
        }
        uri = u;
        headers = new UnsafeLazyObject<>(() -> parseHeader(request));
        queries = new UnsafeLazyObject<>(() -> parseQuery(request.getQueryString()));
        cookies = new UnsafeLazyObject<>(() -> parseCookie(request));
    }

    @Override
    public HttpMethod getHttpMethod() {
        try {
            return HttpMethod.valueOf(request.getMethod());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    protected String parseScheme() {
        String result = super.parseScheme();
        return result == null ? request.getScheme() : result;
    }

    @Override
    protected int parsePort() {
        int result = super.parsePort();
        return result >= 0 ? result : request.getServerPort();
    }

    @Override
    protected String parseHost() {
        String result = super.parseHost();
        return result == null ? request.getServerName() : result;
    }

    protected Map<String, List<String>> parseCookie(HttpServletRequest request) {
        Map<String, List<String>> result = new HashMap<>();
        if (request.getCookies() != null) {
            for (javax.servlet.http.Cookie cookie : request.getCookies()) {
                result.computeIfAbsent(cookie.getName(), name -> new ArrayList<>()).add(cookie.getValue());
            }
        }
        return result;
    }

    protected Map<String, List<String>> parseHeader(HttpServletRequest request) {
        Map<String, List<String>> result = new HashMap<>();
        Enumeration<?> keyEnums = request.getHeaderNames();
        String key;
        List<String> values;
        Enumeration<String> valueEnumeration;
        while (keyEnums.hasMoreElements()) {
            key = (String) keyEnums.nextElement();
            values = result.computeIfAbsent(key, k -> new ArrayList<>());
            valueEnumeration = request.getHeaders(key);
            if (valueEnumeration != null) {
                while (valueEnumeration.hasMoreElements()) {
                    values.add(valueEnumeration.nextElement());
                }
            }
        }
        return result;
    }

}
