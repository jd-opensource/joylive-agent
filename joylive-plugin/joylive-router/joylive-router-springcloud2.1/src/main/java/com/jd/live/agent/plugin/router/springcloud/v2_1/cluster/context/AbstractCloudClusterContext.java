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
package com.jd.live.agent.plugin.router.springcloud.v2_1.cluster.context;

import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.registry.ServiceRegistryFactory;
import com.jd.live.agent.governance.request.ServiceRequest;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.client.config.IClientConfigKey;
import lombok.Getter;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerContext;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.http.HttpMethod;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getQuietly;
import static com.jd.live.agent.core.util.CollectionUtils.singletonList;
import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.StringUtils.splitList;

/**
 * Abstract implementation of CloudClusterContext for load balancing and service instance management
 * Provides core functionalities for load balancer property retrieval and service instance suppliers
 */
@Getter
public abstract class AbstractCloudClusterContext implements CloudClusterContext {

    private static final String TYPE_SPRING_CLIENT_FACTORY = "org.springframework.cloud.netflix.ribbon.SpringClientFactory";

    private static final IClientConfigKey<String> RETRYABLE_STATUS_CODES = new CommonClientConfigKey<String>(
            "retryableStatusCodes") {
    };

    @Getter
    protected Registry registry;

    protected ServiceRegistryFactory system;

    protected LoadBalancedRetryFactory retryFactory;

    public AbstractCloudClusterContext(Registry registry) {
        this.registry = registry;
    }

    @Override
    public boolean isRetryable() {
        return retryFactory != null;
    }

    @Override
    public RetryPolicy getDefaultRetryPolicy(String service) {
        Object value = getQuietly(retryFactory, "clientFactory");
        if (value == null || !value.getClass().getName().equals(TYPE_SPRING_CLIENT_FACTORY)) {
            return null;
        }
        SpringClientFactory clientFactory = (SpringClientFactory) value;
        IClientConfig config = clientFactory.getClientConfig(service);
        boolean retryEnabled = config.get(CommonClientConfigKey.OkToRetryOnAllOperations, false);
        if (!retryEnabled) {
            return null;
        }
        RibbonLoadBalancerContext lbContext = clientFactory.getLoadBalancerContext(service);
        RetryHandler retryHandler = lbContext.getRetryHandler();
        RetryPolicy retryPolicy = new RetryPolicy();
        retryPolicy.setRetry(retryHandler.getMaxRetriesOnNextServer());
        List<Class<? extends Throwable>> exceptions = getQuietly(retryHandler, "retriable");
        if (exceptions != null && !exceptions.isEmpty()) {
            retryPolicy.setExceptions(new HashSet<>(toList(exceptions, Class::getName)));
        }
        if (!lbContext.isOkToRetryOnAllOperations()) {
            retryPolicy.setMethods(new HashSet<>(singletonList(HttpMethod.GET.name())));
        }
        String retryableStatus = config.getPropertyAsString(RETRYABLE_STATUS_CODES, "");
        List<String> statuses = splitList(retryableStatus, ',');
        if (statuses != null && !statuses.isEmpty()) {
            retryPolicy.setErrorCodes(new HashSet<>(statuses));
        }
        return retryPolicy;
    }

    @Override
    public CompletionStage<List<ServiceEndpoint>> getEndpoints(ServiceRequest request) {
        return registry.getEndpoints(request.getService(), request.getGroup(), system);
    }

    @Override
    public boolean isInstanceSensitive() {
        return system != null;
    }
}
