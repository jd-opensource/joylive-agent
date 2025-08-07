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
package com.jd.live.agent.core.bootstrap;

import com.jd.live.agent.bootstrap.classloader.LiveClassLoader;
import com.jd.live.agent.core.bootstrap.AppListener.AppListenerAdapter;
import com.jd.live.agent.core.event.AgentEvent;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Application;

/**
 * An extension of the ApplicationListenerAdapter that publishes events to a Publisher when the application starts, is ready, or stops.
 *
 * @since 1.6.0
 */
@Injectable
@Extension(value = "ApplicationBootstrapListener", order = AppListener.ORDER_BOOTSTRAP)
public class AppBootstrapListener extends AppListenerAdapter {

    @Inject(Publisher.SYSTEM)
    private Publisher<AgentEvent> publisher;

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Override
    public void onLoading(ClassLoader classLoader, Class<?> mainClass) {
        application.setClassLoader(classLoader);
        LiveClassLoader.BOOT_CLASS_LOADER = classLoader;
        application.setMainClass(mainClass);
        publisher.offer(AgentEvent.onApplicationLoading("Application is loading"));
    }

    @Override
    public void onStarted(AppContext context) {
        publisher.offer(AgentEvent.onApplicationStarted("Application is started"));
    }

    @Override
    public void onReady(AppContext context) {
        publisher.offer(AgentEvent.onApplicationReady("Application is ready"));
    }

    @Override
    public void onCLose(AppContext context) {
        publisher.offer(AgentEvent.onApplicationCLose("Application is closing"));
    }

}
