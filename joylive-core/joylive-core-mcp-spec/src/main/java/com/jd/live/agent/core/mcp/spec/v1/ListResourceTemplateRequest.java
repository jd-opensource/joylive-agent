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

import java.util.Map;

/**
 * Sent from the client to request a list of resource templates the server has.
 *
 * @category `resources/templates/list`
 */
public class ListResourceTemplateRequest extends PaginatedRequest {

    public ListResourceTemplateRequest() {
    }

    public ListResourceTemplateRequest(String cursor) {
        super(cursor);
    }

    public ListResourceTemplateRequest(String cursor, Map<String, Object> meta) {
        super(cursor, meta);
    }
}
