/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License; Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing; software
 * distributed under the License is distributed on an "AS IS" BASIS;
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND; either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.core.mcp.spec.v1;

import com.jd.live.agent.core.parser.annotation.JsonField;
import lombok.*;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Represents a tool that the server provides. Tools enable servers to expose
 * executable functionality to the system. Through these tools, you can interact with
 * external systems, perform computations, and take actions in the real world.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tool implements Icons, Serializable {

    public static final String COMPONENT_REF_PREFIX = "#/$defs/";

    /**
     * A unique identifier for the tool. This name is used when calling the tool
     */
    private String name;
    /**
     * A human-readable title for the tool.
     */
    private String title;
    /**
     * A human-readable description of what the tool does. This can be
     * used by clients to improve the LLM's understanding of available tools.
     */
    private String description;
    /**
     * Schema definitions using the newer $defs keyword
     */
    @JsonField("$defs")
    private Map<String, JsonSchema> defs;
    /**
     * A JSON Schema object that describes the expected structure of
     * the arguments when calling this tool. This allows clients to validate tool
     */
    private JsonSchema inputSchema;
    /**
     * An optional JSON Schema object defining the structure of the
     * tool's output returned in the structuredContent field of a CallToolResult.
     */
    private JsonSchema outputSchema;
    /**
     * Optional additional tool information.
     */
    private ToolAnnotations annotations;
    /**
     * Optional set of sized icons that the client can display in a user interface.
     */
    private List<Icon> icons;
    /**
     * See specification for notes on _meta usage
     */
    @JsonField("_meta")
    private Map<String, Object> meta;

}
