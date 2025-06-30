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
package com.jd.live.agent.plugin.failover.jedis.v3.config;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPortMapper;

import java.util.function.Function;

/**
 * HostAndPortMapper implementation that applies both a delegate mapper and a mapping function.
 */
public class JedisAddressMapper implements HostAndPortMapper {

    private final HostAndPortMapper delegate;

    private final Function<HostAndPort, HostAndPort> mapper;

    public JedisAddressMapper(HostAndPortMapper delegate, Function<HostAndPort, HostAndPort> mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public HostAndPort getHostAndPort(HostAndPort hap) {
        HostAndPort result = delegate == null ? hap : delegate.getHostAndPort(hap);
        return mapper.apply(result);
    }
}
