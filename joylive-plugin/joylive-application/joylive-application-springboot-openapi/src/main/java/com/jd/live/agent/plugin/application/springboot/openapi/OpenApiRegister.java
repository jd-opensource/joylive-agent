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
package com.jd.live.agent.plugin.application.springboot.openapi;

import com.jd.live.agent.core.bootstrap.AppBooter;
import com.jd.live.agent.core.bootstrap.AppContext;
import com.jd.live.agent.core.bootstrap.AppListener;
import com.jd.live.agent.core.bootstrap.AppListener.AppListenerAdapter;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.openapi.spec.v3.OpenApiFactory;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.plugin.application.springboot.openapi.swagger.Swagger2Factory;
import com.jd.live.agent.plugin.application.springboot.openapi.swagger.Swagger30Factory;
import com.jd.live.agent.plugin.application.springboot.openapi.swagger.Swagger31Factory;
import io.swagger.models.Swagger;
import io.swagger.v3.oas.models.OpenAPI;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.concurrent.Callable;

import static com.jd.live.agent.core.util.type.ClassUtils.getDeclaredMethod;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

@Extension(value = "OpenApiRegister", order = AppListener.ORDER_OPEN_API)
@ConditionalOnProperty(GovernanceConfig.CONFIG_MCP_ENABLED)
@ConditionalOnClass("org.springframework.context.ConfigurableApplicationContext")
public class OpenApiRegister extends AppListenerAdapter implements AppBooter {

    private static final String TYPE_OPEN_API31 = "io.swagger.v3.oas.models.annotations.OpenAPI31";
    private static final String TYPE_OPEN_API_RESOURCE = "org.springdoc.api.AbstractOpenApiResource";
    private static final String TYPE_DOCUMENTATION_CACHE = "springfox.documentation.spring.web.DocumentationCache";
    private static final String TYPE_SERVICE_MODEL_TO_SWAGGER2_MAPPER = "springfox.documentation.swagger2.mappers.ServiceModelToSwagger2Mapper";
    private static final String TYPE_DOCUMENTATION = "springfox.documentation.service.Documentation";

    @Override
    public void onStarted(AppContext context) {
        OpenApiFactory.INSTANCE_REF.set(getApiFactory(context));
    }

    private OpenApiFactory getApiFactory(AppContext context) {

        ClassLoader classLoader = context.unwrap().getClass().getClassLoader();
        Class<?> openApiV31Class = loadClass(TYPE_OPEN_API31, classLoader);
        Class<?> openApiResourceClass = loadClass(TYPE_OPEN_API_RESOURCE, classLoader);
        Method getOpenApiMethod = getDeclaredMethod(openApiResourceClass, "getOpenApi", new Class[]{Locale.class});
        Class<?> documentationCacheClass = loadClass(TYPE_DOCUMENTATION_CACHE, classLoader);
        Class<?> serviceModelToSwagger2MapperClass = loadClass(TYPE_SERVICE_MODEL_TO_SWAGGER2_MAPPER, classLoader);
        Class<?> documentationClass = loadClass(TYPE_DOCUMENTATION, classLoader);
        Method documentationByGroupMethod = getDeclaredMethod(documentationCacheClass, "documentationByGroup", new Class[]{String.class});
        Method mapDocumentationMethod = getDeclaredMethod(serviceModelToSwagger2MapperClass, "mapDocumentation", new Class[]{documentationClass});

        try {
            if (openApiResourceClass != null && getOpenApiMethod != null) {
                Object bean = context.getBean(openApiResourceClass);
                Callable<OpenAPI> callable = () -> (OpenAPI) getOpenApiMethod.invoke(bean, Locale.getDefault());
                return openApiV31Class != null ? new Swagger31Factory(callable) : new Swagger30Factory(callable);
            } else if (documentationCacheClass != null && serviceModelToSwagger2MapperClass != null) {
                Object documentCache = context.getBean(documentationCacheClass);
                Object mapper = context.getBean(serviceModelToSwagger2MapperClass);
                if (documentCache != null && mapper != null) {
                    Object document = documentationByGroupMethod.invoke(documentCache, "default");
                    if (document != null) {
                        Callable<Swagger> callable = () -> (Swagger) mapDocumentationMethod.invoke(mapper, document);
                        return new Swagger2Factory(callable);
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
