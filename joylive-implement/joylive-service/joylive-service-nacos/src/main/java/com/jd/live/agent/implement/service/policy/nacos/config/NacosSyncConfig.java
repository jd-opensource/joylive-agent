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

import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.governance.policy.service.MergePolicy;
import lombok.Getter;
import lombok.Setter;

/**
 * NacosSyncConfig is responsible for Nacos settings.
 */
@Getter
@Setter
public class NacosSyncConfig extends SyncConfig {

    /**
     * nacos server address
     */
    private String serverAddr;

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


    /**
     * service nacos group
     * note: nacos data id  is service name
     */
    private String serviceNacosGroup;

    /**
     * lane space nacos group
     * note: nacos data id  is  "lanes.json"
     */
    private String laneSpaceNacosGroup;

    /**
     * live space nacos group
     * note 1: workspaces nacos data_id is "workspaces.json"
     * note 2: liveSpace nacos data_id is workspaceId which is from workspaces
     */
    private String liveSpaceNacosGroup;


    private MergePolicy policy = MergePolicy.ALL;

}
