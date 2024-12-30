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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.event.Event;
import com.jd.live.agent.core.event.EventHandler;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.event.config.PublisherConfig;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.util.network.Ipv4;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class JPublisher<E> implements Publisher<E> {

    private static final Logger logger = LoggerFactory.getLogger(JPublisher.class);

    private final String topic;

    private final Application application;

    private final PublisherConfig config;

    private final Set<EventHandler<E>> handlers = new CopyOnWriteArraySet<>();

    private final BlockingQueue<Event<E>> queue;

    private final Thread thread;

    private final AtomicBoolean started;

    public JPublisher(String topic, Application application, PublisherConfig config) {
        this(topic, application, config, true);
    }

    public JPublisher(String topic, Application application, PublisherConfig config, boolean autoStart) {
        this.topic = topic;
        this.application = application;
        this.config = config;
        this.started = new AtomicBoolean(autoStart);
        this.queue = new LinkedBlockingQueue<>(config.getCapacity() > 0 ? config.getCapacity() : PublisherConfig.DEFAULT_CAPACITY);
        this.thread = new Thread(this::run, "LiveAgent-publisher-" + topic);
        if (autoStart) {
            thread.start();
        }
    }

    protected void run() {
        int batchSize = config.getBatchSize() <= 0 ? PublisherConfig.BATCH_SIZE : config.getBatchSize();
        List<Event<E>> events = new ArrayList<>(batchSize + 2);
        Event<E> event;
        while (isStarted() && !Thread.currentThread().isInterrupted()) {
            events.clear();
            if (queue.isEmpty()) {
                try {
                    event = queue.poll(3000, TimeUnit.MILLISECONDS);
                    if (event != null) {
                        events.add(event);
                        if (!queue.isEmpty() && batchSize > 1) {
                            queue.drainTo(events, batchSize - 1);
                        }
                    }
                } catch (InterruptedException ignore) {
                }
            } else {
                queue.drainTo(events, batchSize);
            }
            if (isStarted() && !events.isEmpty()) {
                for (EventHandler<E> handler : handlers) {
                    try {
                        handler.handle(events);
                    } catch (Throwable e) {
                        logger.error("Failed to handle event of " + topic + ", caused by " + e.getMessage(), e);
                    }
                }
            }
        }
    }

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public boolean addHandler(EventHandler<E> handler) {
        return handler != null && handlers.add(handler);
    }

    @Override
    public boolean removeHandler(EventHandler<E> handler) {
        return handler != null && handlers.remove(handler);
    }

    @Override
    public boolean offer(E event) {
        return offer(event, config.getTimeout());
    }

    @Override
    public boolean tryOffer(E event) {
        return offer(event, 0);
    }

    private boolean offer(E event, long timeout) {
        if (event == null || !started.get()) {
            return false;
        } else if (handlers.isEmpty()) {
            return true;
        }
        Event<E> newEvent = new Event<>(event);
        newEvent.setTopic(topic);
        newEvent.setTime(System.currentTimeMillis());
        newEvent.setInstanceId(application.getInstance());
        newEvent.setIp(Ipv4.getLocalIp());
        try {
            return timeout <= 0 ? queue.offer(newEvent) : queue.offer(newEvent, timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    public void stop() {
        if (started.compareAndSet(true, false)) {
            queue.clear();
            thread.interrupt();
        }
    }

    public boolean isStarted() {
        return started.get();
    }
}
