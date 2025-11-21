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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The server's response to a tools/list request from the client.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ListToolsResult implements Result {

    /**
     * An array of Root objects, each representing a root directory or file
     * that the server can operate on.
     */
    private List<Tool> tools;
    /**
     * An optional cursor for pagination. If present, indicates there
     * are more roots available. The client can use this cursor to request the next page
     * of results by sending a roots/list request with the cursor parameter set to this
     */
    private String nextCursor;
    /**
     * See specification for notes on _meta usage
     */
    @JsonField("_meta")
    private Map<String, Object> meta;

    public ListToolsResult(List<Tool> tools) {
        this(tools, null, null);
    }

    public ListToolsResult(List<Tool> tools, String nextCursor) {
        this(tools, nextCursor, null);
    }

    public void addTool(Tool tool) {
        if (tool != null) {
            if (tools == null) {
                tools = new ArrayList<>();
            }
            tools.add(tool);
        }
    }
}
