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

package com.jd.live.agent.governance.openapi.callbacks;

import com.jd.live.agent.core.parser.json.JsonField;
import com.jd.live.agent.governance.openapi.PathItem;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a callback in OpenAPI specification.
 */
@Getter
@Setter
public class Callback extends LinkedHashMap<String, PathItem> {

    /**
     * Reference to a callback defined elsewhere.
     */
    @JsonField("$ref")
    private String ref;

    /**
     * Custom specification extensions that start with "x-".
     */
    private Map<String, Object> extensions;

    /**
     * Constructs a Callback with the provided map of expressions and path items.
     *
     * @param m Map of callback expressions to path items
     */
    public Callback(Map<? extends String, ? extends PathItem> m) {
        super(m);
    }

    /**
     * Constructs a Callback with the provided parameters.
     *
     * @param m          Map of callback expressions to path items
     * @param ref        Reference to a callback defined elsewhere
     * @param extensions Custom specification extensions
     */
    @Builder
    public Callback(Map<? extends String, ? extends PathItem> m, String ref, Map<String, Object> extensions) {
        super(m);
        this.ref = ref;
        this.extensions = extensions;
    }
}