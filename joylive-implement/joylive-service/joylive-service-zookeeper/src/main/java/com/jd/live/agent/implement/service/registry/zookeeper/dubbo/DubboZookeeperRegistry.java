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
package com.jd.live.agent.implement.service.registry.zookeeper.dubbo;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.Executors;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.converter.BiConverter;
import com.jd.live.agent.core.util.converter.TriConverter;
import com.jd.live.agent.core.util.option.Converts;
import com.jd.live.agent.core.util.option.MapOption;
import com.jd.live.agent.core.util.option.Option;
import com.jd.live.agent.core.util.task.RetryExecution;
import com.jd.live.agent.core.util.task.RetryVersionTask;
import com.jd.live.agent.core.util.task.RetryVersionTimerTask;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.RegistryClusterConfig;
import com.jd.live.agent.governance.probe.HealthProbe;
import com.jd.live.agent.governance.registry.*;
import com.jd.live.agent.implement.service.registry.zookeeper.PathData;
import com.jd.live.agent.implement.service.registry.zookeeper.dubbo.converter.*;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.jd.live.agent.core.util.CollectionUtils.shuffle;
import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.StringUtils.*;
import static com.jd.live.agent.core.util.time.Timer.getRetryInterval;

/**
 * An implementation of the {@link Registry} interface specifically for Nacos.
 * This class provides functionality to register, unregister, and subscribe to services using Nacos.
 */
public class DubboZookeeperRegistry implements RegistryService {

    private static final Logger logger = LoggerFactory.getLogger(DubboZookeeperRegistry.class);

    private static final String PROVIDERS = "providers";
    private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 5 * 1000;
    private static final int DEFAULT_SESSION_TIMEOUT_MS = 30 * 1000;

    private final RegistryClusterConfig config;
    private final Timer timer;
    private final HealthProbe probe;

    private final String name;
    private final List<URI> uris;
    private final String address;
    private final int connectionTimeout;
    private final int sessionTimeout;

    private final TriConverter<ServiceId, String, String, String> servicePathConverter;
    private final BiConverter<ServiceId, ServiceInstance, String> instancePathConverter;
    private final BiConverter<ServiceId, ServiceInstance, PathData> instancePathDataConverter;
    private final BiConverter<ServiceId, ChildData, DubboZookeeperEndpoint> endpointConverter;
    private final DubboCuratorCacheFactory cacheFactory;

    private CuratorFramework client;

    private final Map<String, DubboCuratorCache> caches = new ConcurrentHashMap<>(64);
    private final Map<String, PathData> nodes = new ConcurrentHashMap<>();
    private final AtomicLong versions = new AtomicLong(0);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final Predicate<RetryVersionTask> predicate = t -> t.getVersion() == versions.get();
    private final AtomicBoolean started = new AtomicBoolean(false);

    public DubboZookeeperRegistry(RegistryClusterConfig config, Timer timer, ObjectParser parser, HealthProbe probe) {
        this.config = config;
        this.timer = timer;
        this.probe = probe;
        this.uris = shuffle(toList(split(config.getAddress(), SEMICOLON_COMMA), URI::parse));
        this.address = join(uris, uri -> uri.getAddress(true), CHAR_COMMA);
        this.name = "dubbo-zookeeper://" + address;
        Option option = MapOption.of(config.getProperties());
        this.connectionTimeout = Converts.getPositive(option.getString("connectionTimeout", System.getenv("ZOOKEEPER_CONNECTION_TIMEOUT")), DEFAULT_CONNECTION_TIMEOUT_MS);
        this.sessionTimeout = Converts.getPositive(option.getString("sessionTimeout", System.getenv("ZOOKEEPER_SESSION_TIMEOUT")), DEFAULT_SESSION_TIMEOUT_MS);

        String interfaceRoot = url("/", config.getProperty("interfaceRoot", "dubbo"));
        String serviceRoot = url("/", config.getProperty("serviceRoot", "services"));
        this.servicePathConverter = new ServicePathConverter(interfaceRoot, serviceRoot);
        this.instancePathConverter = new InstancePathConverter(new NodeConverter(), servicePathConverter);
        this.instancePathDataConverter = new InstancePathDataConverter(parser, instancePathConverter);
        this.endpointConverter = new EndpointConverter(parser);
        this.cacheFactory = (serviceId, path, consumer) -> {
            CuratorCache cache = CuratorCache.build(client, path);
            CuratorCacheListener listener = CuratorCacheListener.builder()
                    .forPathChildrenCache(path, client, new DubboChildrenCacheListener(serviceId, consumer, endpointConverter))
                    .build();
            cache.listenable().addListener(listener);
            return cache;
        };
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return name;
    }

    @Override
    public RegistryClusterConfig getConfig() {
        return config;
    }

    @Override
    public void start() throws Exception {
        if (started.compareAndSet(false, true)) {
            logger.info("Start connecting to {}.", name);
            addDetectTask(versions.get(), getRetryInterval(100, 1000));
        }
    }

    @Override
    public void close() {
        if (started.compareAndSet(true, false)) {
            // Stop tasks
            versions.incrementAndGet();
            Close.instance().close(client);
            for (CuratorCache cache : caches.values()) {
                cache.close();
            }
        }
    }

    @Override
    public void register(ServiceId serviceId, ServiceInstance instance) {
        if (!started.get()) {
            throw new IllegalStateException("Registry is not started");
        }
        PathData pathData = instancePathDataConverter.convert(serviceId, instance);
        if (pathData != null) {
            nodes.put(pathData.getPath(), pathData);
            long version = versions.get();
            if (connected.get()) {
                addCreationTask(version, pathData);
            }
        }
    }

    @Override
    public void unregister(ServiceId serviceId, ServiceInstance instance) throws Exception {
        String path = instancePathConverter.convert(serviceId, instance);
        if (path != null) {
            nodes.remove(path);
            if (connected.get() && started.get()) {
                remove(client, new PathData(path));
            }
        }
    }

    @Override
    public void subscribe(ServiceId serviceId, Consumer<RegistryEvent> consumer) {
        if (!started.get()) {
            throw new IllegalStateException("Registry is not started");
        }
        String path = servicePathConverter.convert(serviceId, PROVIDERS, null);
        DubboCuratorCache dubboCache = caches.computeIfAbsent(path,
                p -> new DubboCuratorCache(serviceId, path, consumer, cacheFactory));
        if (connected.get()) {
            dubboCache.start();
        }
    }

    @Override
    public void unsubscribe(ServiceId serviceId) {
        String path = servicePathConverter.convert(serviceId, PROVIDERS, null);
        Optional.ofNullable(caches.remove(path)).ifPresent(CuratorCache::close);
    }

    /**
     * Creates and configures a new CuratorFramework client instance.
     *
     * @return configured CuratorFramework client with connection settings and optional authentication
     */
    private CuratorFramework createClient() {
        return Executors.get(this.getClass().getClassLoader(), () -> {
            CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                    .connectString(address)
                    .sessionTimeoutMs(sessionTimeout)
                    .connectionTimeoutMs(connectionTimeout)
                    .retryPolicy(new RetryNTimes(1, 1000));
            String authority = config.getAuthority();
            if (authority != null && !authority.isEmpty()) {
                builder.authorization("digest", authority.getBytes());
            }
            return builder.build();
        });
    }

    /**
     * Schedules cache tasks for all registered caches at the specified version.
     *
     * @param version The current operation version for task execution control
     */
    private void addCacheTask(long version) {
        for (Map.Entry<String, DubboCuratorCache> entry : caches.entrySet()) {
            addCacheTask(version, entry.getValue());
        }
    }

    /**
     * Creates and schedules a cache synchronization task for a specific cache.
     *
     * @param version The current operation version
     * @param cache   The cache instance to synchronize
     */
    private void addCacheTask(long version, DubboCuratorCache cache) {
        BuildCacheExecution creation = new BuildCacheExecution(cache);
        RetryVersionTask task = new RetryVersionTimerTask("CacheTask", creation, version, predicate, timer);
        task.delay(getRetryInterval(100, 500));
    }

    /**
     * Creates and schedules a ZooKeeper server detection task.
     *
     * @param version The current operation version
     * @param delay   Initial delay before first execution (milliseconds)
     */
    private void addDetectTask(long version, long delay) {
        DetectExecution detection = new DetectExecution();
        RetryVersionTask task = new RetryVersionTimerTask("DetectTask", detection, version, predicate, timer);
        task.delay(delay);
    }

    /**
     * Schedules node creation tasks for all registered nodes at the specified version.
     *
     * @param version The current operation version for task execution control
     */
    private void addCreationTask(long version) {
        for (Map.Entry<String, PathData> entry : nodes.entrySet()) {
            addCreationTask(version, entry.getValue());
        }
    }

    /**
     * Creates and schedules a ZooKeeper node creation task for a specific path.
     *
     * @param version  The current operation version
     * @param pathData The node path and data to create
     */
    private void addCreationTask(long version, PathData pathData) {
        CreatePathExecution creation = new CreatePathExecution(pathData);
        RetryVersionTask task = new RetryVersionTimerTask("CreationTask", creation, version, predicate, timer);
        task.delay(getRetryInterval(100, 500));
    }

    /**
     * Removes a ZooKeeper node if it exists and is not owned by the current session.
     * <p>
     * Only removes nodes that are either persistent or ephemeral nodes owned by other sessions.
     *
     * @param client Curator client instance
     * @param path   Path and data of the node to remove
     * @return true if the node was removed or didn't exist, false if node belongs to current session
     * @throws Exception if ZooKeeper operations fail
     */
    private static boolean remove(CuratorFramework client, PathData path) throws Exception {
        Stat stat = client.checkExists().forPath(path.getPath());
        if (stat == null) {
            return true;
        } else if (stat.getEphemeralOwner() == client.getZookeeperClient().getZooKeeper().getSessionId()) {
            try {
                client.delete().forPath(path.getPath());
                return true;
            } catch (KeeperException.NoNodeException e) {
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        // Ephemeral nodes owned by other sessions cannot be deleted.
        return false;
    }

    /**
     * A task that creates or recreates a ZooKeeper node with ephemeral mode.
     */
    private class CreatePathExecution implements RetryExecution {

        private final PathData path;

        CreatePathExecution(PathData path) {
            this.path = path;
        }

        @Override
        public Boolean call() throws Exception {
            if (nodes.containsKey(path.getPath()) && remove(client, path)) {
                // recreate
                create();
            }
            return true;
        }

        @Override
        public long getRetryInterval() {
            return Timer.getRetryInterval(1000, 1000);
        }

        /**
         * Creates an ephemeral ZNode, retrying if it already exists by first deleting it.
         *
         * @throws Exception if creation fails after retry or other errors occur
         */
        private void create() throws Exception {
            try {
                client.create()
                        .creatingParentContainersIfNeeded()
                        .withMode(CreateMode.EPHEMERAL)
                        .forPath(path.getPath(), path.getData());
            } catch (KeeperException.NodeExistsException e) {
                if (delete()) {
                    create();
                } else {
                    throw e;
                }
            }
        }

        /**
         * Attempts to delete the ZNode.
         *
         * @return true if deleted or already non-existent, false if deletion failed
         */
        private boolean delete() {
            try {
                client.delete().forPath(path.getPath());
                return true;
            } catch (KeeperException.NoNodeException e) {
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * A task that detects ZooKeeper server availability by probing multiple URIs.
     */
    private class DetectExecution implements RetryExecution {

        private final AtomicLong counter = new AtomicLong(0);

        @Override
        public Boolean call() {
            if (counter.incrementAndGet() % 50 == 0) {
                logger.error("Retry connecting to zookeeper {}, {} times", name, counter.get());
            }
            // the uris is shuffled to avoid the same server is probed by many clients.
            if (probe.test(address)) {
                logger.info("Try connecting the healthy zookeeper {}.", name);
                client = createClient();
                client.getConnectionStateListenable().addListener(new DubboConnectionListener());
                client.start();
                return true;
            }
            return false;
        }

        @Override
        public long getRetryInterval() {
            return Timer.getRetryInterval(1500, 5000);
        }
    }

    /**
     * A task that starts a {@link DubboCuratorCache} and returns its started status.
     */
    private static class BuildCacheExecution implements RetryExecution {

        private final DubboCuratorCache cache;

        BuildCacheExecution(DubboCuratorCache cache) {
            this.cache = cache;
        }

        @Override
        public Boolean call() {
            cache.recreate();
            return true;
        }

        @Override
        public long getRetryInterval() {
            return Timer.getRetryInterval(1000, 1000);
        }
    }

    /**
     * Handles ZooKeeper connection state changes and session management.
     */
    private class DubboConnectionListener implements ConnectionStateListener {

        private long lastSessionId = -1L;

        @Override
        public void stateChanged(CuratorFramework client, ConnectionState newState) {
            if (newState == ConnectionState.RECONNECTED) {
                // A suspended, lost, or read-only connection has been re-established
                long sessionId = getSessionId(lastSessionId);
                if (lastSessionId == sessionId) {
                    onReconnected();
                } else {
                    lastSessionId = sessionId;
                    onReconnectedNewSession();
                }
            } else if (newState == ConnectionState.LOST) {
                // The ZooKeeper session has expired
                onSessionLost();
            } else if (newState == ConnectionState.CONNECTED) {
                lastSessionId = getSessionId(0);
                // Sent for the first successful connection to the server
                onConnected();
            }
        }

        /**
         * Handles successful ZooKeeper connection by incrementing version and scheduling creation/cache tasks.
         */
        private void onConnected() {
            if (!started.get()) {
                return;
            }
            long version = versions.incrementAndGet();
            connected.set(true);
            recreate(version);
            logger.info("Success connecting to {}", name);
        }

        /**
         * Handles ZooKeeper reconnection event by incrementing version and scheduling node creation tasks.
         */
        private void onReconnected() {
        }

        /**
         * Handles reconnection by creating a new session.
         */
        private void onReconnectedNewSession() {
            if (!started.get()) {
                return;
            }
            long version = versions.incrementAndGet();
            recreate(version);
        }

        /**
         * Handles ZooKeeper session lost.
         */
        private void onSessionLost() {
            if (!started.get()) {
                return;
            }
            long version = versions.incrementAndGet();
            connected.set(false);
            client.close();
            logger.info("Failed to connect {}, trying to reconnect...", name);
            addDetectTask(version, getRetryInterval(1000, 1000));
        }

        /**
         * Recreates session components by scheduling creation and cache tasks.
         *
         * @param version the version number for the new session
         */
        private void recreate(long version) {
            addCreationTask(version);
            addCacheTask(version);
        }

        /**
         * Gets the current ZooKeeper session ID, falling back to input value on error.
         *
         * @param sessionId the fallback session ID if retrieval fails
         * @return current ZooKeeper session ID or fallback value
         */
        private long getSessionId(long sessionId) {
            try {
                return client.getZookeeperClient().getZooKeeper().getSessionId();
            } catch (Exception ignored) {
                // ignore
                return sessionId;
            }
        }
    }

}
