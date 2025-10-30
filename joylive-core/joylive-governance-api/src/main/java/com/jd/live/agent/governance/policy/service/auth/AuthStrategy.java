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
package com.jd.live.agent.governance.policy.service.auth;

/**
 * Authentication strategy interface.
 */
public interface AuthStrategy {

    /**
     * Gets the HTTP Authorization header type (e.g. "Basic", "Bearer").
     *
     * @return the authorization type string, or null if not specified
     */
    String getAuthScheme();

    /**
     * Gets the authentication key, or returns the default key if not set.
     *
     * @param defaultKey the default key to return if no key is configured
     * @return the configured key or the default key
     */
    String getKeyOrDefault(String defaultKey);

    /**
     * Check if the authentication is effective at given time.
     *
     * @param time timestamp to check
     * @return true if effective, false otherwise
     */
    boolean isEffective(long time);
}
