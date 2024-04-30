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
package com.jd.live.agent.implement.event.jbus;

import com.jd.live.agent.core.event.EventBus;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Configurable;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Application;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * EventBus
 */
@Injectable
@Extension(value = "JEventBus", order = EventBus.ORDER_JEVENT_BUS)
@Configurable(prefix = "agent.publisher", auto = false)
public class JEventBus implements EventBus {

    public static final String DEFAULT_NAME = "default";

    @Inject
    private Application application;

    @Config
    private Map<String, PublisherConfig> configs = new ConcurrentHashMap<>();

    private final Map<String, JPublisher<?>> publishers = new ConcurrentHashMap<>();

    private final AtomicBoolean started = new AtomicBoolean(true);

    public JEventBus() {
    }

    public JEventBus(Application application, Map<String, PublisherConfig> configs) {
        this.application = application;
        this.configs = configs;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> Publisher<E> getPublisher(String topic) {
        if (topic == null || topic.isEmpty())
            return null;
        JPublisher<E> result = (JPublisher<E>) publishers.computeIfAbsent(topic,
                o -> new JPublisher<>(topic, application, getConfig(o), started.get()));
        if (!started.get() && result.isStarted()) {
            result.stop();
        }
        return result;
    }

    protected PublisherConfig getConfig(String topic) {
        PublisherConfig config = configs.get(topic);
        if (config == null) {
            config = configs.get(DEFAULT_NAME);
            if (config == null)
                config = new PublisherConfig();
        }
        return config;
    }

    public void stop() {
        if (started.compareAndSet(true, false)) {
            publishers.values().forEach(JPublisher::stop);
        }
    }
}
