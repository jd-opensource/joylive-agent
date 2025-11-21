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
 * Contact information for the exposed API.
 * Includes name, URL and email address.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contact {
    /**
     * Contact name
     */
    private String name;

    /**
     * Contact URL
     */
    private String url;

    /**
     * Contact email
     */
    private String email;

    private Map<String, Object> extensions;

}