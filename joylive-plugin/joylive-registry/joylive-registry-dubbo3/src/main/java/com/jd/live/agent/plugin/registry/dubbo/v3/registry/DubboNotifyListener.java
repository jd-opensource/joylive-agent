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

import com.jd.live.agent.governance.registry.RegistryEvent;
import com.jd.live.agent.governance.registry.RegistryEventPublisher;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.registry.ServiceId;
import com.jd.live.agent.plugin.registry.dubbo.v3.instance.DubboEndpoint;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;

import java.util.ArrayList;
import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.plugin.registry.dubbo.v3.util.UrlUtils.toServiceId;
import static org.apache.dubbo.common.constants.RegistryConstants.*;

public class DubboNotifyListener implements NotifyListener {

    private final URL url;

    private final NotifyListener delegate;

    private final RegistryEventPublisher publisher;

    private final String defaultGroup;

    private final ServiceId serviceId;

    public DubboNotifyListener(URL url, NotifyListener delegate, RegistryEventPublisher publisher, String defaultGroup) {
        this.url = url;
        this.delegate = delegate;
        this.publisher = publisher;
        this.defaultGroup = defaultGroup;
        this.serviceId = toServiceId(url);
    }

    @Override
    public void notify(List<URL> urls) {
        if (!urls.isEmpty()) {
            URL url = urls.get(0);
            String category = url.getParameter(CATEGORY_KEY, DEFAULT_CATEGORY);
            if (PROVIDERS_CATEGORY.equalsIgnoreCase(category)) {
                // When all instances are down, the event includes a ServiceConfigURL with empty protocol.
                List<ServiceEndpoint> endpoints = EMPTY_PROTOCOL.equalsIgnoreCase(url.getProtocol()) ? new ArrayList<>() : toList(urls, DubboEndpoint::new);
                publisher.publish(new RegistryEvent(serviceId.getService(), serviceId.getGroup(), endpoints, defaultGroup));
            }
        }
        delegate.notify(urls);
    }

    @Override
    public void addServiceListener(ServiceInstancesChangedListener instanceListener) {
        delegate.addServiceListener(instanceListener);
    }
}
