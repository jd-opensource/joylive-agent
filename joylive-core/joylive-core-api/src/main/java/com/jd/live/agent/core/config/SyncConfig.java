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
package com.jd.live.agent.core.config;

import lombok.Getter;
import lombok.Setter;

import java.net.URL;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.jd.live.agent.core.util.StringUtils.isEmpty;
import static com.jd.live.agent.core.util.StringUtils.url;

/**
 * SyncConfig
 *
 * @since 1.0.0
 */
@Setter
@Getter
public class SyncConfig {

    public static final String CONFIG_PREFIX = "govern";

    public static final String SYNC = "agent.sync";

    public static final String SYNC_MICROSERVICE = SYNC + ".microservice";

    public static final String SYNC_MICROSERVICE_ENABLED = SYNC_MICROSERVICE + ".enabled";

    public static final String SYNC_MICROSERVICE_TYPE = SYNC_MICROSERVICE + ".type";

    public static final String SYNC_MICROSERVICE_MERGE_POLICY = SYNC_MICROSERVICE + ".policy";

    public static final String SYNC_LIVE_SPACE = SYNC + ".liveSpace";

    public static final String SYNC_LIVE_SPACE_TYPE = SYNC_LIVE_SPACE + ".type";

    public static final String SYNC_LIVE_SPACE_SERVICE = SYNC_LIVE_SPACE + ".service";

    public static final String SYNC_LANE_SPACE = SYNC + ".laneSpace";

    public static final String SYNC_LANE_SPACE_TYPE = SYNC_LANE_SPACE + ".type";

    private String url;

    private String type;

    private long interval = 5000;

    private long timeout = 3000;

    private long initialTimeout = 20000;

    private long delay = 0;

    private long fault = 5000;

    private int concurrency;

    private Map<String, String> headers;

    private Map<String, String> configs;

    public String getConfig(String key, String defaultValue) {
        String value = configs == null || key == null ? null : configs.get(key);
        return value == null || value.isEmpty() ? defaultValue : value;
    }

    public void header(BiConsumer<String, String> consumer) {
        if (headers != null && consumer != null) {
            headers.forEach(consumer);
        }
    }

    /**
     * Retrieves the resource URL from the sync configuration.
     *
     * @param defaultResource the default resource.
     * @return the resource URL as a String
     */
    public String getResource(String defaultResource) {
        return isResource(url) ? url : defaultResource;
    }

    /**
     * Checks if the given file path represents a valid configuration file.
     *
     * @param file The file path to check.
     * @return true if the file is a valid configuration file, false otherwise.
     */
    protected boolean isResource(String file) {
        if (file == null || file.isEmpty()) {
            return false;
        } else if (file.startsWith("http://") || file.startsWith("https://")) {
            return false;
        } else if (file.startsWith("${") && file.endsWith("}")) {
            return false;
        } else {
            URL resource = getClass().getClassLoader().getResource(file);
            return resource != null;
        }
    }

    /**
     * Returns the path to use for a request, based on the provided path and default path.
     *
     * @param path        The path to use if it is not empty and does not start with a slash.
     * @param defaultPath The default path to use if the provided path is empty.
     * @return The path to use for the request.
     */
    protected String getPath(String path, String defaultPath) {
        String root = getUrl();
        if (isEmpty(path) && root != null) {
            return url(root, defaultPath);
        } else if (!isEmpty(path) && path.startsWith("/") && root != null) {
            return url(root, path);
        }
        return path;
    }
}
