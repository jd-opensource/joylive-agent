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

import java.util.Map;

/**
 * The Model Context Protocol (MCP) supports optional progress tracking for
 * long-running operations through notification messages. Either side can send
 * progress notifications to provide updates about operation status.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProgressNotification implements Notification {
    /**
     * A unique token to identify the progress notification. MUST be
     * unique across all active requests.
     */
    private Object progressToken;
    /**
     * A value indicating the current progress.
     */
    private Double progress;
    /**
     * An optional total amount of work to be done, if known.
     */
    private Double total;
    /**
     * An optional message providing additional context about the progress.
     */
    private String message;
    /**
     * See specification for notes on _meta usage
     */
    @JsonField("_meta")
    private Map<String, Object> meta;

    public ProgressNotification(Object progressToken, Double progress, Double total, String message) {
        this(progressToken, progress, total, message, null);
    }
}
