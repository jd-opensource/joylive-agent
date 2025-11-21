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
package com.jd.live.agent.plugin.application.springboot.v2.util;

import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.core.bootstrap.AppContext;
import com.jd.live.agent.core.openapi.spec.v3.OpenApi;
import com.jd.live.agent.plugin.application.springboot.v2.openapi.OpenApiFactory;
import com.jd.live.agent.plugin.application.springboot.v2.openapi.v2.OpenApi2Factory;
import com.jd.live.agent.plugin.application.springboot.v2.openapi.v30.OpenApi30Factory;
import com.jd.live.agent.plugin.application.springboot.v2.openapi.v31.OpenApi31Factory;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.PortDetector;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.PortDetectorFactory;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.PortInfo;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.env.EnvPortDetectorFactory;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.jmx.JmxPortDetectorFactory;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.web.WebPortDetectorFactory;
import org.springframework.core.io.ResourceLoader;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;
import static com.jd.live.agent.core.util.type.ClassUtils.getDeclaredMethod;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

public class SpringUtils {

    private static final String TYPE_LIVE_RELOAD_SERVER = "org.springframework.boot.devtools.livereload.LiveReloadServer";
    private static final String THREAD_NAME = "restartedMain";
    private static final ClassLoader CLASS_LOADER = ResourceLoader.class.getClassLoader();
    private static final Class<?> CLASS_LIVE_RELOAD_SERVER = loadClass(TYPE_LIVE_RELOAD_SERVER, CLASS_LOADER);

    private static final String TYPE_CONFIGURABLE_WEB_ENVIRONMENT = "org.springframework.web.context.ConfigurableWebEnvironment";
    private static final Class<?> CLASS_CONFIGURABLE_WEB_ENVIRONMENT = loadClass(TYPE_CONFIGURABLE_WEB_ENVIRONMENT, CLASS_LOADER);
    private static final Method METHOD_INIT_PROPERTY_SOURCES = getDeclaredMethod(CLASS_CONFIGURABLE_WEB_ENVIRONMENT, "initPropertySources");
    private static final String TYPE_JAVAX_SERVLET_CONTEXT = "javax.servlet.ServletContext";

    // spring boot 4.0
    private static final String TYPE_CONFIGURABLE_REACTIVE_WEB_ENVIRONMENT4 = "org.springframework.boot.web.context.reactive.ConfigurableReactiveWebEnvironment";
    private static final Class<?> CLASS_CONFIGURABLE_REACTIVE_WEB_ENVIRONMENT4 = loadClass(TYPE_CONFIGURABLE_REACTIVE_WEB_ENVIRONMENT4, CLASS_LOADER);
    // spring boot 2+/3+
    private static final String TYPE_CONFIGURABLE_REACTIVE_WEB_ENVIRONMENT3 = "org.springframework.boot.web.reactive.context.ConfigurableReactiveWebEnvironment";
    private static final Class<?> CLASS_CONFIGURABLE_REACTIVE_WEB_ENVIRONMENT3 = loadClass(TYPE_CONFIGURABLE_REACTIVE_WEB_ENVIRONMENT3, CLASS_LOADER);

    private static final String TYPE_OPEN_API_V3 = "io.swagger.v3.oas.models.OpenAPI";
    private static final Class<?> CLASS_OPEN_API_V3 = loadClass(TYPE_OPEN_API_V3, CLASS_LOADER);
    private static final String TYPE_OPEN_API31 = "io.swagger.v3.oas.models.annotations.OpenAPI31";
    private static final Class<?> CLASS_OPEN_API_V31 = loadClass(TYPE_OPEN_API31, CLASS_LOADER);
    private static final String TYPE_OPEN_API2 = "io.swagger.models.Swagger";
    private static final Class<?> CLASS_OPEN_API_V2 = loadClass(TYPE_OPEN_API2, CLASS_LOADER);
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
    private static final String TYPE_PARAMETER_NAME_DISCOVERER = "org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory";
    private static final Class<?> CLASS_PARAMETER_NAME_DISCOVERER = loadClass(TYPE_PARAMETER_NAME_DISCOVERER, CLASS_LOADER);
    private static final FieldAccessor ACCESSOR_PARAMETER_NAME_DISCOVERER = getAccessor(CLASS_PARAMETER_NAME_DISCOVERER, "parameterNameDiscoverer");

    /**
     * Checks if current thread is a development reload thread
     */
    public static boolean isDevThread() {
        return CLASS_LIVE_RELOAD_SERVER != null && THREAD_NAME.equals(Thread.currentThread().getName());
    }

    /**
     * Checks if the environment is a web environment.
     *
     * @param environment Environment to check
     * @return true if web environment, false otherwise
     */
    public static boolean isWeb(Object environment) {
        return CLASS_CONFIGURABLE_WEB_ENVIRONMENT != null && CLASS_CONFIGURABLE_WEB_ENVIRONMENT.isInstance(environment);
    }

    /**
     * Checks if the environment is a WebFlux environment.
     *
     * @param environment Environment to check
     * @return true if WebFlux environment, false otherwise
     */
    public static boolean isWebFlux(Object environment) {
        return CLASS_CONFIGURABLE_REACTIVE_WEB_ENVIRONMENT4 != null && CLASS_CONFIGURABLE_REACTIVE_WEB_ENVIRONMENT4.isInstance(environment)
                || CLASS_CONFIGURABLE_REACTIVE_WEB_ENVIRONMENT3 != null && CLASS_CONFIGURABLE_REACTIVE_WEB_ENVIRONMENT3.isInstance(environment);
    }

    /**
     * Checks if javax.servlet API is available.
     *
     * @return true if javax.servlet is available, false otherwise
     */
    public static boolean isJavaxServlet() {
        return METHOD_INIT_PROPERTY_SOURCES != null && METHOD_INIT_PROPERTY_SOURCES.getParameterTypes()[0].getName().equals(TYPE_JAVAX_SERVLET_CONTEXT);
    }

    /**
     * Detects server port using prioritized strategies:
     * 1. WebServer port for Servlet contexts
     * 2. PortDetector plugin mechanism
     * 3. server.port property fallback
     * Enforces valid port range (1-65535) and handles parsing errors (default:8080)
     *
     * @param context Application context for port detection
     * @return Validated port number
     */
    public static PortInfo getPort(AppContext context) {
        PortDetectorFactory[] factories = new PortDetectorFactory[]{
                new WebPortDetectorFactory(),
                new JmxPortDetectorFactory(),
                new EnvPortDetectorFactory(),
        };
        for (PortDetectorFactory factory : factories) {
            PortDetector detector = factory.get(context);
            if (detector != null) {
                PortInfo portInfo = detector.getPort();
                if (portInfo != null) {
                    return portInfo;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getParameterNameDiscoverer(Object factory) {
        return (T) ACCESSOR_PARAMETER_NAME_DISCOVERER.get(factory);
    }

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

    public static Supplier<OpenApi> getOpenApi(AppContext context) {
        if (CLASS_OPEN_API_RESOURCE != null && METHOD_GET_OPEN_API != null) {
            try {
                Object bean = context.getBean(CLASS_OPEN_API_RESOURCE);
                OpenApiFactory factory = CLASS_OPEN_API_V31 != null ? new OpenApi31Factory() : new OpenApi30Factory();
                return () -> {
                    try {
                        Object object = METHOD_GET_OPEN_API.invoke(bean, Locale.getDefault());
                        return factory.create(object);
                    } catch (Throwable e) {
                        return null;
                    }
                };
            } catch (Throwable ignore) {
            }
        } else if (CLASS_DOCUMENTATION_CACHE != null && CLASS_SERVICE_MODEL_TO_SWAGGER2_MAPPER != null) {
            try {
                Object documentCache = context.getBean(CLASS_DOCUMENTATION_CACHE);
                Object mapper = context.getBean(CLASS_SERVICE_MODEL_TO_SWAGGER2_MAPPER);
                if (documentCache != null && mapper != null) {
                    return () -> {
                        try {
                            Object document = METHOD_DOCUMENTATION_BY_GROUP.invoke(documentCache, "default");
                            if (document != null) {
                                Object swagger = METHOD_MAP_DOCUMENTATION.invoke(mapper, document);
                                OpenApi2Factory factory = new OpenApi2Factory();
                                return factory.create(swagger);
                            }
                        } catch (Throwable ignore) {
                        }
                        return null;
                    };
                }
            } catch (Throwable ignore) {
            }

        }
        return null;
    }
}
