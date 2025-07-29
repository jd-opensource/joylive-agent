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
package com.jd.live.agent.governance.config;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration class for circuit breaker settings.
 */
@Getter
@Setter
public class CircuitBreakerConfig extends RecyclerConfig {

    /**
     * The type of the circuit breaker. Default is "Resilience4j".
     */
    private String type = "Resilience4j";

    private boolean autoHalfOpenEnabled;

    private boolean loggingEnabled = true;

}

