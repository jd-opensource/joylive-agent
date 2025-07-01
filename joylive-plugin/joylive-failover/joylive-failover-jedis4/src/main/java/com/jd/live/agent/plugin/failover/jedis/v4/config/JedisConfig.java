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
package com.jd.live.agent.plugin.failover.jedis.v4.config;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPortMapper;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.Protocol;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import java.util.function.Function;

public class JedisConfig implements JedisClientConfig {

    private final JedisClientConfig config;

    private final Function<HostAndPort, HostAndPort> mapper;

    public JedisConfig(JedisClientConfig config, Function<HostAndPort, HostAndPort> mapper) {
        this.config = config;
        this.mapper = mapper;
    }

    @Override
    public int getConnectionTimeoutMillis() {
        return config == null ? Protocol.DEFAULT_TIMEOUT : config.getConnectionTimeoutMillis();
    }

    @Override
    public int getSocketTimeoutMillis() {
        return config == null ? Protocol.DEFAULT_TIMEOUT : config.getSocketTimeoutMillis();
    }

    @Override
    public int getBlockingSocketTimeoutMillis() {
        return config == null ? 0 : config.getBlockingSocketTimeoutMillis();
    }

    @Override
    public String getUser() {
        return config == null ? null : config.getUser();
    }

    @Override
    public String getPassword() {
        return config == null ? null : config.getPassword();
    }

    @Override
    public int getDatabase() {
        return config == null ? Protocol.DEFAULT_DATABASE : config.getDatabase();
    }

    @Override
    public String getClientName() {
        return config == null ? null : config.getClientName();
    }

    @Override
    public boolean isSsl() {
        return config != null && config.isSsl();
    }

    @Override
    public SSLSocketFactory getSslSocketFactory() {
        return config == null ? null : config.getSslSocketFactory();
    }

    @Override
    public SSLParameters getSslParameters() {
        return config == null ? null : config.getSslParameters();
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        return config == null ? null : config.getHostnameVerifier();
    }

    @Override
    public HostAndPortMapper getHostAndPortMapper() {
        return new JedisAddressMapper(config == null ? null : config.getHostAndPortMapper(), mapper);
    }
}
