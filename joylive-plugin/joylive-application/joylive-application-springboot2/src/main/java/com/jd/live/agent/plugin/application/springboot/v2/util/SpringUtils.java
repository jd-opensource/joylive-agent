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
import com.jd.live.agent.plugin.application.springboot.v2.util.port.PortDetector;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.PortDetectorFactory;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.PortInfo;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.env.EnvPortDetectorFactory;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.jmx.JmxPortDetectorFactory;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.web.WebPortDetectorFactory;
import org.springframework.core.io.ResourceLoader;

import java.lang.reflect.Method;

import static com.jd.live.agent.core.util.type.ClassUtils.getDeclaredMethod;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

public class SpringUtils {

    private static final String TYPE_LIVE_RELOAD_SERVER = "org.springframework.boot.devtools.livereload.LiveReloadServer";
    private static final String THREAD_NAME = "restartedMain";
    private static final Class<?> CLASS_LIVE_RELOAD_SERVER = loadClass(TYPE_LIVE_RELOAD_SERVER, ResourceLoader.class.getClassLoader());

    private static final String TYPE_CONFIGURABLE_WEB_ENVIRONMENT = "org.springframework.web.context.ConfigurableWebEnvironment";
    private static final Class<?> CLASS_CONFIGURABLE_WEB_ENVIRONMENT = loadClass(TYPE_CONFIGURABLE_WEB_ENVIRONMENT, ResourceLoader.class.getClassLoader());
    private static final Method METHOD_INIT_PROPERTY_SOURCES = getDeclaredMethod(CLASS_CONFIGURABLE_WEB_ENVIRONMENT, "initPropertySources");
    private static final String TYPE_JAVAX_SERVLET_CONTEXT = "javax.servlet.ServletContext";

    // spring boot 4.0
    private static final String TYPE_CONFIGURABLE_REACTIVE_WEB_ENVIRONMENT4 = "org.springframework.boot.web.context.reactive.ConfigurableReactiveWebEnvironment";
    private static final Class<?> CLASS_CONFIGURABLE_REACTIVE_WEB_ENVIRONMENT4 = loadClass(TYPE_CONFIGURABLE_REACTIVE_WEB_ENVIRONMENT4, ResourceLoader.class.getClassLoader());
    // spring boot 2+/3+
    private static final String TYPE_CONFIGURABLE_REACTIVE_WEB_ENVIRONMENT3 = "org.springframework.boot.web.reactive.context.ConfigurableReactiveWebEnvironment";
    private static final Class<?> CLASS_CONFIGURABLE_REACTIVE_WEB_ENVIRONMENT3 = loadClass(TYPE_CONFIGURABLE_REACTIVE_WEB_ENVIRONMENT3, ResourceLoader.class.getClassLoader());

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
}
