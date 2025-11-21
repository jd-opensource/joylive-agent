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

package com.jd.live.agent.core.openapi.spec.v3.info;

import lombok.*;

import java.util.Map;

/**
 * API metadata information object.
 * Contains title, description, terms of service, contact, license and version details.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Info {
    /**
     * API title
     */
    private String title;

    /**
     * API description
     */
    private String description;

    private String summary;

    /**
     * Terms of service URL
     */
    private String termsOfService;

    /**
     * Contact information
     */
    private Contact contact;

    /**
     * License information
     */
    private License license;

    /**
     * API version
     */
    private String version;

    /**
     * Custom specification extensions that start with "x-".
     * Allows adding vendor-specific fields not defined in the OpenAPI specification.
     */
    private Map<String, Object> extensions;

    public String getSummary() {
        if (summary == null || summary.isEmpty()) {
            if (description != null && !description.isEmpty()) {
                return description;
            }
            if (title != null && !title.isEmpty()) {
                return title;
            }
        }
        return summary;
    }

    public String getDescription() {
        if (description == null || description.isEmpty()) {
            if (summary != null && !summary.isEmpty()) {
                return summary;
            }
            if (title != null && !title.isEmpty()) {
                return title;
            }
        }
        return description;
    }

}