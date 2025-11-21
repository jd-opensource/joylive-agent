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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Describes the name and version of an MCP implementation, with an optional title for
 * UI representation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Implementation implements Identifier {
    /**
     * Intended for programmatic or logical use, but used as a display name in
     * past specs or fallback (if title isn't present).
     */
    private String name;
    /**
     * Intended for UI and end-user contexts
     */
    private String title;
    /**
     * The version of the implementation.
     */
    private String version;

    public Implementation(String name, String version) {
        this(name, null, version);
    }
}
