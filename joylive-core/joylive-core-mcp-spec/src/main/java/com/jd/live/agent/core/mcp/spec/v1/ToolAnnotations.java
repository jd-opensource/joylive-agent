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

import lombok.*;

/**
 * Additional properties describing a Tool to clients.
 * <p>
 * NOTE: all properties in ToolAnnotations are **hints**. They are not guaranteed to
 * provide a faithful description of tool behavior (including descriptive properties
 * like `title`).
 * <p>
 * Clients should never make tool use decisions based on ToolAnnotations received from
 * untrusted servers.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolAnnotations {
    /**
     * A human-readable title for the tool.
     */
    private String title;
    /**
     * If true, the tool does not modify its environment.
     * <p>
     * Default: false
     */
    private Boolean readOnlyHint;
    /**
     * If true, the tool may perform destructive updates to its environment.
     * If false, the tool performs only additive updates.
     * <p>
     * (This property is meaningful only when `readOnlyHint == false`)
     * <p>
     * Default: true
     */
    private Boolean destructiveHint;
    /**
     * If true, calling the tool repeatedly with the same arguments
     * will have no additional effect on its environment.
     * <p>
     * (This property is meaningful only when `readOnlyHint == false`)
     * <p>
     * Default: false
     */
    private Boolean idempotentHint;
    /**
     * If true, this tool may interact with an "open world" of external
     * entities. If false, the tool's domain of interaction is closed.
     * For example, the world of a web search tool is open, whereas that
     * of a memory tool is not.
     * <p>
     * Default: true
     */
    private Boolean openWorldHint;

    /**
     * It determines whether the result of a tool execution should be returned directly to the client
     * or if it should be processed further by the language model (LLM)
     */
    private Boolean returnDirect;
}
