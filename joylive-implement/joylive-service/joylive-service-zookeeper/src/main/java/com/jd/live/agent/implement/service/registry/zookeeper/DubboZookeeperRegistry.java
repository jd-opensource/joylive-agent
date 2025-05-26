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
package com.jd.live.agent.implement.service.registry.zookeeper;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.core.parser.json.JsonType;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.SocketDetector;
import com.jd.live.agent.core.util.SocketDetector.ZookeeperSocketListener;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.option.MapOption;
import com.jd.live.agent.core.util.option.Option;
import com.jd.live.agent.core.util.task.RetryExecution;
import com.jd.live.agent.core.util.task.RetryVersionTask;
import com.jd.live.agent.core.util.task.RetryVersionTimerTask;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.RegistryClusterConfig;
import com.jd.live.agent.governance.registry.*;
import com.jd.live.agent.governance.registry.RegistryDeltaEvent.EventType;
import lombok.Getter;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.listen.Listenable;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.jd.live.agent.core.util.CollectionUtils.*;
import static com.jd.live.agent.core.util.StringUtils.*;
import static com.jd.live.agent.core.util.time.Timer.getRetryInterval;

/**
 * An implementation of the {@link Registry} interface specifically for Nacos.
 * This class provides functionality to register, unregister, and subscribe to services using Nacos.
 */
public class DubboZookeeperRegistry implements RegistryService {

    private static final Logger logger = LoggerFactory.getLogger(DubboZookeeperRegistry.class);

    private static final String DUBBO = "dubbo";
    private static final String PROTOCOL = "protocol";
    private static final String PROVIDERS = "providers";
    private static final String CONSUMERS = "consumers";
    private static final String SIDE = "side";
    private static final String PROVIDER = "provider";
    private static final String GROUP = "group";
    private static final String CATEGORY = "category";

    private final RegistryClusterConfig config;

    private final Timer timer;

    private final ObjectParser parser;

    private final String name;

    private final List<URI> uris;

    private final String address;

    private final String interfaceRoot;

    private final String serviceRoot;

    private final int connectTimeout;

    private final int sessionTimeout;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private CuratorFramework client;

    private final Map<String, DubboCuratorCache> caches = new ConcurrentHashMap<>(64);

    private final Map<String, PathData> nodes = new HashMap<>();

    private final AtomicLong versions = new AtomicLong(0);

    private final AtomicBoolean connected = new AtomicBoolean(false);

    private final Predicate<RetryVersionTask> predicate = t -> t.getVersion() == versions.get();

    public DubboZookeeperRegistry(RegistryClusterConfig config, Timer timer, ObjectParser parser) {
        this.config = config;
        this.timer = timer;
        this.parser = parser;
        this.uris = shuffle(toList(split(config.getAddress(), SEMICOLON_COMMA), URI::parse));
        this.address = join(uris, uri -> uri.getAddress(true), CHAR_COMMA);
        this.name = "dubbo-zookeeper://" + address;
        this.interfaceRoot = url("/", config.getProperty("interfaceRoot", "dubbo"));
        this.serviceRoot = url("/", config.getProperty("serviceRoot", "services"));
        Option option = new MapOption(config.getProperties());
        this.connectTimeout = option.getInteger("connectTimeout", 1000);
        this.sessionTimeout = option.getInteger("sessionTimeout", 20000);
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
            RetryPolicy retryPolicy = new RetryNTimes(1, 1000);
            CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                    .connectString(address)
                    .sessionTimeoutMs(sessionTimeout)
                    .connectionTimeoutMs(connectTimeout)
                    .retryPolicy(retryPolicy);
            String authority = config.getAuthority();
            if (authority != null && !authority.isEmpty()) {
                builder.authorization("digest", authority.getBytes());
            }
            client = builder.build();
            client.getConnectionStateListenable().addListener((c, newState) -> {
                if (newState == ConnectionState.RECONNECTED) {
                    // A suspended, lost, or read-only connection has been re-established
                    onReconnected();
                } else if (newState == ConnectionState.LOST) {
                    // The ZooKeeper session has expired
                    onLost();
                } else if (newState == ConnectionState.CONNECTED) {
                    // Sent for the first successful connection to the server
                    onConnected();
                }
            });
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
        PathData pathData = getPathData(serviceId, instance);
        if (pathData != null) {
            nodes.put(pathData.path, pathData);
            long version = versions.get();
            if (connected.get()) {
                addCreationTask(version, pathData);
            }
        }
    }

    @Override
    public void unregister(ServiceId serviceId, ServiceInstance instance) throws Exception {
        String path = getPath(serviceId, instance);
        if (path != null) {
            nodes.remove(path);
            if (connected.get()) {
                remove(client, new PathData(path));
            }
        }
    }

    @Override
    public void subscribe(ServiceId serviceId, Consumer<RegistryEvent> consumer) {
        if (!started.get()) {
            throw new IllegalStateException("Registry is not started");
        }
        String path = getPath(serviceId, PROVIDERS, null);
        if (client != null) {
            DubboCuratorCache dubboCache = caches.computeIfAbsent(path, p -> {
                CuratorCache cache = CuratorCache.build(client, p);
                CuratorCacheListener listener = CuratorCacheListener.builder().forPathChildrenCache(path, client, (client, event) -> {
                    EventType eventType = null;
                    switch (event.getType()) {
                        case CHILD_ADDED:
                            eventType = EventType.ADD;
                            break;
                        case CHILD_REMOVED:
                            eventType = EventType.REMOVE;
                            break;
                        case CHILD_UPDATED:
                            eventType = EventType.UPDATE;
                            break;
                    }
                    if (eventType != null) {
                        DubboZookeeperEndpoint endpoint = getInstance(event.getData(), serviceId);
                        if (endpoint != null && match(serviceId.getGroup(), endpoint.getGroup())) {
                            consumer.accept(new RegistryDeltaEvent(serviceId, singletonList(endpoint), eventType));
                        }
                    }
                }).build();
                cache.listenable().addListener(listener);
                return new DubboCuratorCache(cache);
            });
            if (connected.get()) {
                dubboCache.start();
            }
        }
    }

    @Override
    public void unsubscribe(ServiceId serviceId) {
        String path = getPath(serviceId, PROVIDERS, null);
        Optional.ofNullable(caches.remove(path)).ifPresent(CuratorCache::close);
    }

    /**
     * Handles ZooKeeper reconnection event by incrementing version and scheduling node creation tasks.
     */
    private void onReconnected() {
        long version = versions.incrementAndGet();
        if (!started.get()) {
            return;
        }
        addCreationTask(version);
        if (connected.compareAndSet(false, true)) {
            addCacheTask(version);
        }
    }

    /**
     * Handles ZooKeeper connection loss by incrementing version, closing client, and initiating detection.
     */
    private void onLost() {
        long version = versions.incrementAndGet();
        if (!started.get()) {
            return;
        }
        connected.set(false);
        client.close();
        addDetectTask(version, getRetryInterval(1000, 1000));
        logger.info("Failed to connect {}, trying to reconnect...", name);
    }

    /**
     * Handles successful ZooKeeper connection by incrementing version and scheduling creation/cache tasks.
     */
    private void onConnected() {
        long version = versions.incrementAndGet();
        if (!started.get()) {
            return;
        }
        connected.set(true);
        addCreationTask(version);
        addCacheTask(version);
        logger.info("Success connecting to {}", name);
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
        CacheExecution creation = new CacheExecution(cache);
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
        DetectExecution detection = new DetectExecution(name, uris, client, connectTimeout);
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
        CreateExecution creation = new CreateExecution(client, pathData, nodes);
        RetryVersionTask task = new RetryVersionTimerTask("CreationTask", creation, version, predicate, timer);
        task.delay(getRetryInterval(100, 500));
    }

    /**
     * Encodes service instance into URL format for Zookeeper node.
     *
     * @return URL-encoded string or null if encoding fails
     */
    private String getNode(ServiceInstance instance) {
        URI uri = URI.builder()
                .schema(instance.getScheme())
                .host(instance.getHost())
                .port(instance.getPort())
                .path(instance.getService())
                .parameters(instance.getMetadata() == null ? null : new TreeMap<>(instance.getMetadata()))
                .build();
        try {
            return URLEncoder.encode(uri.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    /**
     * Builds full Zookeeper path for service instance registration.
     *
     * @return path string or null if node encoding fails
     */
    private PathData getPathData(ServiceId serviceId, ServiceInstance instance) {
        String path = getPath(serviceId, instance);
        byte[] data = new byte[0];
        if (!instance.isInterfaceMode()) {
            String address = instance.getAddress();
            ZookeeperInstance zi = new ZookeeperInstance(address, serviceId.getService(), instance.getMetadata());
            CuratorInstance<ZookeeperInstance> ci = new CuratorInstance<>(address, serviceId.getService(), instance.getHost(), instance.getPort(), zi);
            StringWriter writer = new StringWriter(2048);
            parser.write(writer, ci);
            data = writer.toString().getBytes(StandardCharsets.UTF_8);
        }
        return path == null ? null : new PathData(path, data);
    }

    /**
     * Builds full Zookeeper path for service instance registration.
     *
     * @return path string or null if node encoding fails
     */
    private String getPath(ServiceId serviceId, ServiceInstance instance) {
        if (serviceId.isInterfaceMode()) {
            Map<String, String> metadata = instance.getMetadata();
            String role = instance.getMetadata(CATEGORY, null);
            if (role == null || role.isEmpty()) {
                String side = metadata == null ? null : metadata.get(SIDE);
                side = side == null || side.isEmpty() ? PROVIDER : side;
                role = PROVIDER.equals(side) ? PROVIDERS : CONSUMERS;
            }

            String node = getNode(instance);
            return node == null ? null : getPath(serviceId, role, node);
        } else {
            return getPath(serviceId, null, instance.getAddress());
        }
    }

    /**
     * Constructs Zookeeper path with role-specific segment.
     * Format: {root}/{service}/{role}/{node}
     */
    private String getPath(ServiceId serviceId, String role, String node) {
        if (!serviceId.isInterfaceMode()) {
            return url(serviceRoot, serviceId.getService(), node);
        }
        return url(url(interfaceRoot, serviceId.getService()), role, node);
    }

    /**
     * Checks if two service groups match (null/empty considered equal).
     *
     * @return true if groups are equivalent
     */
    private boolean match(String group1, String group2) {
        if (group1 == null || group1.isEmpty()) {
            return group2 == null || group2.isEmpty();
        }
        return group1.equals(group2);
    }

    /**
     * Converts Zookeeper node data into service endpoint.
     *
     * @param data      Zookeeper node data
     * @param serviceId Service id
     * @return endpoint instance or null if conversion fails
     */
    private DubboZookeeperEndpoint getInstance(ChildData data, ServiceId serviceId) {
        if (serviceId.isInterfaceMode()) {
            return getInterfaceInstance(data, serviceId);
        } else {
            return getServiceInstance(data, serviceId);
        }
    }

    /**
     * Creates DubboZookeeperEndpoint from Zookeeper service node data.
     *
     * @param data      Zookeeper node data
     * @param serviceId target service ID
     * @return endpoint instance or null if invalid
     */
    private DubboZookeeperEndpoint getServiceInstance(ChildData data, ServiceId serviceId) {
        CuratorInstance<ZookeeperInstance> instance = parser.read(
                new InputStreamReader(new ByteArrayInputStream(data.getData())),
                new TypeReference<CuratorInstance<ZookeeperInstance>>() {
                });
        ZookeeperInstance payload = instance.getPayload();
        Map<String, String> parameters = payload == null ? null : payload.getMetadata();
        String group = parameters == null ? null : parameters.get(GROUP);
        group = group == null || group.isEmpty() ? serviceId.getGroup() : group;
        String params = parameters == null ? null : parameters.get("dubbo.metadata-service.url-params");
        boolean version2 = params != null && params.contains("\"release\":\"2");
        String protocol = version2 ? getProtocol2(params, parser) : getProtocol3(params, parser);
        protocol = protocol == null || protocol.isEmpty() ? DUBBO : protocol;
        return DubboZookeeperEndpoint.builder()
                .scheme(protocol)
                .service(serviceId.getService())
                .group(group)
                .host(instance.getAddress())
                .port(instance.getPort())
                .metadata(parameters)
                .build();
    }

    /**
     * Extracts the protocol type (version 2 format) from URL parameters.
     *
     * @param params URL parameters string to parse (may be null)
     * @param parser Object parser instance for parameter deserialization
     * @return "dubbo" if Dubbo protocol is found, null otherwise
     */
    private String getProtocol2(String params, ObjectParser parser) {
        try {
            Map<String, Map<String, String>> urlParams = params == null ? null : parser.read(new StringReader(params), new TypeReference<Map<String, Map<String, String>>>() {
            });
            return urlParams == null || !urlParams.containsKey(DUBBO) ? null : DUBBO;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts the protocol type (version 3 format) from URL parameters.
     *
     * @param params URL parameters string to parse (may be null)
     * @param parser Object parser instance for parameter deserialization
     * @return The protocol string if found, null otherwise
     */
    private String getProtocol3(String params, ObjectParser parser) {
        try {
            Map<String, String> urlParams = params == null ? null : parser.read(new StringReader(params), new TypeReference<Map<String, String>>() {
            });
            return urlParams == null ? null : urlParams.get(PROTOCOL);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Creates DubboZookeeperEndpoint from Zookeeper interface node path.
     *
     * @param data      Zookeeper node data
     * @param serviceId target service ID
     * @return endpoint instance or null if invalid
     */
    private DubboZookeeperEndpoint getInterfaceInstance(ChildData data, ServiceId serviceId) {
        String path = data.getPath();
        int pos = path.lastIndexOf('/');
        path = pos >= 0 ? path.substring(pos + 1) : path;
        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (Exception ignored) {
        }
        URI uri = URI.parse(path);
        if (uri == null) {
            return null;
        }
        Map<String, String> parameters = uri.getParameters();
        String group = uri.getParameter(GROUP);
        group = group == null || group.isEmpty() ? serviceId.getGroup() : group;
        return DubboZookeeperEndpoint.builder()
                .service(serviceId.getService())
                .group(group)
                .scheme(uri.getSchema())
                .host(uri.getHost())
                .port(uri.getPort())
                .metadata(parameters == null ? null : new HashMap<>(parameters))
                .build();
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
        } else if (stat.getEphemeralOwner() != client.getZookeeperClient().getZooKeeper().getSessionId()) {
            try {
                client.delete().forPath(path.getPath());
                return true;
            } catch (KeeperException.NoNodeException ignored) {
                return true;
            }
        }
        return false;
    }

    @Getter
    private static class PathData {

        private final String path;

        private final byte[] data;

        PathData(String path) {
            this(path, new byte[0]);
        }

        PathData(String path, byte[] data) {
            this.path = path;
            this.data = data;
        }

    }

    @Getter
    private static class ZookeeperInstance {

        private final String id;

        private final String name;

        private final Map<String, String> metadata;

        ZookeeperInstance(String id, String name, Map<String, String> metadata) {
            this.id = id;
            this.name = name;
            this.metadata = metadata;
        }
    }

    @Getter
    private static class CuratorInstance<T> {

        private final String name;
        private final String id;
        private final String address;
        private final Integer port;
        private final Integer sslPort;
        @JsonType("org.apache.dubbo.registry.zookeeper.ZookeeperInstance")
        private final T payload;
        private final long registrationTimeUTC;
        private final String serviceType;

        CuratorInstance(String id, String name, String address, Integer port, T payload) {
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

    /**
     * A task that creates or recreates a ZooKeeper node with ephemeral mode.
     */
    private static class CreateExecution implements RetryExecution {

        private final CuratorFramework client;

        private final PathData path;

        private final Map<String, PathData> nodes;

        CreateExecution(CuratorFramework client, PathData path, Map<String, PathData> nodes) {
            this.client = client;
            this.path = path;
            this.nodes = nodes;
        }

        @Override
        public Boolean call() throws Exception {
            if (nodes.containsKey(path.getPath()) && remove(client, path)) {
                // recreate
                client.create().creatingParentContainersIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path.getPath(), path.getData());
            }
            return true;
        }

        @Override
        public long getRetryInterval() {
            return Timer.getRetryInterval(1000, 1000);
        }
    }

    /**
     * A task that detects ZooKeeper server availability by probing multiple URIs.
     */
    private static class DetectExecution implements RetryExecution {

        private final String name;

        private final List<URI> uris;

        private final CuratorFramework client;

        private final AtomicLong counter = new AtomicLong(0);

        private final SocketDetector detector;

        DetectExecution(String name, List<URI> uris, CuratorFramework client, int connectTimeout) {
            this.name = name;
            this.uris = uris;
            this.client = client;
            this.detector = new SocketDetector(connectTimeout, 2181, new ZookeeperSocketListener());
        }

        @Override
        public Boolean call() {
            if (client.getState() != CuratorFrameworkState.STOPPED) {
                client.close();
            }
            // the uris is shuffled to avoid the same server is probed by many clients.
            for (URI uri : uris) {
                if (detector.test(uri.getHost(), uri.getPort())) {
                    client.start();
                    return true;
                }
            }
            if (counter.incrementAndGet() % 50 == 0) {
                logger.error("Retry connecting to zookeeper {} times, {}", counter.get(), name);
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
    private static class CacheExecution implements RetryExecution {

        private final DubboCuratorCache cache;

        CacheExecution(DubboCuratorCache cache) {
            this.cache = cache;
        }

        @Override
        public Boolean call() {
            cache.start();
            return cache.isStarted();
        }

        @Override
        public long getRetryInterval() {
            return Timer.getRetryInterval(1000, 1000);
        }
    }

    /**
     * A wrapper implementation of {@link CuratorCache} that adds atomic start/close control
     * and delegates all operations to the underlying CuratorCache instance.
     */
    private static class DubboCuratorCache implements CuratorCache {

        private final CuratorCache delegate;

        private final AtomicBoolean started = new AtomicBoolean(false);

        DubboCuratorCache(CuratorCache delegate) {
            this.delegate = delegate;
        }

        public boolean isStarted() {
            return started.get();
        }

        @Override
        public void start() {
            if (started.compareAndSet(false, true)) {
                try {
                    delegate.start();
                } catch (Exception e) {
                    started.set(false);
                }
            }
        }

        @Override
        public void close() {
            if (started.compareAndSet(true, false)) {
                delegate.close();
            }
        }

        @Override
        public Listenable<CuratorCacheListener> listenable() {
            return delegate.listenable();
        }

        @Override
        public Optional<ChildData> get(String path) {
            return delegate.get(path);
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public Stream<ChildData> stream() {
            return delegate.stream();
        }
    }
}
