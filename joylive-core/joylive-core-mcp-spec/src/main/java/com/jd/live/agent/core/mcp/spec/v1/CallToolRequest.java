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
package com.jd.live.agent.core.mcp.spec.v1;

import com.jd.live.agent.core.parser.annotation.JsonField;
import com.jd.live.agent.core.mcp.spec.v1.Request.MetaRequest;
import lombok.*;

import java.util.Map;

/**
 * Used by the client to call a tool provided by the server.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallToolRequest implements MetaRequest {
    /**
     * name The name of the tool to call. This must match a tool name from tools/list.
     */
    private String name;
    /**
     * Arguments to pass to the tool. These must conform to the tool's input schema.
     */
    private Map<String, Object> arguments;
    /**
     * See specification for notes on _meta usage
     */
    @JsonField("_meta")
    private Map<String, Object> meta;

    public CallToolRequest(String name, Map<String, Object> arguments) {
        this(name, arguments, null);
    }
}
