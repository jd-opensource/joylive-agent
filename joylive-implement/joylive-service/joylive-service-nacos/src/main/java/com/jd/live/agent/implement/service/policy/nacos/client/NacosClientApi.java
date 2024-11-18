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
package com.jd.live.agent.implement.service.policy.nacos.client;

import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.jd.live.agent.governance.service.sync.SyncResponse;
import com.jd.live.agent.governance.service.sync.Syncer;
import com.jd.live.agent.implement.service.policy.nacos.NacosSyncKey;

import java.util.function.Function;

public interface NacosClientApi extends AutoCloseable {

     String DEFAULT_NAMESPACE = "public";

    /**
     * Connects to the Nacos server using the specified configuration.
     *
     * @throws NacosException If there is an error connecting to the Nacos server.
     */
    void connect() throws NacosException;

    @Override
    void close() throws NacosException;

    /**
     * Subscribes to configuration changes for the specified dataId and group.
     *
     * @param dataId   The dataId of the configuration to subscribe to.
     * @param group    The group of the configuration to subscribe to.
     * @param listener The listener to notify when configuration changes occur.
     * @throws NacosException If there is an error subscribing to the configuration changes.
     */
    void subscribe(String dataId, String group, Listener listener) throws NacosException;

    /**
     * Unsubscribes from configuration changes for the specified dataId and group.
     *
     * @param dataId   The dataId of the configuration to unsubscribe from.
     * @param group    The group of the configuration to unsubscribe from.
     * @param listener The listener to remove from the subscription.
     */
    void unsubscribe(String dataId, String group, Listener listener);

    /**
     * Creates a new Syncer object that can be used to synchronize data between Nacos and a local cache.
     *
     * @param parser A function that takes a configuration string as input and returns an object of type T.
     * @param <K>    The type of the NacosSyncKey used to identify the configuration in Nacos.
     * @param <T>    The type of the object returned by the parser function.
     * @return A new Syncer object that can be used to synchronize data between Nacos and a local cache.
     */
    <K extends NacosSyncKey, T> Syncer<K, T> createSyncer(Function<String, SyncResponse<T>> parser);
}
