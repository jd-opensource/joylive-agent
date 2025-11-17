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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The server's response to a tools/call request from the client.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallToolResult implements Result {
    /**
     * A list of content items representing the tool's output. Each item
     * can be text, an image, or an embedded resource.
     */
    private List<Content> content;
    /**
     * If true, indicates that the tool execution failed and the content contains error information.
     * If false or absent, indicates successful execution.
     */
    private boolean isError;
    /**
     * An optional JSON object that represents the structured result of the tool call.
     */
    private Object structuredContent;
    /**
     * See specification for notes on _meta usage
     */
    @JsonField("_meta")
    private Map<String, Object> meta;

    public CallToolResult(List<Content> content, boolean isError) {
        this(content, isError, null, null);
    }

    public CallToolResult(List<Content> content, boolean isError, Map<String, Object> structuredContent) {
        this(content, isError, structuredContent, null);
    }

    public CallToolResult(String content, Boolean isError) {
        this(new ArrayList<>(Collections.singletonList(new TextContent(content))), isError, null);
    }

    /**
     * Adds a content item to the tool result.
     *
     * @param item the content item to add
     * @return this builder
     */
    public void addContent(Content item) {
        if (item != null) {
            if (content == null) {
                content = new ArrayList<>();
            }
            content.add(item);
        }
    }

    /**
     * Adds a text content item to the tool result.
     *
     * @param text the text content
     * @return this builder
     */
    public void addContent(String text) {
        if (text != null) {
            addContent(new TextContent(text));
        }
    }

    /**
     * Adds multiple text content items to the tool result.
     *
     * @param texts a list of text contents to add
     */
    public void addContent(List<String> texts) {
        if (texts != null && !texts.isEmpty()) {
            texts.forEach(this::addContent);
        }
    }
}
