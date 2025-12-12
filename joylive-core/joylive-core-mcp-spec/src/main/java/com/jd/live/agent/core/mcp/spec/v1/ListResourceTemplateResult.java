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

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * The server's response to a resources/templates/list request from the client.
 */
@Getter
@Setter
public class ListResourceTemplateResult extends PaginatedRequest {

    /**
     * A list of resource templates that the server provides
     */
    private List<ResourceTemplate> resourcesTemplate;

    public ListResourceTemplateResult() {
    }

    public ListResourceTemplateResult(List<ResourceTemplate> resourcesTemplate) {
        this(resourcesTemplate, null, null);
    }

    public ListResourceTemplateResult(List<ResourceTemplate> resourcesTemplate, String cursor) {
        this(resourcesTemplate, cursor, null);
    }

    public ListResourceTemplateResult(List<ResourceTemplate> resourcesTemplate, String cursor, Map<String, Object> meta) {
        super(cursor, meta);
        this.resourcesTemplate = resourcesTemplate;
    }


}
