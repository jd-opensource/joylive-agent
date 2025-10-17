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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.event.Event;
import com.jd.live.agent.core.event.EventHandler;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.event.config.PublisherConfig;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.thread.NamedThreadFactory;
import com.jd.live.agent.core.util.network.Ipv4;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * A publisher that uses the Disruptor framework to handle events in a high-performance, multi-threaded environment.
 * This class is responsible for publishing events to a ring buffer and notifying registered handlers.
 *
 * @param <E> The type of event to be published.
 */
public class DisruptorPublisher<E> implements Publisher<E> {

    private static final Logger logger = LoggerFactory.getLogger(DisruptorPublisher.class);

    private final String topic;

    private final Application application;

    private final PublisherConfig config;

    private final Set<EventHandler<E>> handlers = new CopyOnWriteArraySet<>();

    private final Disruptor<Event<E>> disruptor;

    private final RingBuffer<Event<E>> ringBuffer;

    private final AtomicBoolean started;

    /**
     * Constructs a DisruptorPublisher with the specified topic, application, and configuration.
     * This constructor automatically starts the disruptor.
     *
     * @param topic       The topic for the events.
     * @param application The application context.
     * @param config      The configuration for the publisher.
     */
    public DisruptorPublisher(String topic, Application application, PublisherConfig config) {
        this(topic, application, config, true);
    }

    /**
     * Constructs a DisruptorPublisher with the specified topic, application, configuration, and autoStart flag.
     *
     * @param topic       The topic for the events.
     * @param application The application context.
     * @param config      The configuration for the publisher.
     * @param autoStart   Flag indicating whether the disruptor should be started automatically.
     */
    public DisruptorPublisher(String topic, Application application, PublisherConfig config, boolean autoStart) {
        this.topic = topic;
        this.application = application;
        this.config = config;
        this.started = new AtomicBoolean(autoStart);
        this.disruptor = new Disruptor<>(Event::new, nearestPowerOfTwo(config.getCapacity()),
                new NamedThreadFactory("LiveAgent-publisher-" + topic), ProducerType.MULTI,
                new BlockingWaitStrategy());
        this.disruptor.handleEventsWith(new MyEventHandler());
        this.ringBuffer = autoStart ? disruptor.start() : null;
    }

    /**
     * Returns the nearest power of two for a given integer.
     *
     * @param n The integer to be converted.
     * @return The nearest power of two.
     */
    private int nearestPowerOfTwo(int n) {
        if (n <= 0) {
            return PublisherConfig.DEFAULT_CAPACITY;
        }
        // If n is already a power of two, return n
        if ((n & (n - 1)) == 0) {
            return n;
        }

        int lowerPowerOfTwo = Integer.highestOneBit(n);
        int higherPowerOfTwo = lowerPowerOfTwo << 1;

        // Determine which power of two is closer
        return (n - lowerPowerOfTwo < higherPowerOfTwo - n) ? lowerPowerOfTwo : higherPowerOfTwo;
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
        long timeout = config.getTimeout();
        if (event == null || !started.get()) {
            return false;
        } else if (handlers.isEmpty()) {
            return true;
        } else if (timeout <= 0) {
            return doOffer(event);
        } else {
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < timeout) {
                if (doOffer(event)) {
                    return true;
                }
                LockSupport.parkNanos(1L);
            }
            return false;
        }

    }

    @Override
    public boolean tryOffer(E event) {
        if (event == null || !started.get()) {
            return false;
        } else if (handlers.isEmpty()) {
            return true;
        } else {
            return doOffer(event);
        }
    }

    /**
     * Internal method to offer an event to the ring buffer.
     *
     * @param data The event data to be offered.
     * @return {@code true} if the event was successfully offered, {@code false} otherwise.
     */
    private boolean doOffer(E data) {
        try {
            long sequence = ringBuffer.tryNext();
            Event<E> event = ringBuffer.get(sequence);
            event.setTopic(topic);
            event.setTime(System.currentTimeMillis());
            event.setInstanceId(application.getInstance());
            event.setIp(Ipv4.getLocalIp());
            event.setData(data);
            ringBuffer.publish(sequence);
            return true;
        } catch (InsufficientCapacityException e) {
            return false;
        }
    }

    /**
     * Stops the disruptor and prevents any further events from being published.
     */
    public void stop() {
        if (started.compareAndSet(true, false)) {
            disruptor.shutdown();
        }
    }

    public boolean isStarted() {
        return started.get();
    }

    /**
     * Internal event handler class that processes events in batches.
     */
    private class MyEventHandler implements com.lmax.disruptor.EventHandler<Event<E>> {

        private final List<Event<E>> events;

        private final int batchSize;

        MyEventHandler() {
            batchSize = config.getBatchSize() <= 0 ? PublisherConfig.BATCH_SIZE : config.getBatchSize();
            events = new ArrayList<>(batchSize);
        }

        @Override
        public void onEvent(Event<E> event, long sequence, boolean endOfBatch) {
            events.add(event);
            if (endOfBatch || events.size() == batchSize) {
                try {
                    for (EventHandler<E> handler : handlers) {
                        handler.handle(events);
                    }
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                } finally {
                    events.forEach(Event::clear);
                    events.clear();
                }
            }
        }
    }
}
