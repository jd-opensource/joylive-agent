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
import com.jd.live.agent.core.bootstrap.resource.BootResource;
import com.jd.live.agent.core.bootstrap.resource.BootResourceLoader;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.parser.ConfigParser;
import com.jd.live.agent.core.util.Close;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AbstractEnvSupplier is an abstract class that provides a mechanism to load environment config
 * from various resources using different parsers.
 */
public abstract class AbstractEnvSupplier implements EnvSupplier {

    private static final Logger logger = LoggerFactory.getLogger(ConfigEnvSupplier.class);

    @Inject
    private Map<String, ConfigParser> parsers;

    @Inject
    private List<BootResourceLoader> loaders;

    /**
     * Loads configurations from the specified resources.
     *
     * @param resources The paths of the resources to load configurations from.
     * @return A {@link Map} containing the loaded configurations, or {@code null} if no configurations could be loaded.
     */
    protected Map<String, Object> loadConfigs(String... resources) {
        if (resources != null) {
            for (String resource : resources) {
                Map<String, Object> result = loadConfigs(new BootResource(null, null, resource));
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * Loads configurations from the specified resources.
     *
     * @param resources The resource to load configurations from.
     * @return A {@link Map} containing the loaded configurations, or {@code null} if no configurations could be loaded.
     */
    protected Map<String, Object> loadConfigs(BootResource... resources) {
        if (resources != null) {
            for (BootResource resource : resources) {
                Map<String, Object> result = loadConfigs(resource);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * Loads configurations from the specified {@link BootResource}.
     *
     * @param resource The {@link BootResource} to load configurations from.
     * @return A {@link Map} containing the loaded configurations, or {@code null} if the resource could not be parsed.
     */
    protected Map<String, Object> loadConfigs(BootResource resource) {

        String ext = resource.getExtension();
        ConfigParser parser = parsers.get(ext);
        if (parser != null) {
            InputStream stream = getResource(resource);
            if (stream != null) {
                try {
                    Map<String, Object> result = parse(stream, parser);
                    logger.info("[LiveAgent] Successfully load config from {}", resource);
                    return result;
                } catch (Throwable e) {
                    logger.warn("[LiveAgent] Failed to load config from {}, caused by {}", resource, e.getMessage());
                    return new HashMap<>();
                } finally {
                    Close.instance().close(stream);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves an {@link InputStream} for the specified {@link BootResource}.
     *
     * @param resource The {@link BootResource} to retrieve the input stream for.
     * @return An {@link InputStream} for the resource, or {@code null} if the resource could not be accessed.
     */
    protected InputStream getResource(BootResource resource) {
        for (BootResourceLoader loader : loaders) {
            if (loader.support(resource.getSchema())) {
                try {
                    InputStream stream = loader.getResource(resource);
                    if (stream != null) {
                        return stream;
                    }
                } catch (IOException ignored) {
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
