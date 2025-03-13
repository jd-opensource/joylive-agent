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
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v4.cluster.context.CloudClusterContext;
import com.jd.live.agent.plugin.router.springcloud.v4.instance.SpringEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v4.response.SpringClusterResponse;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.*;
import java.util.function.Consumer;

/**
 * Represents an outbound HTTP request in a reactive microservices architecture,
 * extending the capabilities of an abstract HTTP outbound request model to include
 * client-specific functionalities. This class encapsulates features such as load balancing,
 * service instance discovery, and lifecycle management, making it suitable for handling
 * dynamic client requests in a distributed system.
 */
public abstract class AbstractClusterRequest<T, C extends CloudClusterContext> extends AbstractHttpOutboundRequest<T> implements SpringClusterRequest {

    protected final C context;

    /**
     * A {@code LoadBalancerProperties} object, containing configuration
     * properties for load balancing.
     */
    protected final LoadBalancerProperties properties;

    /**
     * A lazy-initialized object of {@code Set<LoadBalancerLifecycle>}, representing the lifecycle
     * processors for the load balancer. These processors provide hooks for custom logic at various
     * stages of the load balancing process.
     */
    @SuppressWarnings("rawtypes")
    protected CacheObject<Set<LoadBalancerLifecycle>> lifecycles;

    /**
     * A lazy-initialized {@code Request<?>} object that encapsulates the original request data
     * along with any hints to influence load balancing decisions.
     */
    protected CacheObject<Request<?>> lbRequest;

    /**
     * A lazy-initialized {@code RequestData} object, representing the data of the original
     * request that will be used by the load balancer to select an appropriate service instance.
     */
    protected CacheObject<RequestData> requestData;

    /**
     * A lazy-initialized {@code ServiceInstanceListSupplier} object, responsible for providing
     * a list of available service instances for load balancing.
     */
    protected CacheObject<ServiceInstanceListSupplier> instanceSupplier;

    protected CacheObject<String> stickyId;

    public AbstractClusterRequest(T request, URI uri, C context) {
        super(request);
        this.uri = uri;
        this.context = context;
        // depend on url
        this.properties = context.getLoadBalancerProperties(getService());
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
        LoadBalancerProperties properties = getProperties();
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
    public Mono<List<ServiceInstance>> getInstances() {
        ServiceInstanceListSupplier supplier = getInstanceSupplier();
        if (supplier == null) {
            return Mono.just(new ArrayList<>());
        } else {
            return supplier.get(getLbRequest()).next();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onStart() {
        lifecycles(l -> l.onStart(getLbRequest()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onDiscard() {
        lifecycles(l -> l.onComplete(new CompletionContext<>(
                CompletionContext.Status.DISCARD, getLbRequest(), new EmptyResponse())));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onStartRequest(SpringEndpoint endpoint) {
        lifecycles(l -> l.onStartRequest(getLbRequest(),
                endpoint == null ? new DefaultResponse(null) : endpoint.getResponse()));
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void onSuccess(SpringClusterResponse response, SpringEndpoint endpoint) {
        HttpHeaders httpHeaders = response.getHttpHeaders();
        HttpStatusCode httpStatus = response.getHttpStatus();
        RequestData requestData = getRequestData();
        MultiValueMap<String, ResponseCookie> cookies = null;
        ResponseData responseData = new ResponseData(httpStatus, httpHeaders, cookies, requestData);

        CompletionContext<Object, ServiceInstance, ?> ctx = new CompletionContext<>(
                CompletionContext.Status.SUCCESS,
                getLbRequest(),
                endpoint.getResponse(),
                responseData);
        lifecycles(l -> l.onComplete(ctx));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onError(Throwable throwable, SpringEndpoint endpoint) {
        Response<ServiceInstance> response = endpoint == null ? new DefaultResponse(null) : endpoint.getResponse();
        lifecycles(l -> l.onComplete(new CompletionContext<>(
                CompletionContext.Status.FAILED,
                throwable,
                getLbRequest(),
                response)));
    }

    /**
     * Executes custom logic across the set of lifecycle processors associated with the load balancer,
     * allowing for enhanced control and monitoring of the load balancing process.
     *
     * @param consumer A consumer that accepts {@code LoadBalancerLifecycle} instances for processing.
     */
    @SuppressWarnings("rawtypes")
    protected void lifecycles(Consumer<LoadBalancerLifecycle> consumer) {
        Set<LoadBalancerLifecycle> lifecycles = getLifecycles();
        if (lifecycles != null && consumer != null) {
            lifecycles.forEach(consumer);
        }
    }

    /**
     * Retrieves lifecycle processors for load balancing
     *
     * @return Set of lifecycle processors
     */
    @SuppressWarnings("rawtypes")
    protected Set<LoadBalancerLifecycle> getLifecycles() {
        if (lifecycles == null) {
            lifecycles = new CacheObject<>(context.getLifecycleProcessors(getService()));
        }
        return lifecycles.get();
    }

    /**
     * Gets the load balancer request
     *
     * @return The load balancer request
     */
    protected Request<?> getLbRequest() {
        if (lbRequest == null) {
            lbRequest = new CacheObject<>(buildLbRequest());
        }
        return lbRequest.get();
    }

    /**
     * Retrieves load balancing properties
     *
     * @return Load balancing properties
     */
    protected LoadBalancerProperties getProperties() {
        return properties;
    }

    /**
     * Returns a supplier of service instance lists.
     *
     * @return a supplier of service instance lists
     */
    protected ServiceInstanceListSupplier getInstanceSupplier() {
        if (instanceSupplier == null) {
            instanceSupplier = new CacheObject<>(context.getServiceInstanceListSupplier(getService()));
        }
        return instanceSupplier.get();
    }

    /**
     * Retrieves request data for load balancing
     *
     * @return Request data
     */
    protected RequestData getRequestData() {
        if (requestData == null) {
            requestData = new CacheObject<>(buildRequestData());
        }
        return requestData.get();
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
    protected DefaultRequest<RequestDataContext> buildLbRequest() {
        LoadBalancerProperties properties = getProperties();
        Map<String, String> hints = properties == null ? null : properties.getHint();
        String defaultHint = hints == null ? null : hints.getOrDefault("default", "default");
        String hint = hints == null ? null : hints.getOrDefault(getService(), defaultHint);
        return new DefaultRequest<>(new RequestDataContext(getRequestData(), hint));
    }

    /**
     * Extracts the identifier from a sticky session cookie.
     *
     * @return The value of the sticky session cookie if present; otherwise, {@code null}.
     * This value is used to identify the server instance that should handle requests
     * from this client to ensure session persistence.
     */
    protected String buildStickyId() {
        LoadBalancerProperties properties = getProperties();
        if (properties != null) {
            String instanceIdCookieName = properties.getStickySession().getInstanceIdCookieName();
            Object context = getLbRequest().getContext();
            if (context instanceof RequestDataContext) {
                MultiValueMap<String, String> cookies = ((RequestDataContext) context).getClientRequest().getCookies();
                return cookies == null ? null : cookies.getFirst(instanceIdCookieName);
            }
        }
        return null;
    }
}
