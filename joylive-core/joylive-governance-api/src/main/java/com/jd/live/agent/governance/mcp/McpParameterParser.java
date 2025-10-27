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
package com.jd.live.agent.governance.mcp;

import com.jd.live.agent.core.parser.ObjectConverter;

/**
 * Parser for converting MCP parameters to method arguments.
 */
public interface McpParameterParser {

    /**
     * Parse input to method parameters.
     *
     * @param method Target method
     * @param params Raw params
     * @param converter Converter
     * @param ctx Context
     * @return Converted parameter array
     * @throws Exception If parsing fails
     */
    Object[] parse(McpToolMethod method, Object params, ObjectConverter converter, RequestContext ctx) throws Exception;
}
