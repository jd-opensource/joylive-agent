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
package com.jd.live.agent.implement.bean.openapi.swagger;

import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.core.openapi.spec.v3.OpenApiFactory;

import java.util.Set;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

/**
 * Factory implementation for creating OpenApi objects from Swagger 2 specifications.
 */
public abstract class SwaggerFactory implements OpenApiFactory {

    private static final String TYPE_OPEN_API_RESOURCE = "org.springdoc.api.AbstractOpenApiResource";

    @Override
    public void addHiddenController(Class<?> type, ClassLoader classLoader) {
        Class<?> openApiResourceClass = loadClass(TYPE_OPEN_API_RESOURCE, classLoader);
        FieldAccessor accessor = getAccessor(openApiResourceClass, "HIDDEN_REST_CONTROLLERS");
        if (accessor != null) {
            try {
                Set<Class<?>> set = accessor.get(null, Set.class);
                set.add(type);
            } catch (Throwable ignore) {
            }
        }
    }
}
