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
package com.jd.live.agent.governance.policy.live.db;

/**
 * Manages access to live (real-time) database instances in a multi-active architecture.
 * Provides methods to retrieve databases by address, shard, or read/write requirements.
 */
public interface LiveDatabaseSupervisor {
    /**
     * Gets a database instance by its direct address.
     *
     * @param address The database connection address
     * @return The LiveDatabase instance, or null if not found
     */
    LiveDatabase getDatabase(String address);

    /**
     * Gets the first available database from a list of shard addresses.
     *
     * @param shards Array of potential shard addresses
     * @return The first available LiveDatabase, or null if none found
     */
    LiveDatabase getDatabase(String[] shards);

    /**
     * Gets a writable database instance from available shards.
     *
     * @param shards Preferred shard addresses (ordered by priority)
     * @return The writable LiveDatabase instance, or null if none available
     */
    LiveDatabase getWriteDatabase(String... shards);

    /**
     * Gets a readable database instance for a specific unit and cell.
     *
     * @param unit   The logical unit/partition identifier
     * @param cell   The deployment cell identifier
     * @param shards Preferred shard addresses (fallback order)
     * @return The read-only LiveDatabase instance, or null if none available
     */
    LiveDatabase getReadDatabase(String unit, String cell, String... shards);
}