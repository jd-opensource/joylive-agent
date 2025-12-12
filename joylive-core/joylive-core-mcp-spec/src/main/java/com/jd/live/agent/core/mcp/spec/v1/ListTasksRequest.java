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
 * A request to retrieve a list of tasks.
 *
 * @category `tasks/list`
 */
public class ListTasksRequest extends PaginatedRequest {

    public ListTasksRequest() {
    }

    public ListTasksRequest(String cursor) {
        super(cursor);
    }

    public ListTasksRequest(String cursor, Map<String, Object> meta) {
        super(cursor, meta);
    }
}
