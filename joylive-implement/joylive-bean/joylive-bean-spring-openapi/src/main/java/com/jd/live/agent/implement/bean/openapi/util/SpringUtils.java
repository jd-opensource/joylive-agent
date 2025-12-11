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
package com.jd.live.agent.implement.bean.openapi.util;

import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.core.bootstrap.AppContext;
import com.jd.live.agent.core.openapi.spec.v3.OpenApiFactory;
import com.jd.live.agent.implement.bean.openapi.swagger.OpenApi2Factory;
import com.jd.live.agent.implement.bean.openapi.swagger.OpenApi30Factory;
import com.jd.live.agent.implement.bean.openapi.swagger.OpenApi31Factory;
import io.swagger.models.Swagger;
import org.springframework.core.io.ResourceLoader;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;
import static com.jd.live.agent.core.util.type.ClassUtils.getDeclaredMethod;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

public class SpringUtils {

    private static final ClassLoader CLASS_LOADER = ResourceLoader.class.getClassLoader();

    private static final String TYPE_OPEN_API31 = "io.swagger.v3.oas.models.annotations.OpenAPI31";
    private static final Class<?> CLASS_OPEN_API_V31 = loadClass(TYPE_OPEN_API31, CLASS_LOADER);
    private static final String TYPE_OPEN_API_RESOURCE = "org.springdoc.api.AbstractOpenApiResource";
    private static final Class<?> CLASS_OPEN_API_RESOURCE = loadClass(TYPE_OPEN_API_RESOURCE, CLASS_LOADER);
    private static final Method METHOD_GET_OPEN_API = getDeclaredMethod(CLASS_OPEN_API_RESOURCE, "getOpenApi", new Class[]{Locale.class});
    private static final String TYPE_DOCUMENTATION_CACHE = "springfox.documentation.spring.web.DocumentationCache";
    private static final Class<?> CLASS_DOCUMENTATION_CACHE = loadClass(TYPE_DOCUMENTATION_CACHE, CLASS_LOADER);
    private static final String TYPE_SERVICE_MODEL_TO_SWAGGER2_MAPPER = "springfox.documentation.swagger2.mappers.ServiceModelToSwagger2Mapper";
    private static final Class<?> CLASS_SERVICE_MODEL_TO_SWAGGER2_MAPPER = loadClass(TYPE_SERVICE_MODEL_TO_SWAGGER2_MAPPER, CLASS_LOADER);
    private static final String TYPE_DOCUMENTATION = "springfox.documentation.service.Documentation";
    private static final Class<?> CLASS_DOCUMENTATION = loadClass(TYPE_DOCUMENTATION, CLASS_LOADER);
    private static final Method METHOD_DOCUMENTATION_BY_GROUP = getDeclaredMethod(CLASS_DOCUMENTATION_CACHE, "documentationByGroup", new Class[]{String.class});
    private static final Method METHOD_MAP_DOCUMENTATION = getDeclaredMethod(CLASS_SERVICE_MODEL_TO_SWAGGER2_MAPPER, "mapDocumentation", new Class[]{CLASS_DOCUMENTATION});
    private static final FieldAccessor ACCESSOR_HIDDEN_REST_CONTROLLERS = getAccessor(CLASS_OPEN_API_RESOURCE, "HIDDEN_REST_CONTROLLERS");

    @SuppressWarnings("unchecked")
    public static void addOpenApiHiddenControllers(Class<?>... types) {
        if (ACCESSOR_HIDDEN_REST_CONTROLLERS != null) {
            try {
                Set<Class<?>> set = ACCESSOR_HIDDEN_REST_CONTROLLERS.get(null, Set.class);
                Collections.addAll(set, types);
            } catch (Throwable ignore) {
            }
        }
    }

    public static OpenApiFactory getApiFactory(AppContext context) {
        try {
            if (CLASS_OPEN_API_RESOURCE != null && METHOD_GET_OPEN_API != null) {
                Object bean = context.getBean(CLASS_OPEN_API_RESOURCE);
                Callable<io.swagger.v3.oas.models.OpenAPI> callable = () -> (io.swagger.v3.oas.models.OpenAPI) METHOD_GET_OPEN_API.invoke(bean, Locale.getDefault());
                return CLASS_OPEN_API_V31 != null ? new OpenApi31Factory(callable) : new OpenApi30Factory(callable);
            } else if (CLASS_DOCUMENTATION_CACHE != null && CLASS_SERVICE_MODEL_TO_SWAGGER2_MAPPER != null) {
                Object documentCache = context.getBean(CLASS_DOCUMENTATION_CACHE);
                Object mapper = context.getBean(CLASS_SERVICE_MODEL_TO_SWAGGER2_MAPPER);
                if (documentCache != null && mapper != null) {
                    Object document = METHOD_DOCUMENTATION_BY_GROUP.invoke(documentCache, "default");
                    if (document != null) {
                        Callable<Swagger> callable = () -> (Swagger) METHOD_MAP_DOCUMENTATION.invoke(mapper, document);
                        return new OpenApi2Factory(callable);
                    }
                    return null;
                }
            }
        } catch (Throwable ignore) {
        }
        // TODO add reflection factory
        return null;
    }
}
