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
import com.jd.live.agent.core.util.option.Option;
import lombok.Getter;
import org.redisson.config.ReadMode;
import org.redisson.config.TransportMode;

import java.util.Objects;

import static com.jd.live.agent.core.util.StringUtils.join;
import static com.jd.live.agent.core.util.StringUtils.split;

/**
 * Configuration class for Redis settings.
 */

@Getter
public class RedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    private static final String SCHEMA_REDISS = "rediss://";
    private static final String SCHEMA_REDIS = "redis://";

    private static final String KEY_TYPE = "type";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_USER = "user";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_SENTINEL_USER = "sentinelUser";
    private static final String KEY_SENTINEL_PASSWORD = "sentinelPassword";
    private static final String KEY_DATABASE = "database";
    private static final String KEY_READ_MODE = "readMode";
    private static final String KEY_TRANSPORT_MODE = "transportMode";
    private static final String KEY_TIMEOUT = "timeout";
    private static final String KEY_EXPIRE_TIME = "expireTime";
    private static final String KEY_RETRY_ATTEMPTS = "retryAttempts";
    private static final String KEY_RETRY_INTERVAL = "retryInterval";
    private static final String KEY_CONNECT_TIMEOUT = "connectTimeout";
    private static final String KEY_IDLE_CONNECTION_TIMEOUT = "idleConnectionTimeout";
    private static final String KEY_PING_CONNECTION_INTERVAL = "pingConnectionInterval";
    private static final String KEY_CONNECTION_POOL_SIZE = "connectionPoolSize";
    private static final String KEY_CONNECTION_MINIMUM_IDLE_SIZE = "connectionMinimumIdleSize";
    private static final String KEY_MASTER_CONNECTION_POOL_SIZE = "masterConnectionPoolSize";
    private static final String KEY_SLAVE_CONNECTION_POOL_SIZE = "slaveConnectionPoolSize";
    private static final String KEY_MASTER_CONNECTION_MINIMUM_IDLE_SIZE = "masterConnectionMinimumIdleSize";
    private static final String KEY_SLAVE_CONNECTION_MINIMUM_IDLE_SIZE = "slaveConnectionMinimumIdleSize";

    protected final long id;

    protected final String type;

    protected final String address;

    protected final String user;

    protected final String password;

    protected final String sentinelUser;

    protected final String sentinelPassword;

    protected final int database;

    protected final TransportMode transportMode;

    protected final ReadMode readMode;

    protected final int timeout;

    protected final int expireTime;

    protected final int connectionPoolSize;

    protected final int connectionMinimumIdleSize;

    protected final int masterConnectionPoolSize;

    protected final int masterConnectionMinimumIdleSize;

    protected final int slaveConnectionPoolSize;

    protected final int slaveConnectionMinimumIdleSize;

    protected final int connectTimeout;

    protected final int idleConnectionTimeout;

    protected final int pingConnectionInterval;

    protected final boolean keepAlive;

    protected final int retryAttempts;

    protected final int retryInterval;

    public RedisConfig(long id, Option option) {
        this.id = id;
        type = option.getTrimmed(KEY_TYPE, RedisType.CLUSTER.name()).toUpperCase();
        address = join(resolveAddress(split(option.getTrimmed(KEY_ADDRESS, "").toLowerCase())), ',');
        user = option.getTrimmed(KEY_USER);
        password = option.getTrimmed(KEY_PASSWORD);
        database = option.getNatural(KEY_DATABASE, 0);
        sentinelUser = option.getTrimmed(KEY_SENTINEL_USER);
        sentinelPassword = option.getTrimmed(KEY_SENTINEL_PASSWORD);
        readMode = getReadMode(option);
        transportMode = getTransportMode(option);
        timeout = option.getPositive(KEY_TIMEOUT, 5000);
        expireTime = option.getPositive(KEY_EXPIRE_TIME, 60000);
        keepAlive = option.getBoolean(KEY_RETRY_ATTEMPTS, false);
        connectTimeout = option.getPositive(KEY_CONNECT_TIMEOUT, 10000);
        idleConnectionTimeout = option.getPositive(KEY_IDLE_CONNECTION_TIMEOUT, 10000);
        pingConnectionInterval = option.getPositive(KEY_PING_CONNECTION_INTERVAL, 30000);
        retryAttempts = option.getPositive(KEY_RETRY_ATTEMPTS, 3);
        retryInterval = option.getPositive(KEY_RETRY_INTERVAL, 1500);
        connectionPoolSize = option.getPositive(KEY_CONNECTION_POOL_SIZE, 64);
        connectionMinimumIdleSize = option.getPositive(KEY_CONNECTION_MINIMUM_IDLE_SIZE, 24);
        masterConnectionPoolSize = option.getPositive(KEY_MASTER_CONNECTION_POOL_SIZE, 64);
        masterConnectionMinimumIdleSize = option.getPositive(KEY_MASTER_CONNECTION_MINIMUM_IDLE_SIZE, 24);
        slaveConnectionPoolSize = option.getPositive(KEY_SLAVE_CONNECTION_POOL_SIZE, 64);
        slaveConnectionMinimumIdleSize = option.getPositive(KEY_SLAVE_CONNECTION_MINIMUM_IDLE_SIZE, 24);
    }

    /**
     * Validates the Redis configuration.
     * This method checks if the address is not null or empty.
     *
     * @return true if the address is valid, false otherwise
     */
    public boolean validate() {
        if (address == null || address.isEmpty()) {
            logger.error("redisson address is empty for rate limit policy " + id);
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RedisConfig)) return false;
        RedisConfig that = (RedisConfig) o;
        return id == that.id
                && database == that.database
                && timeout == that.timeout
                && connectionPoolSize == that.connectionPoolSize
                && connectionMinimumIdleSize == that.connectionMinimumIdleSize
                && masterConnectionPoolSize == that.masterConnectionPoolSize
                && masterConnectionMinimumIdleSize == that.masterConnectionMinimumIdleSize
                && slaveConnectionPoolSize == that.slaveConnectionPoolSize
                && slaveConnectionMinimumIdleSize == that.slaveConnectionMinimumIdleSize
                && connectTimeout == that.connectTimeout
                && idleConnectionTimeout == that.idleConnectionTimeout
                && pingConnectionInterval == that.pingConnectionInterval
                && keepAlive == that.keepAlive
                && retryAttempts == that.retryAttempts
                && retryInterval == that.retryInterval
                && Objects.equals(type, that.type)
                && Objects.equals(address, that.address)
                && Objects.equals(user, that.user)
                && Objects.equals(password, that.password)
                && Objects.equals(sentinelUser, that.sentinelUser)
                && Objects.equals(sentinelPassword, that.sentinelPassword);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, address, user, password, database, timeout,
                connectionPoolSize, connectionMinimumIdleSize,
                masterConnectionPoolSize, masterConnectionMinimumIdleSize,
                slaveConnectionPoolSize, slaveConnectionMinimumIdleSize,
                connectTimeout, idleConnectionTimeout, pingConnectionInterval, keepAlive,
                retryAttempts, retryInterval, sentinelUser, sentinelPassword);
    }

    /**
     * Resolves a single address by ensuring it has the correct Redis schema.
     * If the address is null or empty, it returns the address as is.
     * If the address already starts with "rediss://" or "redis://", it returns the address unchanged.
     * Otherwise, it prepends "redis://" to the address.
     *
     * @param address the address to resolve
     * @return the resolved address with the correct schema
     */
    protected String resolveAddress(String address) {
        if (address == null || address.isEmpty()) {
            return address;
        } else if (address.startsWith(SCHEMA_REDISS)) {
            return address;
        } else if (address.startsWith(SCHEMA_REDIS)) {
            return address;
        } else {
            return SCHEMA_REDIS + address;
        }
    }

    /**
     * Resolves an array of addresses by ensuring each address has the correct Redis schema.
     * It uses the {@link #resolveAddress(String)} method for each individual address.
     *
     * @param addresses the array of addresses to resolve
     * @return an array of resolved addresses with the correct schema
     */
    protected String[] resolveAddress(String[] addresses) {
        String[] result = null;
        if (addresses != null) {
            result = new String[addresses.length];
            for (int i = 0; i < addresses.length; i++) {
                result[i] = resolveAddress(addresses[i]);
            }
        }

        return result;
    }

    protected static ReadMode getReadMode(Option option) {
        try {
            return ReadMode.valueOf(option.getTrimmed(KEY_READ_MODE, ReadMode.MASTER.name()).toUpperCase());
        } catch (Throwable e) {
            return ReadMode.MASTER;
        }
    }

    protected static TransportMode getTransportMode(Option option) {
        String transportMode = option.getTrimmed(KEY_TRANSPORT_MODE);
        try {
            return transportMode == null || transportMode.isEmpty() ? null : TransportMode.valueOf(transportMode.toUpperCase());
        } catch (Throwable e) {
            return null;
        }
    }


}
