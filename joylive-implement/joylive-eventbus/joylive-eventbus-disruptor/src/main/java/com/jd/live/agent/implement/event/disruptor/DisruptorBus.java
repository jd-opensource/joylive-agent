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
package com.jd.live.agent.implement.event.disruptor;

import com.jd.live.agent.core.event.EventBus;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.event.config.PublisherConfig;
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
 * DisruptorBus
 */
@Injectable
@Extension(value = "DisruptorBus", order = EventBus.ORDER_DISRUPTOR_BUS)
@Configurable(prefix = "agent.publisher")
public class DisruptorBus implements EventBus {

    public static final String DEFAULT_NAME = "default";

    @Inject
    private Application application;

    @Config
    private Map<String, PublisherConfig> configs = new ConcurrentHashMap<>();

    private final Map<String, DisruptorPublisher<?>> publishers = new ConcurrentHashMap<>();

    private final AtomicBoolean started = new AtomicBoolean(true);

    public DisruptorBus() {
    }

    public DisruptorBus(Application application, Map<String, PublisherConfig> configs) {
        this.application = application;
        this.configs = configs;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> Publisher<E> getPublisher(String topic) {
        if (topic == null || topic.isEmpty())
            return null;
        DisruptorPublisher<E> result = (DisruptorPublisher<E>) publishers.computeIfAbsent(topic,
                o -> new DisruptorPublisher<>(topic, application, getConfig(o), started.get()));
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
            publishers.values().forEach(DisruptorPublisher::stop);
        }
    }
}
