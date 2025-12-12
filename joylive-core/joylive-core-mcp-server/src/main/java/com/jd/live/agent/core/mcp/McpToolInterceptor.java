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
 * Interceptor for MCP tool method invocations.
 */
public interface McpToolInterceptor {

    /**
     * Intercepts and processes a tool method invocation.
     *
     * @param invocation The tool invocation context
     * @return The result of the invocation
     * @throws Exception If an error occurs during interception
     */
    Object intercept(McpToolInvocation invocation) throws Exception;

}
