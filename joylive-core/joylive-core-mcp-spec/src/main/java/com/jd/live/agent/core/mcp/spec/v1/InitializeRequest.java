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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * This request is sent from the client to the server when it first connects, asking
 * it to begin initialization.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InitializeRequest implements Request.MetaRequest {

    /**
     * The latest version of the Model Context Protocol that the client supports.
     * The client MAY decide to support older versions as well
     */
    private String protocolVersion;
    /**
     * The capabilities that the client supports
     */
    private ClientCapabilities capabilities;
    /**
     * Information about the client implementation
     */
    private Implementation clientInfo;
    /**
     * See specification for notes on _meta usage
     */
    @JsonField("_meta")
    private Map<String, Object> meta;

    public InitializeRequest(String protocolVersion, ClientCapabilities capabilities, Implementation clientInfo) {
        this(protocolVersion, capabilities, clientInfo, null);
    }

}
