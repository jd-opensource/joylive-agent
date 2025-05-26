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
package com.jd.live.agent.plugin.registry.dubbo.v2_7.zookeeper.curator4;

import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.plugin.registry.dubbo.v2_7.zookeeper.CuratorFailoverClient;
import com.jd.live.agent.plugin.registry.dubbo.v2_7.zookeeper.CuratorListener;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.zookeeper.ChildListener;
import org.apache.dubbo.remoting.zookeeper.DataListener;
import org.apache.dubbo.remoting.zookeeper.support.AbstractZookeeperClient;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * ZooKeeper client implementation with failover support using Apache Curator.
 * <p>
 * Handles connection management, path operations, and listener registration with
 * automatic retry and recovery capabilities.
 */
public class Curator4FailoverClient extends AbstractZookeeperClient<CuratorListener, CuratorListener> {

    private final CuratorFailoverClient client;

    public Curator4FailoverClient(URL url, Timer timer) {
        super(url);
        this.client = new CuratorFailoverClient(url, this::stateChanged, timer);
    }

    @Override
    public boolean isConnected() {
        return client.isConnected();
    }

    @Override
    public void doClose() {
        client.close();
    }

    @Override
    public void createPersistent(String path) {
        client.createPersistent(path);
    }

    @Override
    protected void createPersistent(String path, String data) {
        client.createPersistent(path, data);
    }

    @Override
    public void createEphemeral(String path) {
        client.createEphemeral(path);
    }

    @Override
    protected void createEphemeral(String path, String data) {
        client.createEphemeral(path, data);
    }

    @Override
    public List<String> getChildren(String path) {
        return client.getChildren(path);
    }

    @Override
    protected void deletePath(String path) {
        client.deletePath(path);
    }

    @Override
    public boolean checkExists(String path) {
        return client.checkExists(path);
    }

    @Override
    public String doGetContent(String path) {
        return client.getData(path);
    }

    @Override
    public CuratorListener createTargetChildListener(String path, ChildListener listener) {
        return client.createChildListener(path, listener);
    }

    @Override
    protected List<String> addTargetChildListener(String path, CuratorListener listener) {
        return client.addChildListener(path, listener);
    }

    @Override
    public void removeTargetChildListener(String path, CuratorListener listener) {
        client.removeChildListener(path, listener);
    }

    @Override
    protected CuratorListener createTargetDataListener(String path, DataListener listener) {
        return client.createDataListener(path, listener);
    }

    @Override
    protected void addTargetDataListener(String path, CuratorListener listener) {
        client.addDataListener(path, listener);
    }

    @Override
    protected void addTargetDataListener(String path, CuratorListener listener, Executor executor) {
        client.addDataListener(path, listener, executor);
    }

    @Override
    protected void removeTargetDataListener(String path, CuratorListener listener) {
        client.removeDataListener(path, listener);
    }

}