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

import lombok.Getter;
import lombok.Setter;

/**
 * A reference to a resource or resource template definition for completion requests.
 */

public class ResourceReference implements CompleteReference {

    /**
     * The reference type identifier (typically "ref/resource")
     */
    private String type = TYPE_REF_RESOURCE;
    /**
     * The URI or URI template of the resource
     */
    @Getter
    @Setter
    private String uri;

    public ResourceReference() {
    }

    public ResourceReference(String uri) {
        this.uri = uri;
    }

    @Override
    public String getType() {
        return TYPE_REF_RESOURCE;
    }

    public void setType(String type) {

    }

    @Override
    public String identifier() {
        return getUri();
    }
}
