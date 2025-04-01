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

import com.jd.live.agent.core.bootstrap.EnvSupplier;
import com.jd.live.agent.core.bootstrap.env.AbstractEnvSupplier;
import com.jd.live.agent.core.bootstrap.resource.BootResource;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.util.type.ValuePath;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.bootstrap.resource.BootResource.SCHEMA_CLASSPATH;
import static com.jd.live.agent.core.bootstrap.resource.BootResource.SCHEMA_FILE;
import static com.jd.live.agent.core.util.StringUtils.CHAR_COMMA;
import static com.jd.live.agent.core.util.StringUtils.split;
import static com.jd.live.agent.core.util.template.Template.evaluate;

@Injectable
@Extension(value = "SpringEnvSupplier",order = EnvSupplier.ORDER_SPRING_ENV_SUPPLIER)
public class SpringEnvSupplier extends AbstractEnvSupplier {

    private static final String KEY_SPRING_APPLICATION_NAME = "spring.application.name";

    private static final String KEY_SPRING_SERVER_PORT = "server.port";

    private static final String KEY_SPRING_CONFIG_LOCATION = "spring.config.location";

    private static final String RESOURCE_APPLICATION_PROPERTIES = "application.properties";

    private static final String RESOURCE_APPLICATION_YAML = "application.yaml";

    private static final String RESOURCE_APPLICATION_YML = "application.yml";

    @Override
    public void process(Map<String, Object> env) {
        BootResource[] resources = getResources(env);
        Map<String, Object> configs = loadConfigs(resources);
        Object value = getConfigAndResolve(configs, env, KEY_SPRING_APPLICATION_NAME);
        String name = value == null ? null : value.toString();
        if (name != null && !name.isEmpty()) {
            env.put(Application.KEY_APPLICATION_NAME, name);
        }
        value = getConfigAndResolve(configs, env, KEY_SPRING_SERVER_PORT);
        String port = value == null ? null : value.toString();
        if (port != null && !port.isEmpty()) {
            try {
                Integer.parseInt(port);
                env.put(Application.KEY_APPLICATION_SERVICE_PORT, port);
            } catch (NumberFormatException ignored) {
                env.remove(Application.KEY_APPLICATION_SERVICE_PORT);
            }
        }
    }

    /**
     * Retrieves an array of {@link BootResource} objects based on the provided environment configuration.
     * If the environment contains a specific configuration location (e.g., `spring.config.location`),
     * it uses that location to create the resources. Otherwise, it defaults to standard resource paths
     * for properties and YAML files.
     *
     * @param env A {@link Map} containing environment configurations, typically system properties or environment variables.
     * @return An array of {@link BootResource} objects representing the resources to load.
     */
    private BootResource[] getResources(Map<String, Object> env) {
        String config = (String) env.get(KEY_SPRING_CONFIG_LOCATION);
        if (config != null && !config.isEmpty()) {
            // location
            return getBootResources(config);
        } else {
            // default
            return new BootResource[]{
                    new BootResource(null, null, RESOURCE_APPLICATION_PROPERTIES),
                    new BootResource(null, null, RESOURCE_APPLICATION_YAML),
                    new BootResource(null, null, RESOURCE_APPLICATION_YML)
            };
        }
    }

    /**
     * Parses a configuration location string and generates an array of {@link BootResource} objects.
     * The location string can contain multiple resource paths separated by commas. Each path is parsed
     * to extract its schema (e.g., `file` or `classpath`), path, and resource name. If the resource name
     * is empty, it defaults to standard resource names for properties and YAML files.
     *
     * @param location A string representing the configuration location(s), typically from `spring.config.location`.
     * @return An array of {@link BootResource} objects representing the parsed resources.
     */
    private BootResource[] getBootResources(String location) {
        // load from spring.config.location
        String[] resources = split(location, CHAR_COMMA);
        List<BootResource> result = new ArrayList<>(4);
        for (String resource : resources) {
            String schema = null;
            String path = null;
            int pos = resource.indexOf(':');
            if (pos >= 0) {
                schema = resource.substring(0, pos);
                if (SCHEMA_FILE.equals(schema) || SCHEMA_CLASSPATH.equals(schema)) {
                    resource = resource.substring(pos + 1);
                } else {
                    // windows c:\a\b\c.txt
                    schema = null;
                }
            }
            pos = resource.lastIndexOf(!SCHEMA_CLASSPATH.equals(schema) ? File.separatorChar : '/');
            if (pos >= 0) {
                path = resource.substring(0, pos);
                resource = resource.substring(pos + 1);
            }
            if (resource.isEmpty()) {
                result.add(new BootResource(schema, path, RESOURCE_APPLICATION_PROPERTIES));
                result.add(new BootResource(schema, path, RESOURCE_APPLICATION_YAML));
                result.add(new BootResource(schema, path, RESOURCE_APPLICATION_YML));
            } else {
                result.add(new BootResource(schema, path, resource));
            }
        }
        return result.toArray(new BootResource[0]);
    }

    /**
     * Retrieves a configuration value from the provided configuration map and resolves any placeholders using the environment map.
     *
     * @param configs a map containing configuration key-value pairs
     * @param env     a map containing environment variables or other context-specific values
     * @param key     the key for the configuration value to retrieve and resolve
     * @return the resolved configuration value as a String
     */
    private Object getConfigAndResolve(Map<String, Object> configs, Map<String, Object> env, String key) {
        Object config = env.get(key);
        config = config == null || (config instanceof String && ((String) config).isEmpty()) ? getConfig(configs, key) : config;
        return config == null || (config instanceof String && ((String) config).isEmpty()) ? config : evaluate(config.toString(), env, false);
    }

    /**
     * Retrieves a configuration value from the provided configuration map based on the specified key.
     * If the key does not directly match a value in the map, it attempts to resolve the value using a {@link ValuePath}.
     *
     * @param configs a map containing configuration key-value pairs
     * @param key     the key for the configuration value to retrieve
     * @return the configuration value as a String, or null if not found
     */
    private Object getConfig(Map<String, Object> configs, String key) {
        Object result = configs == null ? null : configs.get(key);
        if (result == null) {
            result = ValuePath.get(configs, key);
        }
        return result;
    }
}
