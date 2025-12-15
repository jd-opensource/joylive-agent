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
package com.jd.live.agent.governance.subscription.config.gray;

import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.parser.TypeReference;
import lombok.Getter;

import java.io.StringReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Watches for configuration changes and automatically switches between release and beta configurations based on policy matching.
 */
public abstract class ConfigFetcher<C> {

    /**
     * Config client.
     */
    @Getter
    protected final C client;

    /**
     * Release configuration key.
     */
    @Getter
    protected final ConfigKey keyRelease;

    /**
     * Policy configuration key.
     */
    protected final ConfigKey keyPolicy;

    /**
     * Application instance for policy matching.
     */
    protected final Application application;

    /**
     * JSON parser for configuration data.
     */
    protected final ObjectParser json;

    /**
     * Current configuration value.
     */
    protected final AtomicReference<String> valueRef = new AtomicReference<>();

    public ConfigFetcher(C client, ConfigKey key, Application application, ObjectParser json) {
        this.client = client;
        this.keyRelease = key;
        this.keyPolicy = key.getPolicyKey();
        this.application = application;
        this.json = json;
    }

    /**
     * Retrieves configuration value with a specified timeout, implementing gray policy-based configuration switching.
     *
     * @param timeout maximum time in milliseconds to wait for configuration retrieval operations
     * @return the configuration value as a string, either from cache or freshly retrieved
     * @throws Exception if configuration retrieval fails for any reason (network issues, parsing errors, etc.)
     */
    public String getConfig(long timeout) throws Exception {
        String value = valueRef.get();
        if (value == null) {
            ConfigPolicy policy = parsePolicy(doGetConfig(keyPolicy, timeout));
            ConfigKey key = policy == null ? keyRelease : new ConfigKey(policy.getName(), keyRelease.getGroup());
            value = doGetConfig(key, timeout);
        }
        return value;
    }

    /**
     * Abstract method to retrieve configuration value for a specific configuration key within a timeout.
     *
     * @param key     the configuration key containing dataId and group information
     * @param timeout maximum time in milliseconds to wait for the configuration retrieval
     * @return the configuration value as a string
     * @throws Exception if configuration retrieval fails (network errors, timeout, service unavailable, etc.)
     */
    protected abstract String doGetConfig(ConfigKey key, long timeout) throws Exception;

    /**
     * Parses configuration policy JSON and finds the first matching policy for the current application.
     *
     * @param value JSON string containing list of configuration policies
     * @return matching ConfigPolicy for the application, or null if no match found
     */
    protected ConfigPolicy parsePolicy(String value) {
        List<ConfigPolicy> policies = value == null || value.isEmpty() ? null : json.read(new StringReader(value), new TypeReference<List<ConfigPolicy>>() {
        });
        ConfigPolicy policy = null;
        if (policies != null && !policies.isEmpty()) {
            for (ConfigPolicy p : policies) {
                if (p.match(application)) {
                    policy = p;
                    break;
                }
            }
        }
        return policy;
    }
}
