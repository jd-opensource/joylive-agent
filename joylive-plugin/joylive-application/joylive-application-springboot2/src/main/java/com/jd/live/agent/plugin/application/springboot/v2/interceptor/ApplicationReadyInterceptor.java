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
package com.jd.live.agent.plugin.application.springboot.v2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory;
import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.bootstrap.AppListener;
import com.jd.live.agent.core.instance.AppService;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.network.Ipv4;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceInstance;
import com.jd.live.agent.plugin.application.springboot.v2.context.SpringAppContext;
import com.jd.live.agent.plugin.application.springboot.v2.listener.InnerListener;
import com.jd.live.agent.plugin.application.springboot.v2.util.AppLifecycle;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.PortDetector;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.PortDetectorFactory;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.PortInfo;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.HashMap;
import java.util.Map;

public class ApplicationReadyInterceptor extends InterceptorAdaptor {

    private final AppListener listener;

    private final GovernanceConfig config;

    private final Registry registry;

    private final Application application;

    public ApplicationReadyInterceptor(AppListener listener, GovernanceConfig config, Registry registry, Application application) {
        this.listener = listener;
        this.config = config;
        this.registry = registry;
        this.application = application;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        SpringAppContext context = new SpringAppContext(ctx.getArgument(0));
        // fix for spring boot 2.1, it will trigger twice.
        AppLifecycle.ready(() -> {
            if (config.getRegistryConfig().isEnabled()) {
                registry.register(createInstance(context.getContext(), application.getService()));
            }
            InnerListener.foreach(l -> l.onReady(context));
            listener.onReady(context);
        });
    }

    /**
     * Creates a service instance with configuration derived from the application context.
     * Automatically detects host and port (using multiple strategies), collects application
     * labels as metadata, and embeds framework version information.
     *
     * @param context    Application context for environment and port detection
     * @param appService Service metadata provider
     * @return Configured service instance ready for registration
     */
    private ServiceInstance createInstance(ConfigurableApplicationContext context, AppService appService) {
        ConfigurableEnvironment environment = context.getEnvironment();
        String address = environment.getProperty("server.address");
        address = address == null || address.isEmpty() ? Ipv4.getLocalIp() : address;
        PortInfo port = getPort(context);
        ServiceInstance instance = new ServiceInstance();
        instance.setInstanceId(application.getInstance());
        instance.setNamespace(appService.getNamespace());
        instance.setService(appService.getName());
        instance.setGroup(appService.getGroup());
        instance.setHost(address);
        Map<String, String> metadata = new HashMap<>();
        application.labelRegistry(metadata::putIfAbsent, true);
        metadata.put(Constants.LABEL_FRAMEWORK, "spring-boot-" + SpringBootVersion.getVersion());
        if (port != null) {
            instance.setPort(port.getPort());
            if (port.isSecure()) {
                metadata.put(Constants.LABEL_SECURE, String.valueOf(port.isSecure()));
            }
        }
        instance.setMetadata(metadata);
        return instance;
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
    private PortInfo getPort(ConfigurableApplicationContext context) {
        if (context instanceof WebServerApplicationContext) {
            return getPortByWebServer((WebServerApplicationContext) context);
        } else {
            PortInfo port = getPortByDetector(context);
            if (port != null) {
                return port;
            }
            return getPortByEnvironment(context);
        }
    }

    /**
     * Retrieves port directly from embedded WebServer instance (Servlet contexts only)
     *
     * @param context Web-enabled application context
     * @return Actual bound port from web server
     */
    private PortInfo getPortByWebServer(WebServerApplicationContext context) {
        WebServer webServer = context.getWebServer();
        int port = webServer.getPort();
        boolean secure = false;
        ClassLoader classLoader = context.getClassLoader();
        classLoader = classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader;
        try {
            Class<?> clazz = classLoader.loadClass("org.springframework.boot.autoconfigure.web.ServerProperties");
            Object bean = context.getBean(clazz);
            secure = UnsafeFieldAccessorFactory.getQuietly(bean, "ssl") != null;
        } catch (Throwable ignored) {
        }
        return new PortInfo(port, secure);
    }

    /**
     * Extracts port from environment properties with validation.
     * Handles missing/invalid values by falling back to 8080.
     *
     * @param context Application context containing environment
     * @return Validated port number from configuration
     */
    private PortInfo getPortByEnvironment(ConfigurableApplicationContext context) {
        ConfigurableEnvironment environment = context.getEnvironment();
        String serverPort = environment.getProperty("server.port");
        serverPort = serverPort == null || serverPort.isEmpty() ? "8080" : serverPort;
        int port;
        try {
            port = Integer.parseInt(serverPort);
            port = port > 65535 || port <= 0 ? 8080 : port;
        } catch (NumberFormatException e) {
            port = 8080;
        }
        return new PortInfo(port, false);
    }

    /**
     * Attempts port detection through plugin mechanism.
     * Silently ignores detector failures to allow fallback strategies.
     *
     * @param context Application context for detector initialization
     * @return Detected port or null if unavailable
     */
    private PortInfo getPortByDetector(ConfigurableApplicationContext context) {
        PortDetector detector = PortDetectorFactory.get(context);
        try {
            return detector.getPort();
        } catch (Throwable e) {
            return null;
        }
    }
}
