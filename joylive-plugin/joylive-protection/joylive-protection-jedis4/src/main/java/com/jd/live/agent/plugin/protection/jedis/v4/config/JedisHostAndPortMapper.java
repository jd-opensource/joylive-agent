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
package com.jd.live.agent.plugin.protection.jedis.v4.config;

import lombok.Getter;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPortMapper;

public class JedisHostAndPortMapper implements HostAndPortMapper {

    private final HostAndPortMapper delegate;

    @Getter
    private final String[] addresses;

    public JedisHostAndPortMapper(HostAndPortMapper delegate, String[] addresses) {
        this.delegate = delegate;
        this.addresses = addresses;
    }

    @Override
    public HostAndPort getHostAndPort(HostAndPort hap) {
        return delegate == null ? hap : delegate.getHostAndPort(hap);
    }
}
