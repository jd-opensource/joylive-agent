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

import com.alibaba.cloud.nacos.registry.NacosAutoServiceRegistration;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.bootstrap.AppContext;
import com.jd.live.agent.core.bootstrap.AppListener.AppListenerAdapter;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.plugin.application.springboot.v2.context.SpringAppContext;
import com.jd.live.agent.plugin.application.springboot.v2.listener.InnerListener;
import org.springframework.context.ConfigurableApplicationContext;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.Query;
import java.lang.management.ManagementFactory;
import java.util.Set;

/**
 * NacosRegistrationInterceptor
 */
public class NacosRegistrationInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(NacosRegistrationInterceptor.class);

    @Override
    public void onSuccess(ExecutableContext ctx) {
        // Fixed the issue that the WAR application cannot automatically register within the Tomcat container.
        NacosAutoServiceRegistration registration = (NacosAutoServiceRegistration) ctx.getTarget();
        InnerListener.add(new AppListenerAdapter() {
            @Override
            public void onStarted(AppContext appContext) {
                ConfigurableApplicationContext context = ((SpringAppContext) appContext).getContext();
                if (registration.isAutoStartup() && !registration.isRunning()) {
                    PortDetector detector = PortDetectorFactory.get(context);
                    try {
                        Integer port = detector.getPort();
                        if (port != null) {
                            registration.setPort(port);
                        }
                        registration.start();
                    } catch (Throwable e) {
                        logger.error("Failed to start nacos registration, caused by " + e.getMessage(), e);
                    }
                }
                InnerListener.remove(this);
            }
        });
    }

    /**
     * An interface that defines a method to get the port number.
     */
    private interface PortDetector {
        /**
         * Returns the port number.
         *
         * @return The port number as an Integer, or null if the port cannot be determined.
         * @throws Throwable If an error occurs while trying to determine the port.
         */
        Integer getPort() throws Throwable;
    }

    /**
     * A class that implements the PortDetector interface by using JMX to determine the port.
     */
    private static class JmxPortDetector implements PortDetector {

        @Override
        public Integer getPort() throws Throwable {
            MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
            Set<ObjectName> objectNames = beanServer.queryNames(new ObjectName("*:type=Connector,*"), Query.match(Query.attr("protocol"), Query.value("HTTP/1.1")));
            String value = objectNames.isEmpty() ? null : objectNames.iterator().next().getKeyProperty("port");
            return value == null ? null : Integer.valueOf(value);
        }
    }

    /**
     * A factory class that provides a PortDetector instance based on the given ConfigurableApplicationContext.
     */
    private static class PortDetectorFactory {

        /**
         * Returns a PortDetector instance based on the given ConfigurableApplicationContext.
         *
         * @param context The ConfigurableApplicationContext.
         * @return A PortDetector instance.
         */
        public static PortDetector get(ConfigurableApplicationContext context) {
            return new JmxPortDetector();
        }

    }

}
