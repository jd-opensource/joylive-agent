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
package com.jd.live.agent.implement.flowcontrol.ratelimit.redisson.client;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.implement.flowcontrol.ratelimit.redisson.RedissonRateLimiter;
import lombok.Getter;
import lombok.Setter;
import org.redisson.Redisson;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.TransportMode;
import org.redisson.connection.ConnectionListener;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Represents a Redis client managed by {@link RedisClientManager}.
 */
public class RedisClient implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(RedissonRateLimiter.class);

    private final RedisConfig config;

    private final Consumer<RedisClient> consumer;

    private volatile RedissonClient delegate;

    @Getter
    @Setter
    private long lastAccessTime;

    private final AtomicLong counter = new AtomicLong(0);

    @Getter
    private volatile boolean connected;

    public RedisClient(RedisConfig config, Consumer<RedisClient> consumer) {
        this.config = config;
        this.consumer = consumer;
    }

    public long getId() {
        return config.getId();
    }

    /**
     * Retrieves a rate limiter for the specified key.
     *
     * @param key the key for the rate limiter
     * @return the rate limiter for the specified key, or null if the delegate is not initialized
     */
    public RRateLimiter getRateLimiter(String key) {
        return delegate == null ? null : delegate.getRateLimiter(key);
    }

    /**
     * Checks if the Redis client has expired based on the specified timeout.
     *
     * @param timeout the timeout duration in milliseconds
     * @return true if the client has expired, false otherwise
     */
    public boolean isExpired(long timeout) {
        return isUseless() && System.currentTimeMillis() - lastAccessTime >= timeout;
    }

    /**
     * Checks if the endpoint is considered useless based on the counter value.
     *
     * @return true if the counter is zero, indicating the endpoint is useless; false otherwise
     */
    public boolean isUseless() {
        return counter.get() == 0;
    }

    /**
     * Start the redis client.
     */
    public void start() {
        if (delegate == null) {
            synchronized (this) {
                if (delegate == null) {
                    delegate = createClient();
                }
            }
        }
        lastAccessTime = System.currentTimeMillis();
        counter.incrementAndGet();
    }

    /**
     * Close the redis client.
     * If the reference count reaches zero, the client is removed by the consumer.
     */
    public void close() {
        if (counter.decrementAndGet() == 0) {
            consumer.accept(this);
        }
    }

    protected RedisConfig getConfig() {
        return config;
    }

    /**
     * Shuts down the Redis client.
     * This method should be called when the client is no longer needed to release resources.
     */
    protected void shutdown() {
        if (delegate != null) {
            delegate.shutdown();
        }
    }

    /**
     * Creates and returns a RedissonClient instance based on the provided configuration.
     *
     * @return A RedissonClient instance if the configuration is valid, otherwise null.
     */
    private RedissonClient createClient() {
        RedissonClient result = null;
        if (config.validate()) {
            Config cfg = createConfig(config);
            try {
                result = Redisson.create(cfg);
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
        return result;
    }

    private Config createConfig(RedisConfig config) {
        Config result = new Config();
        RedisType redisType = RedisType.parse(config.type);
        redisType.configure(result, config);
        result.setConnectionListener(new ConnectionListener() {
            @Override
            public void onConnect(InetSocketAddress addr) {
                connected = true;
                logger.info("Redis client is connected to {}", addr);
            }

            @Override
            public void onDisconnect(InetSocketAddress addr) {
                connected = false;
                logger.info("Redis client is disconnected from {}", addr);
            }
        });
        if (config.transportMode == TransportMode.EPOLL && NativeDetector.isEpollAvailable()) {
            result.setTransportMode(TransportMode.EPOLL);
        } else if (config.transportMode == TransportMode.KQUEUE && NativeDetector.isKqueueAvailable()) {
            result.setTransportMode(TransportMode.KQUEUE);
        } else if (config.transportMode == TransportMode.IO_URING && NativeDetector.isIoUringAvailable()) {
            result.setTransportMode(TransportMode.IO_URING);
        } else if (config.transportMode == TransportMode.NIO) {
            result.setTransportMode(TransportMode.NIO);
        } else if (NativeDetector.isIoUringAvailable()) {
            result.setTransportMode(TransportMode.IO_URING);
        } else if (NativeDetector.isEpollAvailable()) {
            result.setTransportMode(TransportMode.EPOLL);
        } else if (NativeDetector.isKqueueAvailable()) {
            result.setTransportMode(TransportMode.KQUEUE);
        } else {
            result.setTransportMode(TransportMode.NIO);
        }
        return result;
    }

    /**
     * Detects availability of native transport implementations.
     * Uses reflection to check if specified Netty native transports are available.
     */
    private static class NativeDetector {

        @Getter
        private static final boolean isIoUringAvailable = detect("io.netty.channel.uring.IoUring");

        @Getter
        private static final boolean isEpollAvailable = detect("io.netty.channel.epoll.Epoll");

        @Getter
        private static final boolean isKqueueAvailable = detect("io.netty.channel.kqueue.Kqueue");

        /**
         * Detects if specified native transport is available by reflection
         *
         * @param type Fully qualified class name of the transport to check
         * @return true if the transport is available, false otherwise
         */
        private static boolean detect(String type) {
            try {
                Class<?> clazz = Class.forName(type);
                Method method = clazz.getDeclaredMethod("isAvailable");
                method.setAccessible(true);
                return (Boolean) method.invoke(null);
            } catch (Throwable e) {
                return false;
            }
        }
    }

    public static void main(String[] args) {
        System.out.println(NativeDetector.isIoUringAvailable());
        System.out.println(NativeDetector.isEpollAvailable());
        System.out.println(NativeDetector.isKqueueAvailable());
    }
}
