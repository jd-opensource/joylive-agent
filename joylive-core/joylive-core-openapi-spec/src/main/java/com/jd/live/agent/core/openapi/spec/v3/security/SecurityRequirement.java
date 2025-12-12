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

package com.jd.live.agent.core.openapi.spec.v3.security;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SecurityRequirement object lists required security schemes for the API.
 * Each entry maps a security scheme name to its required scopes.
 */
@Getter
@Setter
public class SecurityRequirement extends LinkedHashMap<String, List<String>> {

    public SecurityRequirement() {
    }

    public SecurityRequirement(int initialCapacity) {
        super(initialCapacity);
    }

    public SecurityRequirement(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public SecurityRequirement(Map<? extends String, ? extends List<String>> m) {
        super(m);
    }

    public SecurityRequirement(int initialCapacity, float loadFactor, boolean accessOrder) {
        super(initialCapacity, loadFactor, accessOrder);
    }
}