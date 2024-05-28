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

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * SyncConfig
 *
 * @since 1.0.0
 */
@Setter
@Getter
public class SyncConfig {

    public static final String SYNC = "agent.sync";

    public static final String SYNC_MICROSERVICE = SYNC + ".microservice";

    public static final String SYNC_MICROSERVICE_TYPE = SYNC_MICROSERVICE + ".type";

    public static final String SYNC_LIVE_SPACE = SYNC + ".liveSpace";

    public static final String SYNC_LIVE_SPACE_TYPE = SYNC_LIVE_SPACE + ".type";

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
}
