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
package com.jd.live.agent.core.mcp;

/**
 * Represents an MCP tool method invocation with its context.
 */
public class McpToolInvocation {

    private McpRequest request;

    private McpSession session;

    private McpToolMethod method;

    private Object[] args;

    public McpToolInvocation(McpRequest request, McpSession session, McpToolMethod method, Object[] args) {
        this.request = request;
        this.session = session;
        this.method = method;
        this.args = args;
    }

    public McpRequest getRequest() {
        return request;
    }

    public McpSession getSession() {
        return session;
    }

    public McpToolMethod getMethod() {
        return method;
    }

    public Object[] getArgs() {
        return args;
    }
}
