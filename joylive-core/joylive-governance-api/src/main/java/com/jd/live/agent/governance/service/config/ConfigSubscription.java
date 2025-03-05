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
package com.jd.live.agent.governance.service.config;

import com.jd.live.agent.core.parser.ConfigParser;
import com.jd.live.agent.governance.subscription.config.ConfigName;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Represents a subscription to a configuration resource.
 * <p>
 * Holds the client, configuration name, parser, and associated configuration data.
 *
 * @param <T> The type of {@link ConfigClientApi} used for the subscription.
 */
@Getter
public class ConfigSubscription<T extends ConfigClientApi> {

    private final T client;

    private final ConfigName name;

    private final ConfigParser parser;

    @Setter
    private Map<String, Object> config;

    public ConfigSubscription(T client, ConfigName name, ConfigParser parser) {
        this.client = client;
        this.name = name;
        this.parser = parser;
    }

}
