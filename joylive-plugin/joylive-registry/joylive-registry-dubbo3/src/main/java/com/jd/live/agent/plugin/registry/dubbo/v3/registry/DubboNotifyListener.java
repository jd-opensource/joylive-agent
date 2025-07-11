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
package com.jd.live.agent.plugin.registry.dubbo.v3.registry;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.registry.*;
import com.jd.live.agent.plugin.registry.dubbo.v3.instance.DubboEndpoint;
import com.jd.live.agent.plugin.registry.dubbo.v3.util.UrlUtils;
import lombok.Getter;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceNotificationCustomizer;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.plugin.registry.dubbo.v3.util.UrlUtils.toInstance;
import static com.jd.live.agent.plugin.registry.dubbo.v3.util.UrlUtils.toServiceId;
import static org.apache.dubbo.common.constants.RegistryConstants.*;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.EXPORTED_SERVICES_REVISION_PROPERTY_NAME;
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

    private final RegistryEventPublisher publisher;

    private final String defaultGroup;

    private final Registry registry;

    private final ApplicationModel model;

    private final Set<ServiceInstanceNotificationCustomizer> customizers;

    private final BiFunction<String, List<ServiceInstance>, MetadataInfo> revisionFunc;

    private final AtomicBoolean started = new AtomicBoolean(false);

    public DubboNotifyListener(URL url,
                               NotifyListener delegate,
                               RegistryEventPublisher publisher,
                               String defaultGroup,
                               Registry registry,
                               ApplicationModel model,
                               Set<ServiceInstanceNotificationCustomizer> customizers,
                               BiFunction<String, List<ServiceInstance>, MetadataInfo> revisionFunc) {
        this(url, toServiceId(url), delegate, publisher, defaultGroup, registry, model, customizers, revisionFunc);
    }

    public DubboNotifyListener(URL url,
                               ServiceId serviceId,
                               NotifyListener delegate,
                               RegistryEventPublisher publisher,
                               String defaultGroup,
                               Registry registry,
                               ApplicationModel model,
                               Set<ServiceInstanceNotificationCustomizer> customizers,
                               BiFunction<String, List<ServiceInstance>, MetadataInfo> revisionFunc) {
        this.url = url;
        this.serviceId = serviceId;
        this.generic = url.getParameter(GENERIC_KEY, false) ? "generic service " : "";
        this.delegate = delegate;
        this.publisher = publisher;
        this.defaultGroup = defaultGroup;
        this.registry = registry;
        this.model = model;
        this.customizers = customizers;
        this.revisionFunc = revisionFunc;
    }

    @Override
    public URL getConsumerUrl() {
        return url;
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
    public ServiceInstancesChangedListener getServiceListener() {
        return delegate.getServiceListener();
    }

    @Override
    public void accept(RegistryEvent event) {
        List<ServiceEndpoint> endpoints = event.getInstances();
        List<ServiceInstance> instances = supply(endpoints);
        List<URL> urls = toList(instances, UrlUtils::toURL);
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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DubboNotifyListener)) return false;
        DubboNotifyListener that = (DubboNotifyListener) o;
        return Objects.equals(delegate, that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(delegate);
    }

    /**
     * Unsubscribes from service notifications.
     */
    private void unsubscribe() {
        if (started.compareAndSet(true, false)) {
            registry.unsubscribe(serviceId, this);
        }
    }

    /**
     * Converts a list of ServiceEndpoints to ServiceInstances with optional customization and metadata processing.
     *
     * @param endpoints the list of service endpoints to convert
     * @return list of processed service instances, empty list if input is null or empty
     */
    private List<ServiceInstance> supply(List<ServiceEndpoint> endpoints) {
        if (endpoints == null || endpoints.isEmpty()) {
            return Collections.emptyList();
        }
        List<ServiceInstance> instances = toList(endpoints, e -> toInstance(e, model));
        if (customizers != null && !customizers.isEmpty()) {
            // customize service instances.
            customizers.forEach(customizer -> customizer.customize(instances));
        }
        if (revisionFunc != null) {
            // Handle metadata info.
            Map<String, List<ServiceInstance>> revisions = new HashMap<>(4);
            // Sort by revision.
            for (ServiceInstance instance : instances) {
                String revision = instance.getMetadata(EXPORTED_SERVICES_REVISION_PROPERTY_NAME);
                if (revision != null && !revision.isEmpty()) {
                    revisions.computeIfAbsent(revision, k -> new ArrayList<>()).add(instance);
                }
            }
            // Update metadata info.
            for (Map.Entry<String, List<ServiceInstance>> entry : revisions.entrySet()) {
                MetadataInfo metadataInfo = revisionFunc.apply(entry.getKey(), entry.getValue());
                for (ServiceInstance instance : entry.getValue()) {
                    instance.setServiceMetadata(metadataInfo);
                }
            }
        }
        return instances;
    }

}
