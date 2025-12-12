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
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * The result of a tool use, provided by the user back to the assistant.
 *
 * @category `sampling/createMessage`
 */
public class ToolResultContent implements Content {

    private String type = TYPE_TOOL_RESULT;

    /**
     * The ID of the tool use this result corresponds to.
     * <p>
     * This MUST match the ID from a previous ToolUseContent.
     */
    @Getter
    @Setter
    private String toolUseId;

    /**
     * The unstructured result content of the tool use.
     * <p>
     * This has the same format as CallToolResult.content and can include text, images,
     * audio, resource links, and embedded resources.
     */
    @Getter
    @Setter
    private List<Content> content;

    /**
     * An optional structured result object.
     * <p>
     * If the tool defined an outputSchema, this SHOULD conform to that schema.
     */
    @Getter
    @Setter
    private Object structuredContent;

    /**
     * Whether the tool use resulted in an error.
     * <p>
     * If true, the content typically describes the error that occurred.
     * Default: false
     */
    @Getter
    @Setter
    private Boolean isError;

    /**
     * See specification for notes on _meta usage
     */
    @JsonField("_meta")
    @Getter
    @Setter
    private Map<String, Object> meta;

    public ToolResultContent() {
    }

    @Builder
    public ToolResultContent(String toolUseId, List<Content> content, Object structuredContent, Boolean isError, Map<String, Object> meta) {
        this.toolUseId = toolUseId;
        this.content = content;
        this.structuredContent = structuredContent;
        this.isError = isError;
        this.meta = meta;
    }

    @Override
    public String getType() {
        return TYPE_TOOL_RESULT;
    }

    public void setType(String type) {

    }
}
