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
package com.jd.live.agent.plugin.application.springboot.v2.mcp.reactive;

import com.jd.live.agent.core.parser.JsonSchemaParser;
import com.jd.live.agent.core.parser.ObjectConverter;
import com.jd.live.agent.governance.mcp.McpParameterParser;
import com.jd.live.agent.governance.mcp.McpRequestContext.AbstractRequestContext;
import com.jd.live.agent.governance.mcp.McpToolMethod;
import com.jd.live.agent.governance.mcp.McpVersion;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseCookie;
import org.springframework.web.server.ServerWebExchange;

import java.util.Map;

import static com.jd.live.agent.core.util.StringUtils.isEmpty;

@Getter
public class ReactiveRequestContext extends AbstractRequestContext {

    private final ServerWebExchange exchange;

    @Builder
    public ReactiveRequestContext(Map<String, McpToolMethod> methods,
                                  Map<String, McpToolMethod> paths,
                                  ObjectConverter converter,
                                  McpParameterParser parameterParser,
                                  JsonSchemaParser jsonSchemaParser,
                                  McpVersion version,
                                  ServerWebExchange exchange) {
        super(methods, paths, converter, parameterParser, jsonSchemaParser, version);
        this.exchange = exchange;
    }

    @Override
    public String getHeader(String name) {
        return name == null ? null : exchange.getRequest().getHeaders().getFirst(name);
    }

    @Override
    public void addCookie(String name, String value) {
        if (!isEmpty(name) && !isEmpty(value)) {
            exchange.getResponse().addCookie(ResponseCookie.from(name, value).build());
        }
    }
}
