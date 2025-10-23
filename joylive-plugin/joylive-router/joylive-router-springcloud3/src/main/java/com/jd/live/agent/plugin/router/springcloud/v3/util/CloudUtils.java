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
package com.jd.live.agent.plugin.router.springcloud.v3.util;

import com.jd.live.agent.core.util.type.ClassUtils;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.RetryLoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.reactive.DeferringLoadBalancerExchangeFilterFunction;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.cloud.client.loadbalancer.reactive.RetryableLoadBalancerExchangeFilterFunction;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.support.HttpAccessor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static com.jd.live.agent.core.util.type.ClassUtils.getDeclaredMethod;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;
import static com.jd.live.agent.governance.annotation.ConditionalOnSpringCloudEnabled.TYPE_LOAD_BALANCED;

/**
 * Utility class for detecting Spring Cloud environment and load balancer configuration.
 */
public class CloudUtils {

    // spring cloud
    private static final Class<?> CLASS_LOAD_BALANCED = loadClass(TYPE_LOAD_BALANCED, HttpAccessor.class.getClassLoader());

    private static final String TYPE_LOAD_BALANCER_PROPERTIES = "org.springframework.cloud.client.loadbalancer.LoadBalancerProperties";

    private static final Class<?> CLASS_LOAD_BALANCER_PROPERTIES = loadClass(TYPE_LOAD_BALANCER_PROPERTIES, HttpAccessor.class.getClassLoader());

    private static final Field FIELD_RAW_STATUS = ClassUtils.getDeclaredField(CLASS_LOAD_BALANCER_PROPERTIES, "useRawStatusCodeInResponseData");

    private static final String TYPE_REACTIVE_LOAD_BALANCER_FACTORY = "org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer$Factory";

    private static final Class<?> CLASS_REACTIVE_LOAD_BALANCER_FACTORY = loadClass(TYPE_REACTIVE_LOAD_BALANCER_FACTORY, HttpAccessor.class.getClassLoader());

    private static final Method METHOD_GET_PROPERTIES = getDeclaredMethod(CLASS_REACTIVE_LOAD_BALANCER_FACTORY, "getProperties", new Class[]{String.class});

    /**
     * Checks if Spring Cloud is available in the classpath.
     *
     * @return true if Spring Cloud is present, false otherwise
     */
    public static boolean isCloudEnabled() {
        return CLASS_LOAD_BALANCED != null;
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
                    if (filter instanceof LoadBalancedExchangeFilterFunction
                            || filter instanceof DeferringLoadBalancerExchangeFilterFunction
                            || filter instanceof RetryableLoadBalancerExchangeFilterFunction) {
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
     * Creates writable copy of HTTP headers.
     *
     * @param headers source headers
     * @return writable headers instance
     */
    public static HttpHeaders writable(HttpHeaders headers) {
        return HttpHeaders.writableHttpHeaders(headers);
    }

    /**
     * Checks if raw status code is enabled.
     *
     * @return true if raw status code is available, false otherwise
     */
    public static boolean isRawStatusCodeEnabled() {
        return FIELD_RAW_STATUS != null;
    }

    /**
     * Checks if service loadbalancer properties is enabled.
     *
     * @return true if service properties is available, false otherwise
     */
    public static boolean isServicePropertiesEnabled() {
        return METHOD_GET_PROPERTIES != null;
    }
}
