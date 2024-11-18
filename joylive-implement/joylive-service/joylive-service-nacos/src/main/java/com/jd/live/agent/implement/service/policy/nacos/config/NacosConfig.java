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
package com.jd.live.agent.implement.service.policy.nacos.config;

import lombok.Getter;
import lombok.Setter;

import static com.jd.live.agent.core.config.SyncConfig.CONFIG_PREFIX;

/**
 * NacosSyncConfig is responsible for Nacos settings.
 */

@Getter
@Setter
public class NacosConfig {

    /**
     * nacos namespace
     */
    private String namespace;

    /**
     * nacos username
     */
    private String username;

    /**
     * nacos password
     */
    private String password;

    private String serviceKeyTemplate = CONFIG_PREFIX + "-service-${name}";

    private String serviceGroup = "DEFAULT_GROUP";

    private String laneSpacesKey = CONFIG_PREFIX + "-laneSpaces";

    private String laneSpaceKeyTemplate = CONFIG_PREFIX + "-laneSpace-${id}";

    private String laneSpaceGroup = "DEFAULT_GROUP";

    private String liveSpacesKey = CONFIG_PREFIX + "-liveSpaces";

    private String liveSpaceKeyTemplate = CONFIG_PREFIX + "-liveSpace-${id}";

    private String liveServiceTemplate = CONFIG_PREFIX + "-liveService-${name}";

    private String liveSpaceGroup = "DEFAULT_GROUP";

}
