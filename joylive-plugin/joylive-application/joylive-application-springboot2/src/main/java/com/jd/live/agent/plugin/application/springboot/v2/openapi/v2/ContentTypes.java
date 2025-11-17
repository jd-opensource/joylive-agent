/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.plugin.application.springboot.v2.openapi.v2;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * Represents content type information for API operations.
 */
@Getter
@AllArgsConstructor
public class ContentTypes {

    public static final String DEFAULT_CONTENT_TYPE = "application/json";
    /**
     * List of MIME types that the operation can consume.
     */
    private final List<String> consumes;

    /**
     * List of MIME types that the operation can produce.
     */
    private final List<String> produces;

    public ContentTypes of(List<String> consumes, List<String> produces) {
        consumes = consumes == null || consumes.isEmpty() ? this.consumes : consumes;
        consumes = consumes == null || consumes.isEmpty() ? Collections.singletonList(DEFAULT_CONTENT_TYPE) : consumes;
        produces = produces == null || produces.isEmpty() ? this.produces : produces;
        produces = produces == null || produces.isEmpty() ? Collections.singletonList(DEFAULT_CONTENT_TYPE) : produces;
        return new ContentTypes(consumes, produces);
    }
}
