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
 * Describes an argument that a prompt can accept.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PromptArgument implements Identifier {

    /**
     * The name of the argument.
     */
    private String name;
    /**
     * An optional title for the argument, which can be used in UI
     */
    private String title;
    /**
     * A human-readable description of the argument.
     */
    private String description;
    /**
     * Whether this argument must be provided.
     */
    private boolean required;

    public PromptArgument(String name, String description, boolean required) {
        this(name, null, description, required);
    }
}
