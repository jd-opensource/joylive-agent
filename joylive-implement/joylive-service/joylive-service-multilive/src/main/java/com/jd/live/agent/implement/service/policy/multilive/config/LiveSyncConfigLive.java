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
package com.jd.live.agent.implement.service.policy.multilive.config;

import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.governance.service.sync.SyncAddress.LiveSpaceAddress;
import com.jd.live.agent.governance.service.sync.SyncAddress.ServiceAddress;
import lombok.Setter;

import static com.jd.live.agent.core.util.StringUtils.url;

/**
 * LiveSyncConfig
 *
 * @since 1.0.0
 */
@Setter
public class LiveSyncConfigLive extends SyncConfig implements ServiceAddress, LiveSpaceAddress {

    private String spacesUrl;

    private String spaceUrl;

    private String serviceUrl;

    @Override
    public String getSpacesUrl() {
        if (spacesUrl == null && getUrl() != null) {
            spacesUrl = url(getUrl(), "/workspaces");
        }
        return spacesUrl;
    }

    @Override
    public String getSpaceUrl() {
        if (spaceUrl == null && getUrl() != null) {
            spaceUrl = url(getUrl(), "/workspaces/${space_id}/version/${space_version}");
        }
        return spaceUrl;
    }

    @Override
    public String getServiceUrl() {
        if (serviceUrl == null && getUrl() != null) {
            serviceUrl = url(getUrl(), "/services/${service_name}/version/${service_version}");
        }
        return serviceUrl;
    }
}
