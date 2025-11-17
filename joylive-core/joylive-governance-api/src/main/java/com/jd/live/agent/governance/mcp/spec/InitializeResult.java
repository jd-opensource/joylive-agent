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
package com.jd.live.agent.governance.mcp.spec;

import com.jd.live.agent.core.parser.json.JsonField;
import lombok.*;

import java.util.Map;

/**
 * After receiving an initialize request from the client, the server sends this response.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitializeResult implements Result {

    /**
     * The version of the Model Context Protocol that the server
     * wants to use. This may not match the version that the client requested. If the
     * client cannot support this version, it MUST disconnect
     */
    private String protocolVersion;
    /**
     * The capabilities that the server supports
     */
    private ServerCapabilities capabilities;
    /**
     * Information about the server implementation
     */
    private Implementation serverInfo;
    /**
     * Instructions describing how to use the server and its features.
     * This can be used by clients to improve the LLM's understanding of available tools,
     * resources, etc. It can be thought of like a "hint" to the model. For example, this
     * information MAY be added to the system prompt
     */
    private String instructions;
    /**
     * See specification for notes on _meta usage
     */
    @JsonField("_meta")
    private Map<String, Object> meta;

    public InitializeResult(String protocolVersion,
                            ServerCapabilities capabilities,
                            Implementation serverInfo,
                            String instructions) {
        this(protocolVersion, capabilities, serverInfo, instructions, null);
    }

}
