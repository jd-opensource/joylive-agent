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
package com.jd.live.agent.core.openapi.spec.v3.media;

import lombok.*;

import java.io.Serializable;
import java.util.Map;

/**
 * Schema discriminator for polymorphic deserialization.
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Discriminator implements Serializable {

    /**
     * Property name for discrimination.
     */
    private String propertyName;

    /**
     * Subtype mappings.
     */
    private Map<String, String> mapping;

    /**
     * Vendor extensions.
     */
    private Map<String, Object> extensions;
}
