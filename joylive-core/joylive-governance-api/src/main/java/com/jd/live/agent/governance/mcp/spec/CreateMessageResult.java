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
import lombok.*;

import java.util.Map;

/**
 * The client's response to a sampling/create_message request from the server. The
 * client should inform the user before returning the sampled message, to allow them
 * to inspect the response (human in the loop) and decide whether to allow the server
 * to see it.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMessageResult implements Result {
    /**
     * The role of the message sender (typically assistant)
     */
    private Role role;
    /**
     * The content of the sampled message
     */
    private Content content;
    /**
     * The name of the model that generated the message
     */
    private String model;
    /**
     * The reason why sampling stopped, if known
     */
    private StopReason stopReason;
    /**
     * See specification for notes on _meta usage
     */
    @JsonField("_meta")
    private Map<String, Object> meta;

    public CreateMessageResult(Role role, Content content, String model, StopReason stopReason) {
        this(role, content, model, stopReason, null);
    }

    public enum StopReason {
        @JsonField("endTurn")
        END_TURN("endTurn"),

        @JsonField("stopSequence")
        STOP_SEQUENCE("stopSequence"),

        @JsonField("maxTokens")
        MAX_TOKENS("maxTokens"),

        @JsonField("unknown")
        UNKNOWN("unknown");

        private final String value;

        StopReason(String value) {
            this.value = value;
        }

    }
}
