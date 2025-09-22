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
package com.jd.live.agent.plugin.router.springcloud.v2_1.util;

import com.jd.live.agent.governance.invoke.cluster.LiveCluster;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.RetryLoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerExchangeFilterFunction;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.support.HttpAccessor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;
import static com.jd.live.agent.governance.annotation.ConditionalOnSpringCloudEnabled.TYPE_LOAD_BALANCED;

/**
 * Utility class for detecting Spring Cloud environment and load balancer configuration.
 */
public class CloudUtils {

    // spring cloud
    private static final Class<?> lbType = loadClass(TYPE_LOAD_BALANCED, HttpAccessor.class.getClassLoader());

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

    /**
     * Checks if the WebClient builder is configured for cloud load balancing.
     *
     * @param client the WebClient builder to check
     * @return true if the builder contains load balancing filters, false otherwise
     */
    public static boolean isReactiveCloudClient(Object client) {
        if (client instanceof WebClient.Builder) {
            WebClient.Builder builder = (WebClient.Builder) client;
            final boolean[] result = new boolean[]{false};
            builder.filters(filters -> {
                for (ExchangeFilterFunction filter : filters) {
                    if (filter instanceof LoadBalancerExchangeFilterFunction) {
                        result[0] = true;
                        break;
                    }
                }
            });
            return result[0];
        }
        return false;
    }

    /**
     * Gets existing cluster or creates new one for the client.
     *
     * @param <K>      client type
     * @param <V>      cluster type
     * @param client   the client key
     * @param function factory function to create cluster
     * @return existing or newly created cluster
     */
    public static <K, V extends LiveCluster> V getOrCreateCluster(K client, Function<K, V> function) {
        return (V) clusters.computeIfAbsent(client, o -> function.apply(client));
    }

    /**
     * Creates writable copy of HTTP headers.
     *
     * @param headers source headers
     * @return writable headers instance
     */
    public static HttpHeaders writable(HttpHeaders headers) {
        return HttpHeaders.writableHttpHeaders(headers);
    }
}
