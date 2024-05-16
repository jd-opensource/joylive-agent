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
package com.jd.live.agent.plugin.transmission.thread.config;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class ThreadConfig {

    public static final String CONFIG_THREAD_PREFIX = "agent.governance.transmission.thread";

    private static final String[] EXCLUDE_EXECUTOR_CLASSES = new String[]{
            "org.apache.dubbo.common.threadpool.ThreadlessExecutor",
            "org.apache.tomcat.util.threads.ThreadPoolExecutor",
            "org.apache.tomcat.util.threads.ScheduledThreadPoolExecutor",
            "org.apache.tomcat.util.threads.InlineExecutorService",
            "javax.management.NotificationBroadcasterSupport$1",
            "io.grpc.stub.ClientCalls.ThreadlessExecutor",
            "io.grpc.SynchronizationContext",
            "io.netty.channel.nio.NioEventLoopGroup",
            "io.netty.channel.MultithreadEventLoopGroup",
            "io.netty.channel.nio.NioEventLoop",
            "io.netty.channel.SingleThreadEventLoop",
            "io.netty.channel.kqueue.KQueueEventLoopGroup",
            "io.netty.channel.kqueue.KQueueEventLoop",
            "io.netty.util.concurrent.MultithreadEventExecutorGroup",
            "io.netty.util.concurrent.AbstractEventExecutorGroup",
            "io.netty.util.concurrent.ThreadPerTaskExecutor",
            "io.netty.util.concurrent.GlobalEventExecutor",
            "io.netty.util.concurrent.AbstractScheduledEventExecutor",
            "io.netty.util.concurrent.AbstractEventExecutor",
            "io.netty.util.concurrent.SingleThreadEventExecutor",
            "io.netty.util.concurrent.DefaultEventExecutor",
            "io.netty.util.internal.ThreadExecutorMap$1",
            "reactor.core.scheduler.BoundedElasticScheduler$BoundedScheduledExecutorService",
            "reactor.netty.resources.ColocatedEventLoopGroup",
            "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoopGroup",
            "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.MultithreadEventLoopGroup",
            "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.concurrent.MultithreadEventExecutorGroup",
            "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.concurrent.AbstractEventExecutorGroup",
            "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.concurrent.ThreadPerTaskExecutor",
            "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.concurrent.GlobalEventExecutor",
            "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.concurrent.AbstractScheduledEventExecutor",
            "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.concurrent.AbstractEventExecutor",
            "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoop",
            "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.SingleThreadEventLoop",
            "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.concurrent.SingleThreadEventExecutor",
            "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.internal.ThreadExecutorMap$1",
            "com.alibaba.nacos.shaded.io.grpc.internal.ManagedChannelImpl$ExecutorHolder",
            "com.alibaba.nacos.shaded.io.grpc.internal.ManagedChannelImpl$RestrictedScheduledExecutor",
            "com.alibaba.nacos.shaded.io.grpc.internal.ManagedChannelImpl$2",
            "com.alibaba.nacos.shaded.io.grpc.internal.SerializingExecutor",
            "com.alibaba.nacos.shaded.io.grpc.stub.ClientCalls.ThreadlessExecutor",
            "com.alibaba.nacos.shaded.io.grpc.SynchronizationContext",
            "com.alibaba.nacos.shaded.com.google.common.util.concurrent.DirectExecutor"
    };

    private static final String[] EXCLUDE_TASK_CLASSES = new String[]{
            "com.alibaba.nacos.shaded.io.grpc.internal.DnsNameResolver.Resolve",
    };

    private Set<String> excludeExecutors = new HashSet<>(Arrays.asList(EXCLUDE_EXECUTOR_CLASSES));

    private Set<String> excludeTasks = new HashSet<>(Arrays.asList(EXCLUDE_TASK_CLASSES));

    public boolean isExcludedExecutor(String name) {
        return name == null || excludeExecutors.contains(name);
    }

    public boolean isExcludedTask(String name) {
        return name == null || excludeTasks.contains(name);
    }
}
