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

import com.jd.live.agent.implement.flowcontrol.ratelimit.redisson.client.RedisConfigurator.*;
import org.redisson.config.Config;

/**
 * Enum representing different types of Redis configurations.
 * Each enum constant provides a method to configure a Redisson {@link Config} object based on the specific Redis type.
 */
public enum RedisType {

    /**
     * Represents a single Redis server configuration.
     */
    SINGLE {
        @Override
        public void configure(Config config, RedisConfig redisConfig) {
            SingleConfigurator.INSTANCE.configure(config.useSingleServer(), redisConfig);
        }
    },

    /**
     * Represents a replicated Redis servers configuration.
     */
    REPLICATED {
        @Override
        public void configure(Config config, RedisConfig redisConfig) {
            ReplicatedConfigurator.INSTANCE.configure(config.useReplicatedServers(), redisConfig);
        }
    },

    /**
     * Represents a Redis sentinel configuration.
     */
    SENTINEL {
        @Override
        public void configure(Config config, RedisConfig redisConfig) {
            SentinelConfigurator.INSTANCE.configure(config.useSentinelServers(), redisConfig);
        }
    },

    /**
     * Represents a Redis cluster configuration.
     */
    CLUSTER {
        @Override
        public void configure(Config config, RedisConfig redisConfig) {
            ClusterConfigurator.INSTANCE.configure(config.useClusterServers(), redisConfig);
        }
    },

    /**
     * Represents a master-slave Redis configuration.
     */
    MASTER_SLAVE {
        @Override
        public void configure(Config config, RedisConfig redisConfig) {
            MasterSlaveConfigurator.INSTANCE.configure(config.useMasterSlaveServers(), redisConfig);
        }
    };

    /**
     * Configures the Redisson {@link Config} object based on the specific Redis type.
     *
     * @param config      the Redisson {@link Config} object to be configured
     * @param redisConfig the {@link RedisConfig} containing the configuration settings
     */
    public abstract void configure(Config config, RedisConfig redisConfig);

    /**
     * Parses a string to a {@link RedisType} enum constant.
     * If the input string is null or empty, or if it does not match any valid enum constant,
     * the method returns {@link RedisType#CLUSTER} as the default value.
     *
     * @param type the string representation of the Redis type
     * @return the corresponding {@link RedisType} enum constant, or {@link RedisType#CLUSTER} if the input is invalid
     */
    public static RedisType parse(String type) {
        if (type == null || type.isEmpty()) {
            return CLUSTER;
        }
        try {
            return RedisType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CLUSTER;
        }
    }

}
