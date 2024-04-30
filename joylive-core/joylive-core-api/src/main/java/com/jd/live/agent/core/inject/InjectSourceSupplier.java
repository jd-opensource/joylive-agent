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
package com.jd.live.agent.core.inject;

import com.jd.live.agent.core.extension.annotation.Extensible;

/**
 * Defines the contract for suppliers that provide {@link InjectSource} instances. These suppliers
 * are responsible for configuring or supplying sources of injection, allowing for dynamic and
 * flexible injection strategies. The interface is marked as extensible to encourage implementations
 * that can adapt or enhance the injection source provisioning process.
 * <p>
 * Suppliers can have an associated order, defined by {@code ORDER_POLICY_MANAGER}, which can be
 * used to prioritize or sequence the application of different injection sources.
 */
@Extensible("InjectSourceSupplier")
public interface InjectSourceSupplier {

    /**
     * The order value for policy managers, indicating their priority in the application of
     * injection sources. Lower values indicate higher priority.
     */
    int ORDER_POLICY_MANAGER = 1;

    /**
     * Applies the given {@link InjectSource} to configure or provide the necessary injection
     * sources. Implementations of this method are expected to modify, enrich, or use the provided
     * injection source according to their specific injection strategy.
     *
     * @param source The {@link InjectSource} to be applied.
     */
    void apply(InjectSource source);

}
