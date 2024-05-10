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
package com.jd.live.agent.governance.invoke.retry;

import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;

/**
 * Factory interface for creating {@link Retrier} instances based on specified {@link RetryPolicy} configurations.
 *
 * @since 1.0.0
 */
@Extensible("RetrierFactory")
public interface RetrierFactory {

    /**
     * Returns a {@link Retrier} instance configured with the specified {@link RetryPolicy}.
     *
     * @param policy The {@link RetryPolicy} to be used for configuring the returned {@code Retrier}.
     * @return A {@code Retrier} instance configured according to the specified {@code RetryPolicy}.
     */
    Retrier get(RetryPolicy policy);

}

