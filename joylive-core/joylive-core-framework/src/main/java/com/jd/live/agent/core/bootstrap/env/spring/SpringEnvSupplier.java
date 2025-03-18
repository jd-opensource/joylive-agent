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
package com.jd.live.agent.core.bootstrap.env.spring;

import com.jd.live.agent.core.bootstrap.env.AbstractEnvSupplier;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.util.type.ValuePath;
import lombok.Getter;

import java.util.Map;
import java.util.function.Predicate;

@Injectable
@Extension("SpringEnvSupplier")
public class SpringEnvSupplier extends AbstractEnvSupplier {

    private static final String KEY_SPRING_APPLICATION_NAME = "spring.application.name";

    private static final String KEY_SPRING_SERVER_PORT = "server.port";

    private static final String RESOURCE_SPRINGBOOT_APPLICATION_PROPERTIES = "application.properties";

    private static final String RESOURCE_SPRINGBOOT_APPLICATION_YAML = "application.yaml";

    private static final String RESOURCE_SPRINGBOOT_APPLICATION_YML = "application.yml";

    private static final ValuePath APP_PATH = new ValuePath(KEY_SPRING_APPLICATION_NAME);

    private static final ValuePath PORT_PATH = new ValuePath(KEY_SPRING_SERVER_PORT);

    private static final Predicate<String> VARIABLE = v -> v.startsWith("${") && v.endsWith("}");

    public SpringEnvSupplier() {
        super(RESOURCE_SPRINGBOOT_APPLICATION_PROPERTIES,
                RESOURCE_SPRINGBOOT_APPLICATION_YAML,
                RESOURCE_SPRINGBOOT_APPLICATION_YML);
    }

    @Override
    public void process(Map<String, Object> env) {
        Map<String, Object> configs = loadConfigs();
        if (configs == null) {
            return;
        }
        String name = getConfigAndResolve(configs, env, KEY_SPRING_APPLICATION_NAME);
        if (name != null && !name.isEmpty()) {
            env.put(Application.KEY_APPLICATION_NAME, name);
        }

        String port = getConfigAndResolve(configs, env, KEY_SPRING_SERVER_PORT);
        if (port != null && !port.isEmpty()) {
            try {
                Integer.parseInt(port);
                env.put(Application.KEY_APPLICATION_SERVICE_PORT, name);
            } catch (NumberFormatException ignored) {
                env.remove(Application.KEY_APPLICATION_SERVICE_PORT);
            }
        }
    }

    /**
     * Retrieves a configuration value from the provided configuration map and resolves any placeholders using the environment map.
     * This method combines the functionality of {@link #getConfig(Map, String)} and {@link #resolve(Map, String)} to simplify
     * the process of fetching and resolving configuration values.
     *
     * @param configs a map containing configuration key-value pairs
     * @param env     a map containing environment variables or other context-specific values
     * @param key     the key for the configuration value to retrieve and resolve
     * @return the resolved configuration value as a String
     */
    private String getConfigAndResolve(Map<String, Object> configs, Map<String, Object> env, String key) {
        return resolve(env, getConfig(configs, key)).toString();
    }

    /**
     * Retrieves a configuration value from the provided configuration map based on the specified key.
     * If the key does not directly match a value in the map, it attempts to resolve the value using a {@link ValuePath}.
     *
     * @param configs a map containing configuration key-value pairs
     * @param key     the key for the configuration value to retrieve
     * @return the configuration value as a String, or null if not found
     */
    private String getConfig(Map<String, Object> configs, String key) {
        String name = (String) configs.get(key);
        if (name == null) {
            ValuePath path = new ValuePath(key);
            name = (String) path.get(configs);
        }
        return name;
    }

    /**
     * Resolves any placeholders in the provided expression using the environment map.
     * If the expression contains placeholders in the format "${...}", they are evaluated and replaced
     * using the environment map. If no placeholders are present, the original expression is returned.
     *
     * @param env        a map containing environment variables or other context-specific values
     * @param expression the expression to resolve, which may contain placeholders
     * @return the resolved value as a String
     */
    private ResolveResult resolve(Map<String, Object> env, String expression) {
        // TODO move to Template
        if (VARIABLE.test(expression)) {
            String key = expression.substring(2, expression.length() - 1);
            String defaultValue = null;

            int pos = key.indexOf(':');
            if (pos >= 0) {
                defaultValue = key.substring(pos + 1);
                key = key.substring(0, pos);
            }
            String value = key.isEmpty() ? null : (String) env.get(key);
            if ((value == null || value.isEmpty()) && defaultValue != null && !defaultValue.isEmpty()) {
                ResolveResult result = resolve(env, defaultValue);
                if (result.isResolved()) {
                    return result;
                }
            }
            if (value != null) {
                return new ResolveResult(value, true);
            }
            return new ResolveResult(expression, false);
        }
        return new ResolveResult(expression, true);
    }

    @Getter
    static class ResolveResult {

        private final String value;

        private final boolean resolved;

        ResolveResult(String value, boolean resolved) {
            this.value = value;
            this.resolved = resolved;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
