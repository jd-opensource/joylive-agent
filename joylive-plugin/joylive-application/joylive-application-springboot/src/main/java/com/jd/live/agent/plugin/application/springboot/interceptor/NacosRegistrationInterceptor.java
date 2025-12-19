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
package com.jd.live.agent.plugin.application.springboot.interceptor;

import com.alibaba.cloud.nacos.registry.NacosAutoServiceRegistration;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.bootstrap.AppContext;
import com.jd.live.agent.core.bootstrap.AppListener.AppListenerAdapter;
import com.jd.live.agent.core.bootstrap.AppListenerSupervisor;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.plugin.application.springboot.util.port.PortDetector;
import com.jd.live.agent.plugin.application.springboot.util.port.PortInfo;
import com.jd.live.agent.plugin.application.springboot.util.port.jmx.JmxPortDetectorFactory;

/**
 * NacosRegistrationInterceptor
 */
public class NacosRegistrationInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(NacosRegistrationInterceptor.class);

    private final AppListenerSupervisor supervisor;

    public NacosRegistrationInterceptor(AppListenerSupervisor supervisor) {
        this.supervisor = supervisor;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        // Fixed the issue that the WAR application cannot automatically register within the Tomcat container.
        supervisor.addFirst(new StartedListener((NacosAutoServiceRegistration) ctx.getTarget(), supervisor));
    }

    private static class StartedListener extends AppListenerAdapter {

        private final NacosAutoServiceRegistration registration;

        private final AppListenerSupervisor supervisor;

        StartedListener(NacosAutoServiceRegistration registration, AppListenerSupervisor supervisor) {
            this.registration = registration;
            this.supervisor = supervisor;
        }

        @Override
        public void onStarted(AppContext appContext) {
            if (registration.isAutoStartup() && !registration.isRunning()) {
                JmxPortDetectorFactory factory = new JmxPortDetectorFactory();
                PortDetector detector = factory.get(appContext);
                try {
                    PortInfo port = detector.getPort();
                    if (port != null) {
                        registration.setPort(port.getPort());
                    }
                    registration.start();
                    logger.info("Success starting nacos registration.");
                } catch (Throwable e) {
                    logger.error("Failed to start nacos registration, caused by " + e.getMessage(), e);
                }
            }
            supervisor.remove(this);
        }
    }
}
