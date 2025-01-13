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

import com.jd.live.agent.core.bootstrap.ApplicationListener.ApplicationListenerAdapter;
import com.jd.live.agent.core.config.Configurator;
import com.jd.live.agent.core.event.AgentEvent;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;

/**
 * An extension of the ApplicationListenerAdapter that publishes events to a Publisher when the application starts, is ready, or stops.
 *
 * @since 1.6.0
 */
@Injectable
@Extension(value = "ApplicationBootstrapListener", order = ApplicationListener.ORDER_BOOTSTRAP)
public class ApplicationBootstrapListener extends ApplicationListenerAdapter {

    @Inject(Publisher.SYSTEM)
    private Publisher<AgentEvent> publisher;

    @Inject(value = Configurator.COMPONENT_CONFIGURATOR, component = true, nullable = true)
    private Configurator configurator;

    @Override
    public void onEnvironmentPrepared(ApplicationBootstrapContext context, ApplicationEnvironment environment) {
        if (configurator != null) {
            environment.addFirst(new LivePropertySource());
        }
    }

    @Override
    public void onStarted(ApplicationContext context) {
        publisher.offer(AgentEvent.onApplicationStarted("Application is started"));
    }

    @Override
    public void onReady(ApplicationContext context) {
        publisher.offer(AgentEvent.onApplicationReady("Application is ready"));
    }

    @Override
    public void onStop(ApplicationContext context) {
        publisher.offer(AgentEvent.onApplicationStop("Application is stopping"));
    }

    private class LivePropertySource implements ApplicationPropertySource {
        @Override
        public String getProperty(String name) {
            return configurator.getProperty(name);
        }

        @Override
        public String getName() {
            return "LiveConfigurator";
        }
    }
}
