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

import java.io.Serializable;
import java.util.Map;

/**
 * Base interface for MCP objects that include optional metadata in the `_meta` field.
 */
public interface Meta extends Serializable {

    /**
     * @return additional metadata related to this resource.
     * @see <a href=
     * "https://modelcontextprotocol.io/specification/2025-06-18/basic/index#meta">Specification</a>
     * for notes on _meta usage
     */
    Map<String, Object> getMeta();
}
