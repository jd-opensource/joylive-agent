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
package com.jd.live.agent.plugin.registry.dubbo.v2_7.zookeeper;

/**
 * Manages failover between multiple server addresses in distributed systems.
 * Handles connection failures and provides recovery mechanisms.
 */
public interface CuratorFailoverAddressList {

    /**
     * @return Current active server address
     */
    String current();

    /**
     * @return First available server address in the list
     */
    String first();

    /**
     * Switches to next available server address
     */
    void next();

    /**
     * Resets to initial server address
     */
    void reset();

    /**
     * @return Total number of available server addresses
     */
    int size();
}
