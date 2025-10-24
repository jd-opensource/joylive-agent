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

import com.jd.live.agent.core.bootstrap.AppContext;
import com.jd.live.agent.plugin.application.springboot.v2.util.param.CompositeSystemParameterFactory;
import com.jd.live.agent.governance.mcp.ParameterParser;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.PortDetector;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.PortDetectorFactory;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.PortInfo;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.env.EnvPortDetectorFactory;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.jmx.JmxPortDetectorFactory;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.web.WebPortDetectorFactory;
import org.springframework.core.io.ResourceLoader;
import reactor.core.publisher.Mono;

import java.lang.reflect.Parameter;
import java.util.function.Function;

import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

public class SpringUtils {

    private static final String TYPE_LIVE_RELOAD_SERVER = "org.springframework.boot.devtools.livereload.LiveReloadServer";
    private static final String THREAD_NAME = "restartedMain";
    private static final Class<?> CLASS_LIVE_RELOAD_SERVER = loadClass(TYPE_LIVE_RELOAD_SERVER, ResourceLoader.class.getClassLoader());
    private static final String TYPE_REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController";
    private static final Class<?> CLASS_REST_CONTROLLER = loadClass(TYPE_REST_CONTROLLER, ResourceLoader.class.getClassLoader());
    private static final String TYPE_DISPATCHER_SERVLET = "org.springframework.web.servlet.DispatcherServlet";
    private static final Class<?> CLASS_DISPATCHER_SERVLET = loadClass(TYPE_DISPATCHER_SERVLET, ResourceLoader.class.getClassLoader());
    private static final String TYPE_MONO = "reactor.core.publisher.Mono";
    private static final Class<?> CLASS_MONO = loadClass(TYPE_MONO, ResourceLoader.class.getClassLoader());

    private static final String ERROR_CONTROLLER_TYPE = "org.springframework.boot.web.servlet.error.ErrorController";
    private static final Class<?> ERROR_CONTROLLER_CLASS = loadClass(ERROR_CONTROLLER_TYPE, ResourceLoader.class.getClassLoader());
    private static final String API_RESOURCE_CONTROLLER_TYPE = "springfox.documentation.swagger.web.ApiResourceController";
    private static final Class<?> API_RESOURCE_CONTROLLER_CLASS = loadClass(API_RESOURCE_CONTROLLER_TYPE, ResourceLoader.class.getClassLoader());
    private static final String SWAGGER2_CONTROLLER_WEB_MVC_TYPE = "springfox.documentation.swagger2.web.Swagger2ControllerWebMvc";
    private static final Class<?> SWAGGER2_CONTROLLER_WEB_MVC_CLASS = loadClass(SWAGGER2_CONTROLLER_WEB_MVC_TYPE, ResourceLoader.class.getClassLoader());

    /**
     * Checks if current thread is a development reload thread
     */
    public static boolean isDevThread() {
        return CLASS_LIVE_RELOAD_SERVER != null && THREAD_NAME.equals(Thread.currentThread().getName());
    }

    /**
     * Checks if Spring Web is enabled
     */
    public static boolean isWebEnabled() {
        return CLASS_REST_CONTROLLER != null;
    }

    /**
     * Checks if Spring MVC is enabled
     */
    public static boolean isWebMvcEnabled() {
        return CLASS_DISPATCHER_SERVLET != null;
    }

    /**
     * Gets converter function for Mono type
     *
     * @param type The class type to check
     * @return Function to convert object to Mono, or null if not applicable
     */
    public static Function<Object, Object> getMonoConverter(Class<?> type) {
        return type != null && type == CLASS_MONO ? obj -> Mono.just(obj) : null;
    }

    /**
     * Checks if controller is a system-level controller
     *
     * @param controller The controller instance to check
     */
    public static boolean isSystemController(Object controller) {
        return ERROR_CONTROLLER_CLASS != null && ERROR_CONTROLLER_CLASS.isInstance(controller)
                || API_RESOURCE_CONTROLLER_CLASS != null && API_RESOURCE_CONTROLLER_CLASS.isInstance(controller)
                || SWAGGER2_CONTROLLER_WEB_MVC_CLASS != null && SWAGGER2_CONTROLLER_WEB_MVC_CLASS.isInstance(controller);
    }

    /**
     * Gets system parameter parser for given parameter
     *
     * @param parameter Method parameter to get supplier for
     * @return Supplier for system objects like HttpServletRequest/Response
     */
    public static ParameterParser getParser(Parameter parameter) {
        return CompositeSystemParameterFactory.INSTANCE.getParser(parameter);
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
}
