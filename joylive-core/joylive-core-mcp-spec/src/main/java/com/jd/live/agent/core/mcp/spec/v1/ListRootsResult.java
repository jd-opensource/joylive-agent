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

import java.util.List;
import java.util.Map;

/**
 * The client's response to a roots/list request from the server. This result contains
 * an array of Root objects, each representing a root directory or file that the
 * server can operate on.
 */
@Getter
@Setter
public class ListRootsResult extends PaginatedResult {

    /**
     * A list of tools that the server provides.
     */
    private List<Root> roots;

    public ListRootsResult() {
    }

    public ListRootsResult(List<Root> roots) {
        this(roots, null, null);
    }

    public ListRootsResult(List<Root> roots, String nextCursor) {
        this(roots, nextCursor, null);
    }

    public ListRootsResult(List<Root> roots, String nextCursor, Map<String, Object> meta) {
        super(nextCursor, meta);
        this.roots = roots;
    }
}
