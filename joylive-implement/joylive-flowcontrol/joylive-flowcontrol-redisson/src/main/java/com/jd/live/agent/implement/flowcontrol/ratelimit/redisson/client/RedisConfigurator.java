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

import org.redisson.config.*;

import java.util.List;

import static com.jd.live.agent.core.util.StringUtils.split;
import static com.jd.live.agent.core.util.StringUtils.splitList;

public interface RedisConfigurator<T> {

    /**
     * Configures the given configuration object using settings from the provided {@link RedisConfig}.
     *
     * @param config      the configuration object to be configured
     * @param redisConfig the {@link RedisConfig} object containing the settings
     */
    void configure(T config, RedisConfig redisConfig);

    /**
     * Abstract base class for configuring Redis settings.
     *
     * @param <T> the type of the configuration object, extending {@link BaseConfig}
     */
    abstract class BaseConfigurator<T extends BaseConfig<T>> implements RedisConfigurator<T> {

        @Override
        public void configure(T config, RedisConfig redisConfig) {
            config.setUsername(redisConfig.user == null || redisConfig.user.isEmpty() ? null : redisConfig.user);
            config.setPassword(redisConfig.password);
            config.setRetryAttempts(redisConfig.retryAttempts);
            config.setRetryInterval(redisConfig.retryInterval);
            config.setKeepAlive(redisConfig.keepAlive);
            config.setTimeout(redisConfig.timeout);
            config.setConnectTimeout(redisConfig.connectTimeout);
            config.setIdleConnectionTimeout(redisConfig.idleConnectionTimeout);
            config.setPingConnectionInterval(redisConfig.pingConnectionInterval);
        }

    }

    /**
     * Abstract base class for configuring Redis master-slave settings.
     *
     * @param <T> the type of the configuration object, extending {@link BaseMasterSlaveServersConfig}
     */
    abstract class BaseMasterSlaveConfigurator<T extends BaseMasterSlaveServersConfig<T>> extends BaseConfigurator<T> {

        @Override
        public void configure(T config, RedisConfig redisConfig) {
            super.configure(config, redisConfig);
            config.setReadMode(ReadMode.MASTER)
                    .setMasterConnectionPoolSize(redisConfig.masterConnectionPoolSize)
                    .setMasterConnectionMinimumIdleSize(redisConfig.masterConnectionMinimumIdleSize)
                    .setSlaveConnectionPoolSize(redisConfig.slaveConnectionPoolSize)
                    .setSlaveConnectionMinimumIdleSize(redisConfig.slaveConnectionMinimumIdleSize);
        }
    }

    /**
     * Configurator for Redis master-slave setups.
     */
    class MasterSlaveConfigurator extends BaseMasterSlaveConfigurator<MasterSlaveServersConfig> {

        public static MasterSlaveConfigurator INSTANCE = new MasterSlaveConfigurator();

        @Override
        public void configure(MasterSlaveServersConfig config, RedisConfig redisConfig) {
            super.configure(config, redisConfig);
            List<String> addresses = splitList(redisConfig.address);
            String master = addresses.get(0);
            String[] slaves = addresses.size() == 1 ? new String[0] : addresses.subList(1, addresses.size()).toArray(new String[0]);
            config.setMasterAddress(master)
                    .addSlaveAddress(slaves)
                    .setDatabase(redisConfig.database);
        }
    }

    /**
     * Configurator for Redis sentinel setups.
     */
    class SentinelConfigurator extends BaseMasterSlaveConfigurator<SentinelServersConfig> {

        public static final SentinelConfigurator INSTANCE = new SentinelConfigurator();

        @Override
        public void configure(SentinelServersConfig config, RedisConfig redisConfig) {
            super.configure(config, redisConfig);
            config.addSentinelAddress(split(redisConfig.address))
                    .setDatabase(config.getDatabase())
                    .setSentinelUsername(redisConfig.sentinelUser)
                    .setSentinelPassword(redisConfig.sentinelPassword);
        }
    }

    /**
     * Configurator for Redis cluster setups.
     */
    class ClusterConfigurator extends BaseMasterSlaveConfigurator<ClusterServersConfig> {

        public static final ClusterConfigurator INSTANCE = new ClusterConfigurator();

        @Override
        public void configure(ClusterServersConfig config, RedisConfig redisConfig) {
            super.configure(config, redisConfig);
            config.addNodeAddress(split(redisConfig.address));
        }
    }

    /**
     * Configurator for Redis replicated setups.
     */
    class ReplicatedConfigurator extends BaseMasterSlaveConfigurator<ReplicatedServersConfig> {

        public static ReplicatedConfigurator INSTANCE = new ReplicatedConfigurator();

        @Override
        public void configure(ReplicatedServersConfig config, RedisConfig redisConfig) {
            super.configure(config, redisConfig);
            config.addNodeAddress(split(redisConfig.address)).setDatabase(config.getDatabase());
        }
    }

    /**
     * Configurator for single Redis server setups.
     */
    class SingleConfigurator extends BaseConfigurator<SingleServerConfig> {

        public static final SingleConfigurator INSTANCE = new SingleConfigurator();

        @Override
        public void configure(SingleServerConfig config, RedisConfig redisConfig) {
            super.configure(config, redisConfig);
            config.setAddress(redisConfig.address)
                    .setDatabase(redisConfig.database)
                    .setConnectionPoolSize(redisConfig.connectionPoolSize)
                    .setConnectionMinimumIdleSize(redisConfig.connectionMinimumIdleSize);
        }
    }
}
