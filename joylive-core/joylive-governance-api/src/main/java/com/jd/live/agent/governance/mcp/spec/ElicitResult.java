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
 * The client's response to an elicitation request.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElicitResult implements Result {
    /**
     * The user action in response to the elicitation. "accept": User
     * submitted the form/confirmed the action, "decline": User explicitly declined the
     * action, "cancel": User dismissed without making an explicit choice
     */
    private Action action;
    /**
     * The submitted form data, only present when action is "accept".
     * Contains values matching the requested schema
     */
    private Map<String, Object> content;
    /**
     * See specification for notes on _meta usage
     */
    @JsonField("_meta")
    private Map<String, Object> meta;

    public ElicitResult(Action action, Map<String, Object> content) {
        this(action, content, null);
    }

    public enum Action {

        @JsonField("accept")
        ACCEPT,

        @JsonField("decline")
        DECLINE,

        @JsonField("cancel")
        CANCEL
    }
}
