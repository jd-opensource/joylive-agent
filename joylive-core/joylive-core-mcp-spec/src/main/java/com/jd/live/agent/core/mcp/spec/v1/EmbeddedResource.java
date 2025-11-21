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

import com.jd.live.agent.core.parser.annotation.JsonField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * The contents of a resource, embedded into a prompt or tool call result.
 * It is up to the client how best to render embedded resources for the benefit of the
 * LLM and/or the user.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddedResource implements Annotated, Content {

    /**
     * Optional annotations for the client
     */
    private Annotations annotations;
    /**
     * The resource contents that are embedded
     */
    private ResourceContents resource;
    /**
     * See specification for notes on _meta usage
     */
    @JsonField("_meta")
    private Map<String, Object> meta;

    public EmbeddedResource(Annotations annotations, ResourceContents resource) {
        this(annotations, resource, null);
    }

    @Override
    public String getType() {
        return "resource";
    }

    public List<Role> audience() {
        return annotations == null ? null : annotations.getAudience();
    }

    public Double priority() {
        return annotations == null ? null : annotations.getPriority();
    }
}
