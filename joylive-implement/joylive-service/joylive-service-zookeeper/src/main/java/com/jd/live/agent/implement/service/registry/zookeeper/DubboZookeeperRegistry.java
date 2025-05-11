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
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.config.RegistryClusterConfig;
import com.jd.live.agent.governance.registry.*;
import com.jd.live.agent.governance.registry.RegistryDeltaEvent.EventType;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
        String path = getPath(service, group);
        caches.computeIfAbsent(path, p -> {
            CuratorCache cache = CuratorCache.build(client, p);
            cache.listenable().addListener((type, oldData, newData) -> {
                DubboZookeeperEndpoint endpoint = getInstance(service, newData != null ? newData : oldData);
                if (endpoint != null && match(group, endpoint.getGroup())) {
                    switch (type) {
                        case NODE_CREATED:
                            consumer.accept(new RegistryDeltaEvent(service, endpoint.getGroup(), singletonList(endpoint), EventType.ADD));
                            break;
                        case NODE_DELETED:
                            consumer.accept(new RegistryDeltaEvent(service, endpoint.getGroup(), singletonList(endpoint), EventType.REMOVE));
                            break;
                        case NODE_CHANGED:
                        default:
                            consumer.accept(new RegistryDeltaEvent(service, endpoint.getGroup(), singletonList(endpoint), EventType.UPDATE));
                            break;
                    }
                }
            });
            cache.start();
            return cache;
        });
    }

    @Override
    public void unsubscribe(String service, String group) throws Exception {
        String path = getPath(service, group);
        Optional.ofNullable(caches.remove(path)).ifPresent(CuratorCache::close);
    }

    private String getPath(String service, String group) {
        return url(root, service);
    }

    private String getNode(ServiceInstance instance) {
        URI uri = URI.builder().schema(instance.getScheme()).host(instance.getHost()).port(instance.getPort()).parameters(instance.getMetadata()).build();
        try {
            return URLEncoder.encode(uri.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

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

    private String getPath(String service, String group, ServiceInstance instance) {
        String node = getNode(instance);
        return node == null ? null : url(getPath(service, group), "providers", node);
    }

    private boolean match(String group1, String group2) {
        if (group1 == null || group1.isEmpty()) {
            return group2 == null || group2.isEmpty();
        }
        return group1.equals(group2);
    }
}
