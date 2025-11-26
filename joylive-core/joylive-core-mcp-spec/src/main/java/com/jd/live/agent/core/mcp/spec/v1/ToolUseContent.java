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

import java.util.Map;

/**
 * A request from the assistant to call a tool.
 *
 * @category `sampling/createMessage`
 */
public class ToolUseContent implements Content {

    private String type = TYPE_TOOL_USE;

    /**
     * A unique identifier for this tool use.
     * <p>
     * This ID is used to match tool results to their corresponding tool uses.
     */
    @Getter
    @Setter
    private String id;

    /**
     * The name of the tool to call.
     */
    @Getter
    @Setter
    private String name;

    /**
     * The arguments to pass to the tool, conforming to the tool's input schema.
     */
    @Getter
    @Setter
    private Map<String, Object> input;
    /**
     * See specification for notes on _meta usage
     */
    @JsonField("_meta")
    @Getter
    @Setter
    private Map<String, Object> meta;

    public ToolUseContent() {
    }

    @Builder
    public ToolUseContent(String id, String name, Map<String, Object> input, Map<String, Object> meta) {
        this.id = id;
        this.name = name;
        this.input = input;
        this.meta = meta;
    }

    @Override
    public String getType() {
        return TYPE_TOOL_USE;
    }

    public void setType(String type) {

    }
}
