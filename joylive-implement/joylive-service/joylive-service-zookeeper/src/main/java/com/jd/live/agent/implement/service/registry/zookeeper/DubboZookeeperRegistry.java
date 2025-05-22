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

import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.core.parser.json.JsonType;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.RegistryClusterConfig;
import com.jd.live.agent.governance.registry.*;
import com.jd.live.agent.governance.registry.RegistryDeltaEvent.EventType;
import lombok.Getter;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.jd.live.agent.core.util.CollectionUtils.singletonList;
import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.StringUtils.*;

/**
 * An implementation of the {@link Registry} interface specifically for Nacos.
 * This class provides functionality to register, unregister, and subscribe to services using Nacos.
 */
public class DubboZookeeperRegistry implements RegistryService {

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

    private final String address;

    private final String interfaceRoot;

    private final String serviceRoot;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private CuratorFramework client;

    private final Map<String, CuratorCache> caches = new ConcurrentHashMap<>(64);

    private final Map<String, PathData> nodes = new HashMap<>();

    private final Object mutex = new Object();

    public DubboZookeeperRegistry(RegistryClusterConfig config, Timer timer, ObjectParser parser) {
        this.config = config;
        this.timer = timer;
        this.parser = parser;
        this.address = join(toList(split(config.getAddress(), SEMICOLON_COMMA), URI::parse),
                uri -> uri.getAddress(true), CHAR_COMMA);
        this.name = "dubbo-zookeeper://" + address;
        this.interfaceRoot = url("/", config.getProperty("interfaceRoot", "dubbo"));
        this.serviceRoot = url("/", config.getProperty("serviceRoot", "services"));

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
            RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
            client = CuratorFrameworkFactory.builder()
                    .connectString(address)
                    .sessionTimeoutMs(3000)
                    .connectionTimeoutMs(5000)
                    .retryPolicy(retryPolicy)
                    .build();
            client.getConnectionStateListenable().addListener((c, newState) -> {
                if (newState == ConnectionState.RECONNECTED) {
                    onReconnected();
                }
            });
            client.start();
        }
    }

    @Override
    public void close() {
        if (started.compareAndSet(true, false)) {
            Close.instance().close(client);
            for (CuratorCache cache : caches.values()) {
                cache.close();
            }
        }
    }

    @Override
    public void register(ServiceId serviceId, ServiceInstance instance) throws Exception {
        if (!started.get()) {
            throw new IllegalStateException("Registry is not started");
        }
        PathData pathData = getPathData(serviceId, instance);
        if (pathData != null) {
            client.create().creatingParentContainersIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(pathData.getPath(), pathData.getData());
            synchronized (mutex) {
                nodes.put(pathData.path, pathData);
            }
        }
    }

    @Override
    public void unregister(ServiceId serviceId, ServiceInstance instance) throws Exception {
        String path = getPath(serviceId, instance);
        if (path != null) {
            remove(new PathData(path));
            synchronized (mutex) {
                nodes.remove(path);
            }
        }
    }

    @Override
    public void subscribe(ServiceId serviceId, Consumer<RegistryEvent> consumer) throws Exception {
        if (!started.get()) {
            throw new IllegalStateException("Registry is not started");
        }
        String path = getPath(serviceId, PROVIDERS, null);
        if (client != null) {
            caches.computeIfAbsent(path, p -> {
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
                cache.start();
                return cache;
            });
        }
    }

    @Override
    public void unsubscribe(ServiceId serviceId) throws Exception {
        String path = getPath(serviceId, PROVIDERS, null);
        Optional.ofNullable(caches.remove(path)).ifPresent(CuratorCache::close);
    }

    private void onReconnected() {
        synchronized (mutex) {
            if (!nodes.isEmpty()) {
                for (Map.Entry<String, PathData> entry : nodes.entrySet()) {
                    recreate(entry.getValue());
                }
            }
        }
    }

    private void recreate(PathData path) {
        if (started.get() && nodes.containsKey(path.getPath())) {
            try {
                if (remove(path)) {
                    // recreate
                    client.create().creatingParentContainersIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path.getPath(), path.getData());
                }
            } catch (Exception e) {
                addCreateTask(path);
            }
        }
    }

    private boolean remove(PathData path) throws Exception {
        Stat stat = client.checkExists().forPath(path.getPath());
        if (stat != null && stat.getEphemeralOwner() != client.getZookeeperClient().getZooKeeper().getSessionId()) {
            try {
                client.delete().forPath(path.getPath());
                return true;
            } catch (KeeperException.NoNodeException ignored) {
                return true;
            }
        }
        return false;
    }

    private void addCreateTask(PathData path) {
        if (client.getZookeeperClient().isConnected()) {
            timer.delay("recreate-node", 2000, () -> {
                synchronized (mutex) {
                    recreate(path);
                }
            });
        }
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
     * @param data       Zookeeper node data
     * @param serviceId  Service id
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
        Map<String, String> urlParams = params == null ? null : parser.read(new StringReader(params), new TypeReference<Map<String, String>>() {
        });
        String protocol = urlParams == null ? null : urlParams.get("protocol");
        protocol = protocol == null || protocol.isEmpty() ? "dubbo" : protocol;
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
}
