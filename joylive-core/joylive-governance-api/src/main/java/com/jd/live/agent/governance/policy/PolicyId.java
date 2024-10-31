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
package com.jd.live.agent.governance.policy;

import com.jd.live.agent.core.util.URI;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Supplier;

/**
 * The {@code PolicyId} class is an abstract implementation of the {@code IdGenerator} interface.
 * It represents a policy with an identifier that can be generated based on a URI. The class also
 * maintains a set of tags that can be used to supplement the policy with additional information.
 */
@Getter
public class PolicyId implements PolicyIdGen {
    /**
     * The key for the service group tag.
     */
    public static final String KEY_SERVICE_GROUP = "group";

    /**
     * The key for the service method tag.
     */
    public static final String KEY_SERVICE_METHOD = "method";

    /**
     * The key for the service variable tag.
     */
    public static final String KEY_SERVICE_VARIABLE = "variable";

    /**
     * The key for the service lane space ID tag.
     */
    public static final String KEY_SERVICE_LANE_SPACE_ID = "laneSpaceId";

    public static final String KEY_SERVICE_CONCURRENCY_LIMIT = "concurrencyLimit";

    public static final String KEY_SERVICE_RATE_LIMIT = "rateLimit";

    public static final String KEY_SERVICE_LOAD_LIMIT = "loadLimit";

    public static final String KEY_SERVICE_ROUTE = "route";

    public static final String KEY_SERVICE_CIRCUIT_BREAK = "circuitBreak";

    public static final String KEY_FAULT_INJECTION = "faultInjection";

    public static final String KEY_SERVICE_AUTH = "auth";

    public static final String KEY_SERVICE_ENDPOINT = "endpoint";

    /**
     * The default group name for the service.
     */
    public static final String DEFAULT_GROUP = "default";

    /**
     * The unique identifier for the policy, generated based on the URI.
     */
    @Setter
    protected Long id;

    /**
     * The URI associated with the policy.
     */
    protected transient URI uri;

    @Override
    public void supplement(Supplier<URI> uriSupplier) {
        if (uri == null && uriSupplier != null) {
            uri = uriSupplier.get();
        }
        if (id == null && uri != null) {
            id = uri.getId();
        }
    }
}

