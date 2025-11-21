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
 * A reference to a resource or resource template definition for completion requests.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResourceReference implements CompleteReference {

    public static final String TYPE = "ref/resource";

    /**
     * The reference type identifier (typically "ref/resource")
     */
    private String type;
    /**
     * The URI or URI template of the resource
     */
    private String uri;

    public ResourceReference(String uri) {
        this(TYPE, uri);
    }

    @Override
    public String identifier() {
        return getUri();
    }
}
