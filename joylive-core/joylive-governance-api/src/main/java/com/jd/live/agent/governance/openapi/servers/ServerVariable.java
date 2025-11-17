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

package com.jd.live.agent.governance.openapi.servers;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Server variable for URL substitution.
 * Contains enum values, default value and description.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerVariable {
    /**
     * Enum values allowed for this variable
     */
    private List<String> enumValues;

    /**
     * Default value of the variable
     */
    private String defaultValue;

    /**
     * Variable description
     */
    private String description;

    private Map<String, Object> extensions;
}