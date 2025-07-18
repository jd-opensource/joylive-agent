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
import com.jd.live.agent.core.util.option.Converts;
import com.jd.live.agent.core.util.task.RetryExecution;
import com.jd.live.agent.core.util.task.RetryVersionTask;
import com.jd.live.agent.core.util.task.RetryVersionTimerTask;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.probe.DetectTaskListener;
import com.jd.live.agent.governance.probe.FailoverDetectTask;
import com.jd.live.agent.governance.probe.FailoverRecoverTask;
import com.jd.live.agent.governance.probe.HealthProbe;
import com.jd.live.agent.plugin.registry.dubbo.v2_7.zookeeper.CuratorExecution.CuratorVoidExecution;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.retry.RetryNTimes;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.zookeeper.ChildListener;
import org.apache.dubbo.remoting.zookeeper.DataListener;
import org.apache.dubbo.remoting.zookeeper.StateListener;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.jd.live.agent.core.util.StringUtils.join;
import static com.jd.live.agent.core.util.StringUtils.splitList;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;

/**
 * ZooKeeper client with failover support using Apache Curator framework.
 */
public class CuratorFailoverClient implements ZookeeperClient {

    private static final Logger logger = LoggerFactory.getLogger(CuratorFailoverClient.class);
    private static final String ZK_SESSION_EXPIRE_KEY = "zk.session.expire";

    private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 5 * 1000;
    private static final int DEFAULT_SESSION_TIMEOUT_MS = 30 * 1000;

    private final URL url;
    private final Timer timer;
    private final HealthProbe probe;
    private final int connectionTimeout;
    private final int sessionTimeout;
    private final int initializationTimeout;
    private final int successThreshold;
    private final boolean autoRecover;

    private final Set<StateListener> stateListeners = new CopyOnWriteArraySet<>();
    private final List<String> addresses;
    private final CuratorFailoverEnsembleProvider ensembleProvider;
    private final FailoverStateListener stateListener;
    private final Function<String, TreeCache> cacheFactory;
    private final Predicate<RetryVersionTask> predicate;
    private final PathChildWatcher watcher;
    private volatile CuratorFramework client;

    private final Map<String, PathData> paths = new ConcurrentHashMap<>(16);
    private final Map<String, PathChildListener> childListeners = new ConcurrentHashMap<>(16);
    private final Map<String, PathDataListener> dataListeners = new ConcurrentHashMap<>();

    private final CountDownLatch connectLatch = new CountDownLatch(1);
    private final AtomicLong versions = new AtomicLong(0);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean started = new AtomicBoolean(true);

    public CuratorFailoverClient(URL url, Timer timer, HealthProbe probe) {
        this.url = url;
        this.timer = timer;
        this.probe = probe;
        this.connectionTimeout = Converts.getPositive(url.getParameter(TIMEOUT_KEY, System.getenv("ZOOKEEPER_CONNECTION_TIMEOUT")), DEFAULT_CONNECTION_TIMEOUT_MS);
        this.sessionTimeout = Converts.getPositive(url.getParameter(ZK_SESSION_EXPIRE_KEY, System.getenv("ZOOKEEPER_SESSION_TIMEOUT")), DEFAULT_SESSION_TIMEOUT_MS);
        this.successThreshold = Converts.getPositive(url.getParameter("successThreshold", System.getenv("ZOOKEEPER_SUCCESS_THRESHOLD")), 3);
        this.autoRecover = Converts.getBoolean(url.getParameter("autoRecover", System.getenv("ZOOKEEPER_AUTO_RECOVER")), true);
        this.addresses = getAddresses(url);
        this.initializationTimeout = Converts.getPositive(url.getParameter("initializationTimeout", System.getenv("ZOOKEEPER_INITIALIZATION_TIMEOUT")), connectionTimeout * addresses.size());
        this.ensembleProvider = new CuratorFailoverEnsembleProvider(addresses);
        this.stateListener = new FailoverStateListener();
        this.stateListeners.add(stateListener);
        this.cacheFactory = path -> TreeCache.newBuilder(client, path).setCacheData(false).build();
        this.predicate = v -> started.get() && v.getVersion() == versions.get();
        this.watcher = (p, w) -> client.getChildren().usingWatcher(w).forPath(p);
        this.client = createClient(url);
        try {
            // Replace client.start() with addDetectTask
            logger.info("Try detecting healthy zookeeper {}", join(addresses, ';'));
            stateListener.addDetectTask(false);
            // wait for connected
            if (!connectLatch.await(initializationTimeout, TimeUnit.MILLISECONDS)) {
                // cancel task.
                versions.incrementAndGet();
                throw new IllegalStateException("It's timeout to connect to zookeeper.");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public void addDataListener(String path, DataListener listener) {
        addDataListener(path, listener, null);
    }

    @Override
    public void addDataListener(String path, DataListener listener, Executor executor) {
        PathDataListener dataListener = dataListeners.computeIfAbsent(path, p -> new PathDataListener(p, cacheFactory, executor));
        if (dataListener.addListener(listener) && isConnected()) {
            dataListener.start();
        }
    }

    @Override
    public void removeDataListener(String path, DataListener listener) {
        PathDataListener dataListener = dataListeners.get(path);
        if (dataListener != null && dataListener.removeListener(listener) && dataListener.isEmpty()) {
            dataListeners.remove(path);
            dataListener.close();
        }
    }

    @Override
    public List<String> addChildListener(String path, ChildListener listener) {
        PathChildListener children = childListeners.computeIfAbsent(path, p -> new PathChildListener(path, watcher));
        if (isConnected()) {
            children.start();
        }
        children.addListener(listener);
        // use cached data
        return children.getChildren();
    }

    @Override
    public void removeChildListener(String path, ChildListener listener) {
        PathChildListener children = childListeners.get(path);
        if (children != null && children.removeListener(listener) && children.isEmpty()) {
            childListeners.remove(path);
            children.close();
        }
    }

    @Override
    public void addStateListener(StateListener listener) {
        stateListeners.add(listener);
    }

    @Override
    public void removeStateListener(StateListener listener) {
        stateListeners.remove(listener);
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public String getContent(String path) {
        return CuratorTask.of(client).execute(new PathData(path), (pathData, client) -> {
            try {
                byte[] dataBytes = client.getData().forPath(path);
                return (dataBytes == null || dataBytes.length == 0) ? null : new String(dataBytes, StandardCharsets.UTF_8);
            } catch (KeeperException.NoNodeException e) {
                return null;
            }
        });
    }

    @Override
    public boolean isConnected() {
        return client.getZookeeperClient().isConnected();
    }

    @Override
    public void close() {
        if (started.compareAndSet(true, false)) {
            dataListeners.forEach((k, v) -> v.close());
            childListeners.forEach((k, v) -> v.close());
            client.close();
        }
    }

    @Override
    public void create(String path, boolean ephemeral) {
        createOrUpdate(path, null, ephemeral);
    }

    @Override
    public void create(String path, String content, boolean ephemeral) {
        byte[] data = content == null ? null : content.getBytes(StandardCharsets.UTF_8);
        createOrUpdate(path, data, ephemeral);
    }

    @Override
    public void delete(String path) {
        CuratorTask.of(client).execute(new PathData(path), new CuratorVoidExecution() {
            @Override
            protected void doExecute(PathData pathData, CuratorFramework client) throws Exception {
                try {
                    client.delete().deletingChildrenIfNeeded().forPath(path);
                    paths.remove(path);
                } catch (KeeperException.NoNodeException e) {
                    paths.remove(path);
                }
            }
        });
    }

    @Override
    public List<String> getChildren(String path) {
        return CuratorTask.of(client).execute(new PathData(path), (pathData, client) -> {
            try {
                return client.getChildren().forPath(path);
            } catch (KeeperException.NoNodeException e) {
                return new ArrayList<>(0);
            }
        });
    }

    @Override
    public boolean checkExists(String path) {
        return CuratorTask.of(client).execute(new PathData(path), (pathData, client) -> client.checkExists().forPath(path) != null);
    }

    /**
     * Creates or updates a ZNode with optional ephemeral flag.
     *
     * <p>Behavior when node exists:
     * <ul>
     *   <li>For null/default data: Logs warning (retries for ephemeral nodes)</li>
     *   <li>For new data: Updates existing node</li>
     * </ul>
     *
     * @param path ZNode path to create/update
     * @param data Data to store (null/default skips update)
     * @param ephemeral If true, creates ephemeral node (retries on conflict)
     */
    private void createOrUpdate(String path, byte[] data, boolean ephemeral) {
        CuratorTask.of(client).execute(new PathData(path, data, !ephemeral), new CuratorVoidExecution() {
            @Override
            public void doExecute(PathData pathData, CuratorFramework client) throws Exception {
                try {
                    client.create()
                            .creatingParentsIfNeeded()
                            .withMode(ephemeral ? CreateMode.EPHEMERAL : CreateMode.PERSISTENT)
                            .forPath(pathData.getPath(), pathData.getData());
                    paths.put(pathData.getPath(), pathData);
                } catch (KeeperException.NodeExistsException e) {
                    if (data == null || data == PathData.DEFAULT_DATA) {
                        if (ephemeral) {
                            delete(path);
                            createOrUpdate(path, data, true);
                        }
                    } else {
                        // Update data
                        client.setData().forPath(pathData.getPath(), pathData.getData());
                        paths.put(pathData.getPath(), pathData);
                    }
                }
            }
        });
    }

    /**
     * Retrieves a list of failover addresses from the given URL.
     *
     * <p>The address list consists of:
     * <ol>
     *   <li>The primary backup address from {@link URL#getBackupAddress()}</li>
     *   <li>Additional failover addresses parsed from the "failovers" URL parameter (semicolon-delimited)</li>
     * </ol>
     *
     * @param url The URL containing address configuration
     * @return List of all available addresses (primary backup first, then failovers)
     * @see URL#getBackupAddress()
     */
    private List<String> getAddresses(URL url) {
        List<String> result = new ArrayList<>(2);
        result.add(url.getBackupAddress());
        result.addAll(splitList(url.getParameter("failovers", System.getenv("ZOOKEEPER_FAILOVER_ADDRESS")), ';'));
        return result;
    }

    /**
     * Creates and configures a new CuratorFramework client instance based on the provided URL.
     *
     * @param url The configuration URL containing:
     *            - Authority information (for optional authentication)
     *            - Other connection parameters
     * @return A configured but unstarted CuratorFramework client instance
     */
    private CuratorFramework createClient(URL url) {
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .ensembleProvider(ensembleProvider)
                .retryPolicy(new RetryNTimes(1, 1000))
                .connectionTimeoutMs(connectionTimeout)
                .sessionTimeoutMs(sessionTimeout)
                .defaultData(PathData.DEFAULT_DATA);
        String authority = url.getAuthority();
        if (authority != null && !authority.isEmpty()) {
            builder = builder.authorization("digest", authority.getBytes());
        }
        CuratorFramework client = builder.build();
        // listener keep session id, so create new one.
        client.getConnectionStateListenable().addListener(new CuratorConnectionStateListener(stateListeners, connectionTimeout, sessionTimeout));
        return client;
    }

    /**
     * A failover-aware connection state listener.
     */
    private class FailoverStateListener implements StateListener {

        FailoverStateListener() {
        }

        @Override
        public void stateChanged(int state) {
            switch (state) {
                case StateListener.CONNECTED:
                    doConnected();
                    // Notify main thread which is waiting for connection
                    connectLatch.countDown();
                    break;
                case StateListener.SESSION_LOST:
                    doSessionLost();
                    break;
                case StateListener.RECONNECTED:
                    doReconnected();
                    break;
                case StateListener.NEW_SESSION_CREATED:
                    doReconnectedNewSession();
                    break;
                case StateListener.SUSPENDED:
                default:
                    doSuspended();
                    break;
            }
        }

        /**
         * Handles actions when ZooKeeper connection is suspended (temporarily disconnected).
         */
        protected void doSuspended() {

        }

        /**
         * Handles connection loss by initiating reconnection attempts.
         */
        protected void doSessionLost() {
            // close first
            client.close();
            boolean flag = connected.get();
            if (!flag && ensembleProvider.size() == 1) {
                // fail fast when initialization.
                connectLatch.countDown();
            } else {
                logger.info("Failed to connect {}, trying to reconnect...", ensembleProvider.current());
                // reconnect to next server
                ensembleProvider.next();
                addDetectTask(flag);
            }
        }

        /**
         * Schedules a ZooKeeper connection detection task.
         *
         * @param connected true if currently connected (will delay retry),
         *                  false for immediate detection attempt
         */
        protected void addDetectTask(boolean connected) {
            long version = versions.incrementAndGet();
            FailoverDetectTask detect = new FailoverDetectTask(ensembleProvider, probe, successThreshold, connected, new DetectTaskListener() {

                @Override
                public void onSuccess() {
                    logger.info("Try connecting to healthy zookeeper {}", ensembleProvider.current());
                    // recreate client
                    client = createClient(url);
                    client.start();
                }

                @Override
                public void onFailure() {
                    connectLatch.countDown();
                }
            });
            RetryVersionTimerTask task = new RetryVersionTimerTask("zookeeper.detect", detect, version, predicate, timer);
            // fast to reconnect when initialization
            task.delay(connected ? Timer.getRetryInterval(1500, 5000) : 0);
        }

        /**
         * Performs actions when the ZooKeeper client successfully reconnects to the server.
         */
        protected void doReconnected() {
        }

        /**
         * Handles actions required when a new ZooKeeper session is created.
         */
        protected void doReconnectedNewSession() {
            // Discard running tasks
            long version = versions.incrementAndGet();
            if (connected.get()) {
                doRecreate(version);
            }
        }

        /**
         * Handles successful connection and optionally initiates recovery to primary server.
         */
        protected void doConnected() {
            // Discard running tasks
            long version = versions.incrementAndGet();
            connected.set(true);
            if (autoRecover) {
                doRecover(version);
            }
            if (connected.get()) {
                doRecreate(version);
            }
        }

        /**
         * Attempts to detect and recover connection to the preferred ZooKeeper ensemble server.
         *
         * @param version The version number used for tracking recovery attempts and ensuring
         *                only the latest recovery task executes when multiple are scheduled
         */
        protected void doRecover(long version) {
            String current = ensembleProvider.current();
            String first = ensembleProvider.first();
            if (Objects.equals(current, first)) {
                return;
            }
            logger.info("Try detecting unhealthy preferred zookeeper {}...", first);
            FailoverRecoverTask execution = new FailoverRecoverTask(first, probe, successThreshold, () -> {
                if (!Objects.equals(ensembleProvider.current(), first)) {
                    // recover immediately
                    client.close();
                    ensembleProvider.reset();
                    logger.info("Try switching to the healthy preferred zookeeper {}.", ensembleProvider.current());
                    client = createClient(url);
                    client.start();
                }
            });
            RetryVersionTimerTask task = new RetryVersionTimerTask("zookeeper.recover", execution, version, predicate, timer);
            task.delay(Timer.getRetryInterval(1500, 5000));
        }

        /**
         * Recreates all registered paths and caches with retry logic and version control.
         *
         * @param version the current version number used to prevent stale recreations
         *                (only tasks matching this version will execute)
         */
        protected void doRecreate(long version) {
            AtomicInteger pathCounter = new AtomicInteger(paths.size());
            AtomicInteger childCounter = new AtomicInteger(childListeners.size());
            AtomicInteger cacheCounter = new AtomicInteger(dataListeners.size());
            logger.info("Try recreating paths {}, children watchers {}, caches {} at {}", pathCounter.get(), childCounter.get(), cacheCounter.get(), ensembleProvider.current());

            paths.forEach((path, data) -> {
                RetryVersionTimerTask task = new RetryVersionTimerTask("zookeeper.recreate.path", new RecreatePath(data, pathCounter), version, predicate, timer);
                task.delay(Timer.getRetryInterval(100, 500));
            });

            // recreate all children
            childListeners.forEach((path, data) -> {
                RetryVersionTimerTask task = new RetryVersionTimerTask("zookeeper.recreate.children", new RecreateChildren(path, childCounter), version, predicate, timer);
                task.delay(Timer.getRetryInterval(100, 500));
            });

            // recreate all caches
            dataListeners.forEach((path, data) -> {
                RetryVersionTimerTask task = new RetryVersionTimerTask("zookeeper.recreate.cache", new RecreateCache(path, cacheCounter), version, predicate, timer);
                task.delay(Timer.getRetryInterval(100, 500));
            });
        }
    }

    /**
     * A retryable execution task for creating ZooKeeper nodes (both persistent and ephemeral).
     * Implements {@link RetryExecution} to support retry logic with a fixed interval.
     */
    private class RecreatePath implements RetryExecution {

        private final PathData data;

        private final AtomicInteger counter;

        RecreatePath(PathData data, AtomicInteger counter) {
            this.data = data;
            this.counter = counter;
        }

        @Override
        public long getRetryInterval() {
            return Timer.getRetryInterval(1000, 1000);
        }

        @Override
        public Boolean call() {
            createOrUpdate(data.getPath(), data.getData(), !data.isPersistent());
            if (counter.decrementAndGet() == 0) {
                logger.info("Complete recreate paths at {}", ensembleProvider.current());
            }
            return true;
        }
    }

    /**
     * Retryable task for recreating a path cache with its listeners.
     */
    private class RecreateCache implements RetryExecution {

        private final String path;

        private final AtomicInteger counter;

        RecreateCache(String path, AtomicInteger counter) {
            this.path = path;
            this.counter = counter;
        }

        @Override
        public long getRetryInterval() {
            return Timer.getRetryInterval(200, 500);
        }

        @Override
        public Boolean call() {
            PathDataListener dataListener = dataListeners.get(path);
            if (dataListener != null) {
                dataListener.recreate();
            }
            if (counter.decrementAndGet() == 0) {
                logger.info("Complete recreate caches at {}", ensembleProvider.current());
            }
            return true;
        }
    }

    /**
     * Retryable task for recreating child node watchers.
     */
    private class RecreateChildren implements RetryExecution {

        private final String path;

        private final AtomicInteger counter;

        RecreateChildren(String path, AtomicInteger counter) {
            this.path = path;
            this.counter = counter;
        }

        @Override
        public long getRetryInterval() {
            return Timer.getRetryInterval(200, 500);
        }

        @Override
        public Boolean call() {
            PathChildListener children = childListeners.get(path);
            if (children != null) {
                children.recreate();
            }
            if (counter.decrementAndGet() == 0) {
                logger.info("Complete recreate children watchers at {}", ensembleProvider.current());
            }
            return true;
        }
    }
}