/*
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
package com.jd.live.agent.core.openapi.spec.v3;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Factory interface for creating OpenAPI specification objects.
 * Provides methods to create OpenAPI instances and manage controller visibility.
 */
public interface OpenApiFactory {

    AtomicReference<OpenApiFactory> INSTANCE_REF = new AtomicReference<>();

    /**
     * Creates a new OpenAPI specification instance.
     *
     * @return the created OpenAPI object
     */
    OpenApi create();

    /**
     * Adds a controller type to the hidden controllers list.
     * Hidden controllers will be excluded from the OpenAPI specification generation.
     *
     * @param type the controller class type to hide
     */
    void addHiddenController(Class<?> type);

}
