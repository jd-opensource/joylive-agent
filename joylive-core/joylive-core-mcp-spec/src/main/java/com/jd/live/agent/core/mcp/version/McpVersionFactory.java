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
package com.jd.live.agent.core.mcp.version;

import com.jd.live.agent.core.mcp.version.v1.McpVersion1;
import com.jd.live.agent.core.mcp.version.v2.McpVersion2;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for retrieving MCP version implementations.
 */
public class McpVersionFactory {

    private static Map<String, McpVersion> VERSIONS = new HashMap<>();

    static {
        VERSIONS.put(McpVersion1.INSTANCE.getRevision(), McpVersion1.INSTANCE);
        VERSIONS.put(McpVersion2.INSTANCE.getRevision(), McpVersion2.INSTANCE);
    }

    /**
     * Returns the McpVersion implementation for the specified version string.
     * Defaults to McpVersion1 if the requested version is null or not found.
     *
     * @param version The version identifier string
     * @return The corresponding McpVersion implementation
     */
    public static McpVersion getVersion(String version) {
        McpVersion result = version == null ? null : VERSIONS.get(version);
        result = result == null ? McpVersion1.INSTANCE : result;
        return result;
    }

}
