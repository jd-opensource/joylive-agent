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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.util.task.RetryVersionTask;
import com.jd.live.agent.core.util.task.RetryVersionTimerTask;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.plugin.registry.dubbo.v2_7.zookeeper.CuratorExecution.CuratorVoidExecution;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.retry.RetryNTimes;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.zookeeper.ChildListener;
import org.apache.dubbo.remoting.zookeeper.DataListener;
import org.apache.zookeeper.CreateMode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.jd.live.agent.core.util.StringUtils.join;
import static com.jd.live.agent.core.util.StringUtils.splitList;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;

/**
 * ZooKeeper client with failover support using Apache Curator framework.
 * <p>
 * Provides automatic connection recovery and retry mechanisms for:
 * <ul>
 *   <li>Node operations (create/delete/check existence)
 *   <li>Data operations (get/set)
 *   <li>Path monitoring (child/data listeners)
 * </ul>
 *
 * <p>Configuration through {@link URL} including:
 * <ul>
 *   <li>Connection timeout ({@value #DEFAULT_CONNECTION_TIMEOUT_MS}ms default)
 *   <li>Session timeout ({@value #DEFAULT_SESSION_TIMEOUT_MS}ms default)
 *   <li>Authentication credentials
 * </ul>
 */
public class CuratorFailoverClient {

    private static final Logger logger = LoggerFactory.getLogger(CuratorFailoverClient.class);
    private static final String ZK_SESSION_EXPIRE_KEY = "zk.session.expire";

    private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 5 * 1000;
    private static final int DEFAULT_SESSION_TIMEOUT_MS = 60 * 1000;

    private final URL url;
    private final Consumer<Integer> stateListener;
    private final Timer timer;
    private final int timeout;
    private final int successThreshold;
    private final boolean autoRecover;
    private final List<String> addresses = new ArrayList<>();
    private final CuratorFailoverEnsembleProvider ensembleProvider;
    private final CuratorConnectionStateListener connectionListener;
    private final CuratorFramework client;
    private final AtomicLong versions = new AtomicLong(0);
    private final Predicate<RetryVersionTask> predicate = v -> v.getVersion() == versions.get();
    @SuppressWarnings("deprecation")
    private final Map<String, TreeCache> caches = new ConcurrentHashMap<>();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final CountDownLatch connectLatch = new CountDownLatch(1);

    public CuratorFailoverClient(URL url, Consumer<Integer> stateListener, Timer timer) {
        this.url = url;
        this.stateListener = stateListener;
        this.timer = timer;
        this.timeout = url.getParameter(TIMEOUT_KEY, DEFAULT_CONNECTION_TIMEOUT_MS);
        this.successThreshold = url.getParameter("successThreshold", 3);
        this.autoRecover = url.getParameter("autoRecover", true);
        int sessionExpireMs = url.getParameter(ZK_SESSION_EXPIRE_KEY, DEFAULT_SESSION_TIMEOUT_MS);
        addresses.add(url.getBackupAddress());
        addresses.addAll(splitList(url.getParameter("failovers"), ';'));
        this.ensembleProvider = new CuratorFailoverEnsembleProvider(addresses);
        this.connectionListener = new CuratorConnectionStateListener(stateListener, timeout, sessionExpireMs) {
            @Override
            protected void onLost() {
                doLost();
                super.onLost();
            }

            @Override
            protected void onSuspended(long sessionId) {
                doSuspended(sessionId);
                super.onSuspended(sessionId);
            }

            @Override
            protected void onConnected(long sessionId) {
                doConnected(sessionId);
                super.onConnected(sessionId);
                // Notify main thread which is waiting for connection
                connectLatch.countDown();
            }

            @Override
            protected void onReconnected(long sessionId) {
                doReconnected(sessionId);
                super.onReconnected(sessionId);
            }
        };
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .ensembleProvider(ensembleProvider)
                .retryPolicy(new RetryNTimes(1, 1000))
                .connectionTimeoutMs(timeout)
                .sessionTimeoutMs(sessionExpireMs);
        String authority = url.getAuthority();
        if (authority != null && !authority.isEmpty()) {
            builder = builder.authorization("digest", authority.getBytes());
        }
        this.client = builder.build();
        client.getConnectionStateListenable().addListener(connectionListener);

        try {
            // Replace client.start() with addDetectTask
            logger.info("Try detect healthy zookeeper {}", join(addresses, ';'));
            addDetectTask(false);
            if (!connectLatch.await((long) timeout * addresses.size(), TimeUnit.MILLISECONDS)) {
                // cancel task.
                versions.incrementAndGet();
                throw new IllegalStateException("It's timeout to connect to zookeeper.");
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Checks if currently connected to ZooKeeper.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return client.getZookeeperClient().isConnected();
    }

    /**
     * Closes the client and releases all resources.
     */
    public void close() {
        client.close();
    }

    /**
     * Creates a persistent znode at specified path.
     *
     * @param path the node path to create
     */
    public void createPersistent(String path) {
        CuratorTask.of(client).execute(new PathData(path), new CuratorVoidExecution() {

            @Override
            public void doExecute(PathData pathData, CuratorFramework client) throws Exception {
                client.create().forPath(pathData.getPath(), pathData.getData());
            }

            @Override
            public void doOnNodeExists(PathData pathData, CuratorFramework client, Exception e) {
                logger.warn("ZNode {} already exists.", pathData.getPath(), e);
            }
        });
    }

    /**
     * Creates a persistent znode with data at specified path.
     *
     * @param path the node path to create
     * @param data the data to store in the node
     */
    public void createPersistent(String path, String data) {
        CuratorTask.of(client).execute(new PathData(path, data), new CuratorVoidExecution() {
            @Override
            public void doExecute(PathData pathData, CuratorFramework client) throws Exception {
                client.create().forPath(pathData.getPath(), pathData.getData());
            }

            @Override
            public void doOnNodeExists(PathData pathData, CuratorFramework client, Exception e) {
                try {
                    client.setData().forPath(pathData.getPath(), pathData.getData());
                } catch (Exception ex) {
                    throw new IllegalStateException(e.getMessage(), ex);
                }
            }
        });
    }

    /**
     * Creates an ephemeral znode at specified path.
     *
     * @param path the node path to create (will be removed on session close)
     */
    public void createEphemeral(String path) {
        CuratorTask.of(client).execute(new PathData(path), new CuratorVoidExecution() {
            @Override
            public void doExecute(PathData pathData, CuratorFramework client) throws Exception {
                client.create().withMode(CreateMode.EPHEMERAL).forPath(pathData.getPath(), pathData.getData());
            }

            @Override
            public void doOnNodeExists(PathData pathData, CuratorFramework client, Exception e) {
                logger.warn("ZNode {} already exists, since we will only try to recreate a node on a session expiration" +
                        ", this duplication might be caused by a delete delay from the zk server, which means the old expired session" +
                        " may still holds this ZNode and the server just hasn't got time to do the deletion. In this case, " +
                        "we can just try to delete and create again.", pathData.getPath(), e);
                deletePath(path);
                createEphemeral(path);
            }
        });
    }

    /**
     * Creates an ephemeral znode with data at specified path.
     *
     * @param path the node path to create (will be removed on session close)
     * @param data the data to store in the node
     */
    public void createEphemeral(String path, String data) {
        CuratorTask.of(client).execute(new PathData(path, data), new CuratorVoidExecution() {
            @Override
            public void doExecute(PathData pathData, CuratorFramework client) throws Exception {
                client.create().withMode(CreateMode.EPHEMERAL).forPath(pathData.getPath(), pathData.getData());
            }

            @Override
            public void doOnNodeExists(PathData pathData, CuratorFramework client, Exception e) {
                logger.warn("ZNode {} already exists, since we will only try to recreate a node on a session expiration" +
                        ", this duplication might be caused by a delete delay from the zk server, which means the old expired session" +
                        " may still holds this ZNode and the server just hasn't got time to do the deletion. In this case, " +
                        "we can just try to delete and create again.", pathData.getPath(), e);
                deletePath(path);
                createEphemeral(path, data);
            }
        });
    }

    /**
     * Gets all children of a znode.
     *
     * @param path the parent node path
     * @return list of child node names
     */
    public List<String> getChildren(String path) {
        return CuratorTask.of(client).execute(new PathData(path), (pathData, client) -> client.getChildren().forPath(path));
    }

    /**
     * Deletes a znode and all its children recursively.
     *
     * @param path the node path to delete
     */
    public void deletePath(String path) {
        CuratorTask.of(client).execute(new PathData(path), (CuratorExecution<Void>) (pathData, client) -> {
            client.delete().deletingChildrenIfNeeded().forPath(path);
            return null;
        });
    }

    /**
     * Checks if a znode exists.
     *
     * @param path the node path to check
     * @return true if node exists, false otherwise
     */
    public boolean checkExists(String path) {
        return CuratorTask.of(client).execute(new PathData(path), (pathData, client) -> client.checkExists().forPath(path) != null);
    }

    /**
     * Gets data stored in a znode.
     *
     * @param path the node path to read
     * @return the node data as string, or null if empty
     */
    public String getData(String path) {
        return CuratorTask.of(client).execute(new PathData(path), (pathData, client) -> {
            byte[] dataBytes = client.getData().forPath(path);
            return (dataBytes == null || dataBytes.length == 0) ? null : new String(dataBytes, StandardCharsets.UTF_8);
        });
    }

    /**
     * Creates a child listener for path changes.
     *
     * @param path     the path to monitor
     * @param listener the callback for child events
     * @return the created listener instance
     */
    public CuratorListener createChildListener(String path, ChildListener listener) {
        return new CuratorListenerImpl(client, listener, path);
    }

    /**
     * Creates a data listener for node changes.
     *
     * @param listener the callback for data events
     * @return the created listener instance
     */
    public CuratorListener createDataListener(String path, DataListener listener) {
        return new CuratorListenerImpl(client, listener);
    }

    /**
     * Adds a child listener to monitor path changes.
     *
     * @param path     the path to monitor
     * @param listener the listener to add
     * @return current list of children
     */
    public List<String> addChildListener(String path, CuratorListener listener) {
        return CuratorTask.of(client).execute(new PathData(path), (pathData, client) -> client.getChildren().usingWatcher(listener).forPath(path));
    }

    /**
     * Removes a child listener from monitoring.
     *
     * @param path     the monitored path
     * @param listener the listener to remove
     */
    public void removeChildListener(String path, CuratorListener listener) {
        listener.removeChildrenListener();
    }

    /**
     * Adds a data listener to monitor node changes.
     *
     * @param path     the node path to monitor
     * @param listener the listener to add
     */
    public void addDataListener(String path, CuratorListener listener) {
        addDataListener(path, listener, null);
    }

    /**
     * Adds a data listener with custom executor.
     *
     * @param path     the node path to monitor
     * @param listener the listener to add
     * @param executor the executor for handling events
     */
    @SuppressWarnings("deprecation")
    public void addDataListener(String path, CuratorListener listener, Executor executor) {
        TreeCache cache = caches.computeIfAbsent(path, p -> {
            TreeCache result = TreeCache.newBuilder(client, path).setCacheData(false).build();
            try {
                result.start();
                return result;
            } catch (Exception e) {
                result.close();
                throw new IllegalStateException("Add treeCache listener for path:" + path, e);
            }
        });
        if (executor == null) {
            cache.getListenable().addListener(listener);
        } else {
            cache.getListenable().addListener(listener, executor);
        }
    }

    /**
     * Removes a data listener from monitoring.
     *
     * @param path     the monitored path
     * @param listener the listener to remove
     */
    @SuppressWarnings("deprecation")
    public void removeDataListener(String path, CuratorListener listener) {
        TreeCache cache = caches.get(path);
        if (cache != null) {
            cache.getListenable().removeListener(listener);
        }
        listener.removeDataListener();
    }

    /**
     * Handles actions when ZooKeeper connection is suspended (temporarily disconnected).
     *
     * @param sessionId The ID of the suspended ZooKeeper session (in hexadecimal format)
     */
    private void doSuspended(long sessionId) {

    }

    /**
     * Handles connection loss by initiating reconnection attempts.
     */
    private void doLost() {
        // close first
        client.close();
        boolean connected = this.connected.get();
        if (!connected && ensembleProvider.size() == 1) {
            // fail fast when initialization.
            connectLatch.countDown();
        } else {
            logger.info("Failed to connect {}, trying to reconnect...", ensembleProvider.current());
            // reconnect to next server
            ensembleProvider.next();
            addDetectTask(connected);
        }
    }

    /**
     * Schedules a ZooKeeper connection detection task.
     *
     * @param connected true if currently connected (will delay retry),
     *                  false for immediate detection attempt
     */
    private void addDetectTask(boolean connected) {
        long version = versions.incrementAndGet();
        CuratorDetectTask detect = new CuratorDetectTask(ensembleProvider, timeout, successThreshold, connected, new CuratorDetectTaskListener() {
            @Override
            public void onBefore() {
                if (client.getState() != CuratorFrameworkState.STOPPED) {
                    client.close();
                }
            }

            @Override
            public void onSuccess() {
                logger.info("Try connect to healthy zookeeper {}", ensembleProvider.current());
                client.start();
            }

            @Override
            public void onFailure() {
                connectLatch.countDown();
            }
        });
        RetryVersionTimerTask task = new RetryVersionTimerTask("zookeeper-detect", detect, version, predicate, timer);
        // fast to reconnect when initialization
        task.delay(connected ? Timer.getRetryInterval(1500, 5000) : 0);
    }

    /**
     * Handles successful reconnection and optionally initiates recovery to primary server.
     *
     * @param sessionId the established ZooKeeper session ID
     */
    private void doReconnected(long sessionId) {
        doConnected(sessionId);
    }

    /**
     * Handles successful connection and optionally initiates recovery to primary server.
     *
     * @param sessionId the established ZooKeeper session ID
     */
    private void doConnected(long sessionId) {
        // Discard running tasks
        long version = versions.incrementAndGet();
        connected.set(true);
        if (autoRecover) {
            String current = ensembleProvider.current();
            String first = ensembleProvider.first();
            if (!Objects.equals(current, first)) {
                logger.info("Try detect and recover {}...", first);
                CuratorRecoverTask execution = new CuratorRecoverTask(first, timeout, successThreshold, new CuratorDetectTaskListener() {
                    @Override
                    public void onSuccess() {
                        if (!Objects.equals(ensembleProvider.current(), first)) {
                            client.close();
                            // recover immediately
                            ensembleProvider.reset();
                            logger.info("Try switch to the healthy preferred zookeeper {}.", ensembleProvider.current());
                            client.start();
                        }
                    }
                });
                RetryVersionTimerTask task = new RetryVersionTimerTask("zookeeper-recover", execution, version, predicate, timer);
                task.delay(Timer.getRetryInterval(1500, 5000));
            }
        }
    }
}