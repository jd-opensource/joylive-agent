package com.jd.live.agent.plugin.router.springcloud.v2.request;

import com.jd.live.agent.core.util.cache.UnsafeLazyObject;
import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.core.util.http.HttpUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFunction;

/**
 * @author: yuanjinzhong
 * @date: 2025/1/3 17:59
 * @description:
 */
public class ReactiveClusterRequest extends AbstractClusterRequest<ClientRequest> {

    private final ExchangeFunction next;

    /**
     * Constructs a new ClientOutboundRequest with the specified parameters.
     *
     * @param request             The original client request to be processed.
     * @param loadBalancerFactory A factory for creating instances of ReactiveLoadBalancer for service instances.
     * @param next                The next processing step in the request handling pipeline.
     */
    public ReactiveClusterRequest(ClientRequest request,
                                  ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory,
                                  ExchangeFunction next) {
        super(request, loadBalancerFactory);
        this.uri = request.url();
        this.queries = new UnsafeLazyObject<>(() -> HttpUtils.parseQuery(request.url().getRawQuery()));
        this.headers = new UnsafeLazyObject<>(() -> HttpHeaders.writableHttpHeaders(request.headers()));
        this.cookies = new UnsafeLazyObject<>(request::cookies);
        this.next = next;
    }

    @Override
    public HttpMethod getHttpMethod() {
        try {
            return HttpMethod.valueOf(request.method().name());
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }

    @Override
    public String getCookie(String key) {
        return key == null || key.isEmpty() ? null : request.cookies().getFirst(key);
    }

    @Override
    public String getHeader(String key) {
        return key == null || key.isEmpty() ? null : request.headers().getFirst(key);
    }

    @Override
    public void setHeader(String key, String value) {
        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            HttpHeaders.writableHttpHeaders(request.headers()).set(key, value);
        }
    }

    public ExchangeFunction getNext() {
        return next;
    }


}
