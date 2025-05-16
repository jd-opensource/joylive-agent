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

import com.jd.live.agent.governance.policy.AccessMode;

import java.util.regex.Pattern;

/**
 * Defines an interface for database requests, extending the basic {@link Request} interface.
 * <p>
 * This interface encapsulates the common properties and behaviors of database requests, such as host, port, database name,
 * and whether the request is a write operation. It also provides a base for more specialized database request types.
 * </p>
 */
public interface DbRequest extends Request {

    String SYSTEM_REQUEST = "systemRequest";

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

    default String getAddress() {
        String host = getHost();
        int port = getPort();
        return port > 0 ? host + ":" + port : host;
    }

    /**
     * Retrieves the name of the database.
     *
     * @return The database name as a String.
     */
    String getDatabase();

    /**
     * Get the access mode of the request.
     *
     * @return The access mode of the request.
     */
    default AccessMode getAccessMode() {
        Boolean systemRequest = getAttribute(SYSTEM_REQUEST);
        if (systemRequest != null && systemRequest) {
            return AccessMode.NONE;
        }
        return AccessMode.READ_WRITE;
    }

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
         * Pattern to identify SQL read operations.
         */
        Pattern READ_PATTERN = Pattern.compile("SELECT\\b", Pattern.CASE_INSENSITIVE);

        /**
         * Retrieves the SQL statement of the request.
         *
         * @return The SQL statement as a String.
         */
        String getSql();

        @Override
        default AccessMode getAccessMode() {
            Boolean systemRequest = getAttribute(SYSTEM_REQUEST);
            if (systemRequest != null && systemRequest) {
                return AccessMode.NONE;
            }
            String sql = getSql();
            if (sql == null) {
                return AccessMode.NONE;
            }
            boolean isWrite = WRITE_PATTERN.matcher(sql).find();
            boolean isRead = READ_PATTERN.matcher(sql).find();
            if (isWrite) {
                return AccessMode.READ_WRITE;
            } else if (isRead) {
                return AccessMode.READ;
            } else {
                return AccessMode.NONE;
            }
        }
    }
}

