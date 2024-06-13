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
package com.jd.live.agent.plugin.router.springgateway.v3.cluster;

import com.jd.live.agent.core.util.type.ClassDesc;
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.core.util.type.FieldDesc;
import com.jd.live.agent.core.util.type.FieldList;
import com.jd.live.agent.governance.invoke.cluster.ClusterInvoker;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.plugin.router.springcloud.v3.cluster.AbstractClientCluster;
import com.jd.live.agent.plugin.router.springcloud.v3.instance.SpringEndpoint;
import com.jd.live.agent.plugin.router.springgateway.v3.request.GatewayClusterRequest;
import com.jd.live.agent.plugin.router.springgateway.v3.response.GatewayClusterResponse;
import lombok.Getter;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory.RetryConfig;
import org.springframework.cloud.gateway.support.DelegatingServiceInstance;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools.reconstructURI;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

public class GatewayCluster extends AbstractClientCluster<GatewayClusterRequest, GatewayClusterResponse> {

    private static final String FIELD_CLIENT_FACTORY = "clientFactory";

    @Getter
    private final LoadBalancerClientFactory clientFactory;

    public GatewayCluster(GlobalFilter filter) {
        ClassDesc describe = ClassUtils.describe(filter.getClass());
        FieldList fieldList = describe.getFieldList();
        FieldDesc field = fieldList.getField(FIELD_CLIENT_FACTORY);
        this.clientFactory = (LoadBalancerClientFactory) (field == null ? null : field.get(filter));
    }

    @Override
    public ClusterPolicy getDefaultPolicy(GatewayClusterRequest request) {
        RetryConfig retryConfig = request.getRetryConfig();
        if (retryConfig != null && retryConfig.getRetries() > 0) {
            RetryGatewayFilterFactory.BackoffConfig backoff = retryConfig.getBackoff();
            RetryPolicy retryPolicy = new RetryPolicy();
            retryPolicy.setRetry(retryConfig.getRetries());
            retryPolicy.setRetryInterval(backoff != null ? backoff.getFirstBackoff().toMillis() : null);
            retryPolicy.setRetryStatuses(retryConfig.getStatuses().stream().map(v -> String.valueOf(v.value())).collect(Collectors.toSet()));
            retryPolicy.setRetryExceptions(retryConfig.getExceptions().stream().map(v -> v.getClass().getName()).collect(Collectors.toSet()));
            return new ClusterPolicy(ClusterInvoker.TYPE_FAILOVER, retryPolicy);
        }
        return new ClusterPolicy(ClusterInvoker.TYPE_FAILFAST);
    }

    @Override
    protected boolean isRetryable() {
        return true;
    }

    @Override
    public CompletionStage<GatewayClusterResponse> invoke(GatewayClusterRequest request, SpringEndpoint endpoint) {
        Mono<Void> mono = request.getChain().filter(request.getExchange());
        return mono.toFuture().thenApply(v -> new GatewayClusterResponse(request.getExchange().getResponse()));
    }

    @Override
    public GatewayClusterResponse createResponse(Throwable throwable, GatewayClusterRequest request, SpringEndpoint endpoint) {
        return new GatewayClusterResponse(createException(throwable, request, endpoint));
    }

    @Override
    public void onStartRequest(GatewayClusterRequest request, SpringEndpoint endpoint) {
        if (endpoint != null) {
            ServiceInstance instance = endpoint.getInstance();
            URI uri = request.getRequest().getURI();
            // if the `lb:<scheme>` mechanism was used, use `<scheme>` as the default,
            // if the loadbalancer doesn't provide one.
            String overrideScheme = instance.isSecure() ? "https" : "http";
            Map<String, Object> attributes = request.getExchange().getAttributes();
            String schemePrefix = (String) attributes.get(GATEWAY_SCHEME_PREFIX_ATTR);
            if (schemePrefix != null) {
                overrideScheme = request.getURI().getScheme();
            }
            URI requestUrl = reconstructURI(new DelegatingServiceInstance(instance, overrideScheme), uri);

            attributes.put(GATEWAY_REQUEST_URL_ATTR, requestUrl);
            attributes.put(GATEWAY_LOADBALANCER_RESPONSE_ATTR, new DefaultResponse(instance));
        }
        super.onStartRequest(request, endpoint);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(GatewayClusterResponse response, GatewayClusterRequest request, SpringEndpoint endpoint) {
        LoadBalancerProperties properties = request.getProperties();
        boolean useRawStatusCodeInResponseData = properties != null && properties.isUseRawStatusCodeInResponseData();
        request.lifecycles(l -> l.onComplete(new CompletionContext<>(
                CompletionContext.Status.SUCCESS,
                request.getLbRequest(),
                endpoint.getResponse(),
                useRawStatusCodeInResponseData
                        ? new ResponseData(new RequestData(request.getRequest()), response.getResponse())
                        : new ResponseData(response.getResponse(), new RequestData(request.getRequest())))));
    }
}
