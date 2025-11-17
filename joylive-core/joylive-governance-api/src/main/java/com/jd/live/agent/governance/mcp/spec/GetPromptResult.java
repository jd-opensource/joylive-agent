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
package com.jd.live.agent.governance.mcp.spec;

import com.jd.live.agent.core.parser.json.JsonField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * The server's response to a prompts/get request from the client.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetPromptResult implements Result {

    /**
     * An optional description for the prompt.
     */
    private String description;
    /**
     * A list of messages to display as part of the prompt.
     */
    private List<PromptMessage> messages;
    /**
     * See specification for notes on _meta usage
     */
    @JsonField("_meta")
    private Map<String, Object> meta;

    public GetPromptResult(String description, List<PromptMessage> messages) {
        this(description, messages, null);
    }

}
