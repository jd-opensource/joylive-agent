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
package com.jd.live.agent.plugin.router.springgateway.v2.config;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class GatewayConfig {

    public static final String ATTRIBUTE_RETRY_CONFIG = "retryConfig";

    public static final String CONFIG_SPRING_GATEWAY_PREFIX = "agent.governance.router.springgateway";

    public static final String KEY_UNIT = "unit";

    public static final String KEY_CELL = "cell";

    public static final String KEY_HOST = "host";

    public static final String KEY_HOST_EXPRESSION = "hostExpression";

    protected static final String DEFAULT_HOST_EXPRESSION = "${unit}-${host}";

    private String hostExpression;

    private Set<String> pathFilters;

}
