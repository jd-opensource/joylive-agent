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

import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.governance.mcp.spec.JsonSchema;

/**
 * Represents a specific version of the MCP (Model Control Protocol) implementation.
 * <p>
 * This interface provides access to version information and the ability to create
 * tool definitions appropriate for this version of the protocol. Implementations
 * should be registered using the Extension annotation.
 * </p>
 */
@Extensible("McpVersion")
public interface McpVersion {

    String PROPERTY_RESULT = "result";

    /**
     * Gets the revision identifier for this MCP version.
     *
     * @return A string representing the revision of this MCP implementation
     */
    String getRevision();

    /**
     * Creates tool definitions compatible with this MCP version.
     *
     * @return A new instance of tool definitions for this protocol version
     */
    McpToolDefinitions createDefinitions();

    /**
     * Formats the output schema according to the specific MCP version requirements.
     *
     * @param schema The original output JSON schema to be formatted
     * @return A JSON schema formatted according to this MCP version's requirements
     */
    JsonSchema output(JsonSchema schema);

    /**
     * Formats result according to the specific MCP version requirements.
     *
     * @param result The result to be formatted
     * @return An object formatted according to this MCP version's requirements
     */
    Object output(Object result);
}
