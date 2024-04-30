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
package com.jd.live.agent.governance.policy.service.live;

import java.util.List;

/**
 * Defines the strategy for live operations in a distributed system or application.
 */
public interface LiveStrategy {

    /**
     * Determines if write operations are protected. Write protection can be used to prevent
     * unintended modifications to data, ensuring data integrity across distributed systems.
     *
     * @return {@code Boolean} indicating whether write operations are protected.
     */
    Boolean getWriteProtect();

    /**
     * Retrieves the policy for handling units within the system. The unit policy defines how
     * computational tasks, data shards, or service instances are selected or prioritized for operations.
     *
     * @return {@link UnitPolicy} the policy for unit handling.
     */
    UnitPolicy getUnitPolicy();

    /**
     * Provides a list of remote configurations for units. These configurations define the conditions for unit fault tolerance.
     *
     * @return a list of {@link RemoteCnd} objects representing remote configurations for units.
     */
    List<RemoteCnd> getUnitRemotes();

    /**
     * Retrieves the policy for managing cells within the system. Cells can represent logical or
     * physical partitions of the system, and the cell policy dictates how these are handled.
     *
     * @return {@link CellPolicy} the policy for cell management.
     */
    CellPolicy getCellPolicy();

    /**
     * Provides a list of remote configurations for cells. These configurations define the conditions for cell fault tolerance.
     *
     * @return a list of {@link RemoteCnd} objects representing remote configurations for cells.
     */
    List<RemoteCnd> getCellRemotes();
}

