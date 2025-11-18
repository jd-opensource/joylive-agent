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

package com.jd.live.agent.governance.openapi.tags;

import com.jd.live.agent.governance.openapi.ExternalDoc;
import lombok.*;

import java.util.Map;

/**
 * Represents an OpenAPI tag object that can be used to categorize operations.
 *
 * @see <a href="https://spec.openapis.org/oas/v3.0.3#tag-object">OpenAPI Tag Object</a>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {
    /**
     * The name of the tag
     */
    private String name;

    /**
     * A short description for the tag
     */
    private String description;

    /**
     * Additional external documentation for this tag
     */
    private ExternalDoc externalDocs;

    private Map<String, Object> extensions;
}