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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
public class ToolAnnotations {
    private String title;
    private boolean readOnlyHint;
    private boolean destructiveHint;
    private boolean idempotentHint;
    private boolean openWorldHint;
    private boolean returnDirect;
}
