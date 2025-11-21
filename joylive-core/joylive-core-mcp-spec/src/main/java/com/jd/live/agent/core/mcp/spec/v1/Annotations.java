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

import java.io.Serializable;
import java.util.List;

/**
 * Optional annotations for the client. The client can use annotations to inform how
 * objects are used or displayed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Annotations implements Serializable {

    /**
     * Describes who the intended customer of this object or data is. It
     * can include multiple entries to indicate content useful for multiple audiences
     * (e.g., `["user", "assistant"]`).
     */
    private List<Role> audience;

    /**
     * Describes how important this data is for operating the server. A
     * value of 1 means "most important," and indicates that the data is effectively
     * required, while 0 means "least important," and indicates that the data is entirely
     * optional. It is a number between 0 and 1.
     */
    private Double priority;

    private String lastModified;

    public Annotations(List<Role> audience, Double priority) {
        this(audience, priority, null);
    }
}
