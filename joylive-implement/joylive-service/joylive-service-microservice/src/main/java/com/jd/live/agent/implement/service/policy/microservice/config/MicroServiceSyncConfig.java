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
package com.jd.live.agent.implement.service.policy.microservice.config;

import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.governance.service.sync.SyncAddress.LaneSpaceAddress;
import com.jd.live.agent.governance.service.sync.SyncAddress.ServiceAddress;
import lombok.Setter;

/**
 * MicroServiceSyncConfig
 *
 * @since 1.0.0
 */
@Setter
public class MicroServiceSyncConfig extends SyncConfig implements ServiceAddress, LaneSpaceAddress {

    private MicroServiceConfig jmsf = new MicroServiceConfig();

    @Override
    public String getLaneSpacesUrl() {
        return getPath(jmsf.spacesUrl, "/laneSpaces");
    }

    @Override
    public String getLaneSpaceUrl() {
        return getPath(jmsf.spaceUrl, "/laneSpace/${space_id}/version/${space_version}");
    }

    @Override
    public String getServiceUrl() {
        return getPath(jmsf.serviceUrl, "/space/${space}/service/${service_name}/version/${service_version}?application=${application}");
    }
}
