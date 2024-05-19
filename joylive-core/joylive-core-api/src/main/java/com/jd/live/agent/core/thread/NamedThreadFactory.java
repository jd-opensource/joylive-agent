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
package com.jd.live.agent.core.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A factory class for creating named threads or thread pools. This class implements the {@link ThreadFactory} interface,
 * allowing for customization of thread creation. Threads or thread pools created using this factory can be easily identified
 * by their names, which is particularly useful for debugging or management purposes.
 */
public class NamedThreadFactory implements ThreadFactory {

    /**
     * A global counter for all thread pools created by instances of this class. This counter ensures that each thread pool
     * has a unique identifier.
     */
    protected static final AtomicInteger POOL_COUNTER = new AtomicInteger();

    /**
     * A counter for the threads created by this particular instance of {@code NamedThreadFactory}. This helps in assigning
     * a unique identifier to each thread within the pool.
     */
    protected final AtomicInteger threadCounter = new AtomicInteger(0);

    /**
     * The thread group to which threads created by this factory will belong. Using a thread group allows for easier management
     * of threads, such as setting a maximum priority for all threads in the group.
     */
    protected final ThreadGroup group;

    /**
     * The prefix for the names of threads created by this factory. This prefix is used to easily identify threads belonging
     * to a particular pool or purpose.
     */
    protected final String namePrefix;

    /**
     * Indicates whether threads created by this factory should be daemon threads. Daemon threads are terminated by the JVM
     * when all non-daemon threads finish execution. Setting this to true is useful for background tasks that should not prevent
     * the application from exiting.
     */
    protected final boolean isDaemon;

    /**
     * Constructs a new {@code NamedThreadFactory} instance with the specified prefix for thread names.
     *
     * @param prefix The prefix to be used in the names of threads created by this factory.
     */
    public NamedThreadFactory(String prefix) {
        this(null, prefix, true);
    }

    /**
     * Constructs a new {@code NamedThreadFactory} instance with the specified prefix for thread names and daemon status.
     *
     * @param prefix The prefix to be used in the names of threads created by this factory.
     * @param daemon Indicates whether the threads created by this factory should be daemon threads.
     */
    public NamedThreadFactory(String prefix, boolean daemon) {
        this(null, prefix, daemon);
    }

    /**
     * Constructs a new {@code NamedThreadFactory} instance with the specified thread group, prefix for thread names, and
     * daemon status.
     *
     * @param group  The thread group to which threads created by this factory will belong. If {@code null}, the factory
     *               uses the current thread's {@link ThreadGroup}.
     * @param prefix The prefix to be used in the names of threads created by this factory.
     * @param daemon Indicates whether the threads created by this factory should be daemon threads.
     */
    public NamedThreadFactory(ThreadGroup group, String prefix, boolean daemon) {
        this.group = group == null ? Thread.currentThread().getThreadGroup() : group;
        namePrefix = prefix + "-" + POOL_COUNTER.getAndIncrement() + "-T-";
        isDaemon = daemon;
    }

    /**
     * Creates a new {@link Thread} with the specified {@link Runnable} task and settings defined by this factory. The thread
     * will belong to the thread group, have the name prefix, and be a daemon thread as specified during the factory's
     * construction.
     *
     * @param r The {@link Runnable} task to be executed by the new thread.
     * @return A new {@link Thread} configured according to this factory's settings.
     */
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r, namePrefix + threadCounter.incrementAndGet(), 0);
        t.setDaemon(isDaemon);
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
    }

}

