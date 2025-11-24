/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jd.live.agent.core.openapi.spec.v3;

import com.jd.live.agent.core.openapi.spec.v3.parameters.Parameter;
import com.jd.live.agent.core.parser.annotation.JsonField;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PathItem describes the operations available on a single path.
 * Uses a map to flexibly support HTTP methods, e.g. get, post, put, delete, patch, options, head, etc.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PathItem {

    /**
     * Reference to a path item defined in components section
     */
    @JsonField("$ref")
    private String ref;

    /**
     * Summary of the path
     */
    private String summary;

    /**
     * Description of the path
     */
    private String description;

    private Operation get;

    private Operation put;

    private Operation post;

    private Operation delete;

    private Operation options;

    private Operation head;

    private Operation patch;

    private Operation trace;

    /**
     * Parameters for this path
     */
    private List<Parameter> parameters;

    private Map<String, Object> extensions;

    public List<Operation> operations() {
        List<Operation> result = new ArrayList<>(8);
        addOperation(get, result);
        addOperation(put, result);
        addOperation(post, result);
        addOperation(delete, result);
        addOperation(patch, result);
        addOperation(head, result);
        addOperation(options, result);
        addOperation(trace, result);
        return result;
    }

    private void addOperation(Operation operation, List<Operation> operations) {
        if (operation != null) {
            operations.add(operation);
        }
    }

}