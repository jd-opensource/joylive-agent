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
package com.jd.live.agent.plugin.protection.jedis.v3.config;

import lombok.Getter;
import redis.clients.jedis.HostAndPortMapper;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.Protocol;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

public class JedisConfig implements JedisClientConfig {

    private final JedisClientConfig config;

    @Getter
    private final String[] addresses;

    public JedisConfig(JedisClientConfig config, String[] addresses) {
        this.config = config;
        this.addresses = addresses;
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
        return new JedisHostAndPortMapper(config == null ? null : config.getHostAndPortMapper(), addresses);
    }

}
