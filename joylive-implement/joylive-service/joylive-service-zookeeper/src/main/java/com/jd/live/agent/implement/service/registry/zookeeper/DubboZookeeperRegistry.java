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

import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.StringUtils;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.config.RegistryClusterConfig;
import com.jd.live.agent.governance.registry.*;
import com.jd.live.agent.governance.registry.RegistryDeltaEvent.EventType;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
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
    private final RegistryClusterConfig config;

    private final String name;

    private final String address;

    private final String root;

    private CuratorFramework client;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private final Map<String, CuratorCache> caches = new ConcurrentHashMap<>(64);

    public DubboZookeeperRegistry(RegistryClusterConfig config) {
        this.config = config;
        this.address = join(toList(split(config.getAddress(), SEMICOLON_COMMA), URI::parse),
                uri -> uri.getAddress(true), CHAR_COMMA);
        this.name = "dubbo-zookeeper://" + address;
        this.root = url("/", config.getProperty("root", "dubbo"));
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
            client.start();
        }
    }

    @Override
    public void close() {
        if (started.compareAndSet(true, false)) {
            Close.instance().close(client);
        }
    }

    @Override
    public void register(String service, String group, ServiceInstance instance) throws Exception {
        String path = getPath(service, group, instance);
        if (path != null) {
            client.create().creatingParentContainersIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path);
        }
    }

    @Override
    public void unregister(String service, String group, ServiceInstance instance) throws Exception {
        String path = getPath(service, group, instance);
        if (path != null) {
            client.delete().forPath(path);
        }
    }

    @Override
    public void subscribe(String service, String group, Consumer<RegistryEvent> consumer) throws Exception {
        String path = getPath(service, group, PROVIDERS, null);
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
                    DubboZookeeperEndpoint endpoint = getInstance(service, event.getData());
                    if (endpoint != null && match(group, endpoint.getGroup())) {
                        consumer.accept(new RegistryDeltaEvent(service, group, singletonList(endpoint), eventType));
                    }
                }
            }).build();
            cache.listenable().addListener(listener);
            cache.start();
            return cache;
        });
    }

    @Override
    public void unsubscribe(String service, String group) throws Exception {
        String path = getPath(service, group, PROVIDERS, null);
        Optional.ofNullable(caches.remove(path)).ifPresent(CuratorCache::close);
    }

    /**
     * Builds service path in format: {root}/{service}
     */
    private String getPath(String service, String group) {
        return url(root, service);
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
     * Converts Zookeeper node data into service endpoint.
     * @param service service name context
     * @param data Zookeeper node data
     * @return endpoint instance or null if conversion fails
     */
    private DubboZookeeperEndpoint getInstance(String service, ChildData data) {
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
        return DubboZookeeperEndpoint.builder()
                .service(service)
                .group(uri.getParameter("group"))
                .host(uri.getHost())
                .port(uri.getPort())
                .metadata(parameters == null ? null : new HashMap<>(parameters))
                .build();
    }

    /**
     * Builds full Zookeeper path for service instance registration.
     * @return path string or null if node encoding fails
     */
    private String getPath(String service, String group, ServiceInstance instance) {
        Map<String, String> metadata = instance.getMetadata();
        String side = metadata == null ? null : metadata.get(SIDE);
        side = side == null || side.isEmpty() ? PROVIDER : side;
        String role = PROVIDER.equals(side) ? PROVIDERS : CONSUMERS;
        String node = getNode(instance);
        return node == null ? null : getPath(service, group, role, node);
    }

    /**
     * Constructs Zookeeper path with role-specific segment.
     * Format: {root}/{service}/{role}/{node}
     */
    private String getPath(String service, String group, String role, String node) {
        return StringUtils.url(this.getPath(service, group), role, node);
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
}
