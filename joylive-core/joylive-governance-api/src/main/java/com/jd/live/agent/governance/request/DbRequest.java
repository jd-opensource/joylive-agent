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
package com.jd.live.agent.governance.request;

import java.util.regex.Pattern;

/**
 * Defines an interface for database requests, extending the basic {@link Request} interface.
 * <p>
 * This interface encapsulates the common properties and behaviors of database requests, such as host, port, database name,
 * and whether the request is a write operation. It also provides a base for more specialized database request types.
 * </p>
 */
public interface DbRequest extends Request {

    /**
     * Key for specifying the cluster name in the request.
     */
    String KEY_CLUSTER = "cluster";

    /**
     * Retrieves the name of the database request.
     *
     * @return The name of the request, or {@code null} if not applicable.
     */
    default String getName() {
        return null;
    }

    /**
     * Retrieves the host address of the database server.
     *
     * @return The host address as a String.
     */
    String getHost();

    /**
     * Retrieves the port number of the database server.
     *
     * @return The port number.
     */
    int getPort();

    /**
     * Retrieves the name of the database.
     *
     * @return The database name as a String.
     */
    String getDatabase();

    /**
     * Determines if the request is a write operation to the database.
     *
     * @return {@code true} if the request is a write operation; {@code false} otherwise.
     */
    boolean isWrite();

    /**
     * Defines an interface for cache-related database requests.
     * <p>
     * This interface represents database requests specifically targeted at interacting with cache systems.
     * </p>
     */
    interface CacheRequest extends DbRequest {

    }

    /**
     * Defines an interface for SQL database requests.
     * <p>
     * This interface extends {@link DbRequest} with functionality for handling SQL queries, including a method to
     * determine if the SQL statement represents a write operation based on a defined pattern.
     * </p>
     */
    interface SQLRequest extends DbRequest {

        /**
         * Pattern to identify SQL write operations.
         */
        Pattern WRITE_PATTERN = Pattern.compile(
                "INSERT\\b|UPDATE\\b|DELETE\\b|CREATE\\b|ALTER\\b|DROP\\b|TRUNCATE\\b", Pattern.CASE_INSENSITIVE);

        /**
         * Retrieves the SQL statement of the request.
         *
         * @return The SQL statement as a String.
         */
        String getSql();

        /**
         * Determines if the SQL request represents a write operation by matching the SQL statement against a predefined pattern.
         *
         * @return {@code true} if the SQL statement is a write operation; {@code false} otherwise.
         */
        @Override
        default boolean isWrite() {
            String sql = getSql();
            return sql != null && WRITE_PATTERN.matcher(sql).find();
        }
    }
}

