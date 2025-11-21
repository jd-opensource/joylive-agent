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
package com.jd.live.agent.implement.service.registry.zookeeper.dubbo.converter;

import com.jd.live.agent.core.parser.annotation.JsonType;
import lombok.Getter;

/**
 * Represents a service instance registered in ZooKeeper with Dubbo metadata.
 *
 * @param <T> the type of additional payload data
 */
@Getter
public class CuratorInstance<T> {

    private final String name;
    private final String id;
    private final String address;
    private final Integer port;
    private final Integer sslPort;
    @JsonType("org.apache.dubbo.registry.zookeeper.ZookeeperInstance")
    private final T payload;
    private final long registrationTimeUTC;
    private final String serviceType;

    public CuratorInstance(String id, String name, String address, Integer port, T payload) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.port = port;
        this.sslPort = null;
        this.payload = payload;
        this.registrationTimeUTC = System.currentTimeMillis();
        this.serviceType = "DYNAMIC";
    }
}
