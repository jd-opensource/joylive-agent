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
package com.jd.live.agent.plugin.router.springcloud.v1.util;

import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.governance.invoke.cluster.LiveCluster;
import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v1.request.FeignCloudOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v1.request.RibbonOutboundRequest;
import feign.Request;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.RetryLoadBalancerInterceptor;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.support.HttpAccessor;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;
import static com.jd.live.agent.plugin.router.springcloud.v1.condition.ConditionalOnSpringCloud1Enabled.TYPE_DISCOVERY_LIFECYCLE;

/**
 * Utility class for detecting Spring Cloud environment and load balancer configuration.
 */
public class CloudUtils {

    // spring cloud 1
    private static final Class<?> lbType = loadClass(TYPE_DISCOVERY_LIFECYCLE, HttpAccessor.class.getClassLoader());

    private static final String TYPE_RIBBON_REQUEST = "org.springframework.cloud.netflix.feign.ribbon.FeignLoadBalancer$RibbonRequest";

    private static final Class<?> ribbonRequestType = loadClass(TYPE_RIBBON_REQUEST, HttpAccessor.class.getClassLoader());

    private static final FieldAccessor ribbonRequestAccessor = getAccessor(ribbonRequestType, "request");

    private static final Map<Object, LiveCluster> clusters = new ConcurrentHashMap<>();

    /**
     * Checks if Spring Cloud is available in the classpath.
     *
     * @return true if Spring Cloud is present, false otherwise
     */
    public static boolean isCloudEnabled() {
        return lbType != null;
    }

    /**
     * Determines if the RestTemplate is configured as a load-balanced client.
     *
     * @param client the RestTemplate to check
     * @return true if configured with load balancer interceptors, false otherwise
     */
    public static boolean isBlockingCloudClient(Object client) {
        // Parameter client cannot be declared as RestTemplate, as it will cause class loading exceptions.
        if (client instanceof RestTemplate) {
            RestTemplate template = (RestTemplate) client;
            for (ClientHttpRequestInterceptor interceptor : template.getInterceptors()) {
                if (interceptor instanceof RetryLoadBalancerInterceptor) {
                    return true;
                } else if (interceptor instanceof LoadBalancerInterceptor) {
                    return true;
                }
            }
        }
        return false;
    }

    public static <K, V extends LiveCluster> V getOrCreateCluster(K client, Function<K, V> function) {
        return (V) clusters.computeIfAbsent(client, o -> function.apply(client));
    }

    public static HttpOutboundRequest build(Object request, String service) {
        if (request instanceof org.springframework.http.HttpRequest) {
            return new RibbonOutboundRequest((org.springframework.http.HttpRequest) request, service);
        } else if (ribbonRequestType != null && ribbonRequestType.isInstance(request)) {
            Request feignRequest = (Request) ribbonRequestAccessor.get(request);
            return new FeignCloudOutboundRequest(feignRequest, service);
        }
        return null;
    }
}
