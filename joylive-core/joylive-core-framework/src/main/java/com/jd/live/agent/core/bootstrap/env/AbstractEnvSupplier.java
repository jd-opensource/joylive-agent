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
package com.jd.live.agent.core.bootstrap.env;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.bootstrap.EnvSupplier;
import com.jd.live.agent.core.bootstrap.env.config.ConfigEnvSupplier;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.parser.ConfigParser;
import com.jd.live.agent.core.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * AbstractEnvSupplier is an abstract class that provides a mechanism to load environment config
 * from various resources using different parsers.
 */
public abstract class AbstractEnvSupplier implements EnvSupplier {

    private static final Logger logger = LoggerFactory.getLogger(ConfigEnvSupplier.class);

    private final String[] resources;

    private final static String[] PREFIX = new String[]{"", "BOOT-INF/classes/", "WEB-INF/classes/"};

    @Inject
    private Map<String, ConfigParser> parsers;

    /**
     * Constructs an AbstractEnvSupplier with the specified resources.
     *
     * @param resources the resource paths to load configuration from.
     */
    public AbstractEnvSupplier(String... resources) {
        this.resources = resources;
    }

    /**
     * Retrieves the configuration by iterating through the resources and prefixes.
     *
     * @return a map containing the configuration, or null if no configuration is found.
     */
    protected Map<String, Object> loadConfigs() {
        if (resources != null) {
            for (String resource : resources) {
                for (String prefix : PREFIX) {
                    Map<String, Object> result = loadConfigs(resource, prefix);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Retrieves the configuration for a specific resource and prefix.
     *
     * @param resource the resource path.
     * @param prefix   the prefix to be added to the resource path.
     * @return a map containing the configuration, or null if no configuration is found.
     */
    protected Map<String, Object> loadConfigs(String resource, String prefix) {
        resource = prefix != null && !prefix.isEmpty() && !resource.startsWith(prefix)
                ? StringUtils.url(prefix, resource)
                : resource;
        int pos = resource.lastIndexOf('.');
        String ext = pos > 0 ? resource.substring(pos + 1) : "";
        ConfigParser parser = parsers.get(ext);
        if (parser != null) {
            URL url = ClassLoader.getSystemClassLoader().getResource(resource);
            if (url != null) {
                try {
                    Map<String, Object> result = parse(url.openStream(), parser);
                    logger.info("[LiveAgent] Successfully load config from " + url);
                    return result;
                } catch (Throwable e) {
                    logger.warn("[LiveAgent] Failed to load config from " + url, e);
                    return new HashMap<>();
                }
            }
        }
        return null;
    }

    /**
     * Parses the input stream using the specified parser.
     *
     * @param inputStream the input stream to parse.
     * @param parser      the parser to use for parsing the input stream.
     * @return a map containing the parsed configuration.
     * @throws Exception if an error occurs during parsing.
     */
    protected Map<String, Object> parse(InputStream inputStream, ConfigParser parser) throws Exception {
        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return parser.parse(reader);
        }
    }

}
