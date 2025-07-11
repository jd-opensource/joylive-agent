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
package com.jd.live.agent.plugin.registry.dubbo.v2_6.zookeeper;

import com.jd.live.agent.governance.probe.FailoverAddressList;
import org.apache.curator.ensemble.EnsembleProvider;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Provides ZooKeeper ensemble connection strings with failover support.
 * <p>
 * Features:
 * <ul>
 *   <li>Round-robin server selection for connection strings
 *   <li>Optional dynamic server list updates
 *   <li>Basic failover counting mechanism
 * </ul>
 */
public class CuratorFailoverEnsembleProvider implements EnsembleProvider, FailoverAddressList {

    private final List<String> addresses;

    private final AtomicLong counter = new AtomicLong(0);

    public CuratorFailoverEnsembleProvider(List<String> addresses) {
        this.addresses = addresses;
    }

    @Override
    public String current() {
        long count = counter.get();
        List<String> addresses = this.addresses;
        return addresses.get((int) (count % addresses.size()));
    }

    @Override
    public String first() {
        return addresses.get(0);
    }

    @Override
    public void next() {
        counter.incrementAndGet();
    }

    @Override
    public void reset() {
        counter.set(0);
    }

    @Override
    public int size() {
        return addresses.size();
    }

    @Override
    public void start() {

    }

    @Override
    public String getConnectionString() {
        List<String> addresses = this.addresses;
        int index = (int) (counter.get() % addresses.size());
        return addresses.get(index);
    }

    @Override
    public void close() {

    }
}
