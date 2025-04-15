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
package com.jd.live.agent.plugin.router.springcloud.v4.request;

import com.jd.live.agent.core.util.cache.CacheObject;
import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v4.cluster.context.CloudClusterContext;
import com.jd.live.agent.plugin.router.springcloud.v4.cluster.context.RequestLifecycle;
import com.jd.live.agent.plugin.router.springcloud.v4.cluster.context.ServiceContext;
import com.jd.live.agent.plugin.router.springcloud.v4.response.SpringClusterResponse;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.jd.live.agent.plugin.router.springcloud.v4.instance.SpringEndpoint.getResponse;

/**
 * Represents an outbound HTTP request in a reactive microservices architecture,
 * extending the capabilities of an abstract HTTP outbound request model to include
 * client-specific functionalities. This class encapsulates features such as load balancing,
 * service instance discovery, and lifecycle management, making it suitable for handling
 * dynamic client requests in a distributed system.
 */
public abstract class AbstractCloudClusterRequest<T, C extends CloudClusterContext> extends AbstractHttpOutboundRequest<T> implements SpringClusterRequest {

    protected final C context;

    protected final ServiceContext serviceContext;

    protected Request<?> lbRequest;

    protected RequestData requestData;

    protected CacheObject<String> stickyId;

    public AbstractCloudClusterRequest(T request, URI uri, C context) {
        super(request);
        this.uri = uri;
        this.context = context;
        String service = getService();
        // depend on url
        this.serviceContext = context.getServiceContext(service);
        this.requestData = buildRequestData();
        this.lbRequest = buildLbRequest(service, requestData);
    }

    @Override
    public String getCookie(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        } else if (request instanceof ServerHttpRequest) {
            ServerHttpRequest httpRequest = (ServerHttpRequest) request;
            HttpCookie cookie = httpRequest.getCookies().getFirst(key);
            return cookie == null ? null : cookie.getValue();
        } else {
            return super.getCookie(key);
        }
    }

    @Override
    public String getStickyId() {
        if (stickyId == null) {
            stickyId = new CacheObject<>(buildStickyId());
        }
        return stickyId.get();
    }

    @Override
    public RetryPolicy getDefaultRetryPolicy() {
        LoadBalancerProperties properties = serviceContext.getLoadBalancerProperties();
        LoadBalancerProperties.Retry retry = properties == null ? null : properties.getRetry();
        if (retry != null && retry.isEnabled() && (getHttpMethod() == HttpMethod.GET || retry.isRetryOnAllOperations())) {
            Set<String> statuses = new HashSet<>(retry.getRetryableStatusCodes().size());
            retry.getRetryableStatusCodes().forEach(status -> statuses.add(String.valueOf(status)));
            RetryPolicy retryPolicy = new RetryPolicy();
            retryPolicy.setRetry(retry.getMaxRetriesOnNextServiceInstance());
            retryPolicy.setInterval(retry.getBackoff().getMinBackoff().toMillis());
            retryPolicy.setErrorCodes(statuses);
            return retryPolicy;
        }
        return null;
    }

    @Override
    public void onStart() {
        lifecycle(l -> l.onStart(lbRequest));
    }

    @Override
    public void onDiscard() {
        lifecycle(l -> l.onComplete(new CompletionContext<>(
                CompletionContext.Status.DISCARD, lbRequest, new EmptyResponse())));
    }

    @Override
    public void onStartRequest(ServiceEndpoint endpoint) {
        lifecycle(l -> l.onStartRequest(lbRequest,
                endpoint == null ? new DefaultResponse(null) : getResponse(endpoint)));
    }

    @Override
    public void onSuccess(SpringClusterResponse response, ServiceEndpoint endpoint) {
        HttpHeaders httpHeaders = response.getHttpHeaders();
        HttpStatusCode httpStatus = response.getHttpStatus();
        MultiValueMap<String, ResponseCookie> cookies = null;
        ResponseData responseData = new ResponseData(httpStatus, httpHeaders, cookies, requestData);

        CompletionContext<Object, ServiceInstance, ?> ctx = new CompletionContext<>(
                CompletionContext.Status.SUCCESS,
                lbRequest,
                getResponse(endpoint),
                responseData);
        lifecycle(l -> l.onComplete(ctx));
    }

    @Override
    public void onError(Throwable throwable, ServiceEndpoint endpoint) {
        lifecycle(l -> l.onComplete(new CompletionContext<>(CompletionContext.Status.FAILED, throwable, lbRequest, getResponse(endpoint))));
    }

    /**
     * Executes custom logic across the set of lifecycle processors associated with the load balancer,
     * allowing for enhanced control and monitoring of the load balancing process.
     *
     * @param consumer A consumer that accepts {@code LoadBalancerLifecycle} instances for processing.
     */
    protected void lifecycle(Consumer<RequestLifecycle> consumer) {
        RequestLifecycle lifecycle = serviceContext.getLifecycle();
        if (lifecycle != null && consumer != null) {
            consumer.accept(lifecycle);
        }
    }

    /**
     * Creates a new {@code RequestData} object representing the data of the original request.
     * This abstract method must be implemented by subclasses to provide specific request data
     * for the load balancing process.
     *
     * @return a new {@code RequestData} object
     */
    protected abstract RequestData buildRequestData();

    /**
     * Creates a new load balancer request object that encapsulates the original request data along with
     * any hints that may influence load balancing decisions. This object is used by the load balancer to
     * select an appropriate service instance based on the provided hints and other criteria.
     *
     * @return A DefaultRequest object containing the context for the load balancing operation.
     */
    protected DefaultRequest<RequestDataContext> buildLbRequest(String service, RequestData requestData) {
        LoadBalancerProperties properties = serviceContext.getLoadBalancerProperties();
        Map<String, String> hints = properties == null ? null : properties.getHint();
        String defaultHint = hints == null ? null : hints.getOrDefault("default", "default");
        String hint = hints == null ? null : hints.getOrDefault(service, defaultHint);
        return new DefaultRequest<>(new RequestDataContext(requestData, hint));
    }

    /**
     * Extracts the identifier from a sticky session cookie.
     *
     * @return The value of the sticky session cookie if present; otherwise, {@code null}.
     * This value is used to identify the server instance that should handle requests
     * from this client to ensure session persistence.
     */
    protected String buildStickyId() {
        LoadBalancerProperties properties = serviceContext.getLoadBalancerProperties();
        if (properties != null) {
            String instanceIdCookieName = properties.getStickySession().getInstanceIdCookieName();
            Object context = lbRequest.getContext();
            if (context instanceof RequestDataContext) {
                MultiValueMap<String, String> cookies = ((RequestDataContext) context).getClientRequest().getCookies();
                return cookies == null ? null : cookies.getFirst(instanceIdCookieName);
            }
        }
        return null;
    }
}
