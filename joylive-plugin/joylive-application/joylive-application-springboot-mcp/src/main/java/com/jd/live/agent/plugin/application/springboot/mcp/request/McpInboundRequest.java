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
package com.jd.live.agent.plugin.application.springboot.mcp.request;

import com.jd.live.agent.core.mcp.McpRequest;
import com.jd.live.agent.core.mcp.McpToolMethod;
import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.core.util.map.CaseInsensitiveLinkedMap;
import com.jd.live.agent.core.util.network.ClientIp;
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpInboundRequest;

import java.util.*;
import java.util.function.Function;

import static com.jd.live.agent.core.util.type.ClassUtils.isSimpleValueType;

public class McpInboundRequest extends AbstractHttpInboundRequest<McpRequest> {

    private final McpToolMethod method;

    public McpInboundRequest(McpRequest request, McpToolMethod method) {
        super(request);
        this.method = method;
    }

    @Override
    public HttpMethod getHttpMethod() {
        Set<String> methods = method.getHttpMethods();
        if (methods != null && !methods.isEmpty()) {
            if (methods.size() == 1) {
                return HttpMethod.ofNullable(methods.iterator().next());
            }
            for (String methodName : methods) {
                HttpMethod method = HttpMethod.ofNullable(methodName);
                if (method != null) {
                    return method;
                }
            }
        }
        return null;
    }

    @Override
    public String getClientIp() {
        return ClientIp.getIp(this::getHeader, request::getRemoteAddr);
    }

    @Override
    public String getQuery(String key) {
        Object value = request.getQuery(key);
        return value instanceof String ? (String) value : null;
    }

    @Override
    public String getHeader(String key) {
        Object value = request.getHeader(key);
        return value instanceof String ? (String) value : null;
    }

    @Override
    public String getCookie(String key) {
        Object value = request.getCookie(key);
        return value instanceof String ? (String) value : null;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map<String, List<String>> parseCookies() {
        return toMultiValueMap(request.getCookies(), i -> new LinkedHashMap<>());
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return toMultiValueMap(request.getHeaders(), i -> new CaseInsensitiveLinkedMap<>());
    }

    @Override
    protected Map<String, List<String>> parseQueries() {
        return toMultiValueMap(request.getQueries(), i -> new LinkedHashMap<>());
    }

    /**
     * Converts a generic map to a multi-value map with string list values.
     *
     * @param src      Source map to convert
     * @param function Factory function to create the result map with appropriate size
     * @return Map with string lists as values, or null if source is empty
     */
    private Map<String, List<String>> toMultiValueMap(Map<String, ?> src, Function<Integer, Map<String, List<String>>> function) {
        if (src == null || src.isEmpty()) {
            return null;
        }
        Map<String, List<String>> result = function.apply(src.size());
        for (Map.Entry<String, ?> entry : src.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                result.put(key, new ArrayList<>());
            } else if (value instanceof List<?>) {
                List<?> source = (List<?>) value;
                if (source.isEmpty()) {
                    result.put(key, (List<String>) source);
                } else {
                    List<String> values = new ArrayList<>(source.size());
                    for (Object item : source) {
                        if (item != null && (item instanceof String || isSimpleValueType(item.getClass()))) {
                            values.add(item.toString());
                        }
                    }
                    result.put(key, values);
                }
            } else if (value instanceof String || isSimpleValueType(value.getClass())) {
                List<String> values = new ArrayList<>();
                values.add(value.toString());
                result.put(key, values);
            }
        }
        return result;
    }
}
