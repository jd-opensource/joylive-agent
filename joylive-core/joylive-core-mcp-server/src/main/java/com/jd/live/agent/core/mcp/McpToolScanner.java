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
package com.jd.live.agent.core.mcp;

import java.util.List;

/**
 * Scanner for MCP tool methods.
 * Used to scan controller objects and extract MCP tool methods.
 */
public interface McpToolScanner {

    /**
     * Scans a controller object and extracts MCP tool methods.
     *
     * @param controller the controller object to scan
     * @return list of MCP tool methods found in the controller
     */
    List<McpToolMethod> scan(Object controller);
}
