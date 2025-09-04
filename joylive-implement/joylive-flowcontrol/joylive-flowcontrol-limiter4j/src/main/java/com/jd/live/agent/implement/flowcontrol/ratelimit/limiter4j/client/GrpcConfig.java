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
package com.jd.live.agent.implement.flowcontrol.ratelimit.limiter4j.client;

import com.jd.live.agent.core.util.network.Address;
import com.jd.live.agent.core.util.option.Option;
import lombok.Getter;

import java.util.Objects;

/**
 * gRPC client configuration
 *
 * @since 1.9.0
 */
@Getter
public class GrpcConfig {

    /**
     * Server address
     */
    private String address;

    /**
     * Server host
     */
    private final String host;

    /**
     * Server port
     */
    private final int port;

    /**
     * Connection timeout in milliseconds
     */
    private final long connectTimeoutMs;

    private final long keepAliveTimeMs;

    private final long keepAliveTimeoutMs;

    /**
     * Configuration ID
     */
    private final long id;

    public GrpcConfig(long id, Option option) {
        this.id = id;
        this.address = option.getString("address", "localhost:9090");
        Address addr = Address.parse(address, 9090);
        this.host = addr.getHost();
        this.port = addr.getPort();
        this.connectTimeoutMs = option.getPositive("connectTimeoutMs", 5000L);
        this.keepAliveTimeMs = option.getLong("keepAliveTimeMs", 30000L);
        this.keepAliveTimeoutMs = option.getLong("keepAliveTimeoutMs", 5000L);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GrpcConfig)) return false;
        GrpcConfig that = (GrpcConfig) o;
        return connectTimeoutMs == that.connectTimeoutMs
                && id == that.id
                && Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, connectTimeoutMs, id);
    }
}