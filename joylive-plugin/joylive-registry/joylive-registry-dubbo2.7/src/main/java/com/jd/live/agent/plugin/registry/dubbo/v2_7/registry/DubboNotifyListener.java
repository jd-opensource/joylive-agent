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
package com.jd.live.agent.plugin.registry.dubbo.v2_7.registry;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.registry.*;
import com.jd.live.agent.plugin.registry.dubbo.v2_7.instance.DubboEndpoint;
import lombok.Getter;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.plugin.registry.dubbo.v2_7.util.UrlUtils.toURL;
import static org.apache.dubbo.common.constants.RegistryConstants.*;
import static org.apache.dubbo.rpc.Constants.GENERIC_KEY;

/**
 * Dubbo notification listener that bridges between Dubbo's NotifyListener and RegistryEvent system.
 * Handles service instance changes and converts between Dubbo URL and RegistryEvent formats.
 */
public class DubboNotifyListener implements NotifyListener, Consumer<RegistryEvent>, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(DubboNotifyListener.class);

    @Getter
    private final URL url;

    @Getter
    private final ServiceId serviceId;

    private final String generic;

    @Getter
    private final NotifyListener delegate;

    private final ServiceDiscovery discovery;

    private final RegistryEventPublisher publisher;

    private final String defaultGroup;

    private final Registry registry;

    private final AtomicBoolean started = new AtomicBoolean(false);

    public DubboNotifyListener(URL url,
                               ServiceId serviceId,
                               NotifyListener delegate,
                               RegistryEventPublisher publisher,
                               ServiceDiscovery discovery,
                               String defaultGroup,
                               Registry registry) {
        this.url = url;
        this.serviceId = serviceId;
        this.generic = url.getParameter(GENERIC_KEY, false) ? "generic service " : "";
        this.delegate = delegate;
        this.discovery = discovery;
        this.publisher = publisher;
        this.defaultGroup = defaultGroup;
        this.registry = registry;
    }

    @Override
    public void notify(List<URL> urls) {
        if (!urls.isEmpty()) {
            URL url = urls.get(0);
            String category = url.getParameter(CATEGORY_KEY, DEFAULT_CATEGORY);
            if (PROVIDERS_CATEGORY.equalsIgnoreCase(category)) {
                // When all instances are down, the event includes a ServiceConfigURL with empty protocol.
                List<ServiceEndpoint> endpoints = EMPTY_PROTOCOL.equalsIgnoreCase(url.getProtocol())
                        ? new ArrayList<>()
                        : toList(urls, u -> new DubboEndpoint(u, serviceId));
                publisher.publish(new RegistryEvent(serviceId, endpoints, defaultGroup));
            }
        }
    }

    @Override
    public void addServiceListener(ServiceInstancesChangedListener instanceListener) {
        delegate.addServiceListener(instanceListener);
    }

    @Override
    public void accept(RegistryEvent event) {
        List<ServiceEndpoint> endpoints = event.getInstances();
        List<URL> urls = toList(endpoints, e -> toURL(serviceId, e, url, discovery));
        try {
            delegate.notify(urls);
            logger.info("Dubbo registry notify event {} instances for {}{}", urls.size(), generic, serviceId.getUniqueName());
        } catch (Throwable e) {
            logger.error("Failed to notify service change event for {}{}", generic, serviceId.getUniqueName(), e);
        }
    }

    /**
     * Starts listening for service changes.
     */
    public void start() {
        if (started.compareAndSet(false, true)) {
            registry.subscribe(serviceId, this);
        }
    }

    @Override
    public void close() {
        unsubscribe();
    }

    /**
     * Unsubscribes from service notifications.
     */
    private void unsubscribe() {
        if (started.compareAndSet(true, false)) {
            registry.unsubscribe(serviceId, this);
        }
    }
}
