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
package com.jd.live.agent.implement.service.policy.file;

import com.jd.live.agent.core.config.ConfigWatcher;
import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.service.AbstractFileSyncer;
import lombok.Getter;

/**
 * ServiceFileSyncer
 */
@Getter
@Injectable
@Extension("ServiceFileSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_MICROSERVICE_TYPE, value = "file")
public class ServiceFileSyncer extends AbstractFileSyncer {

    private static final String CONFIG_MICROSERVICE = "microservice.json";

    @Config(SyncConfig.SYNC_MICROSERVICE)
    private SyncConfig syncConfig = new SyncConfig();

    @Override
    public String getType() {
        return ConfigWatcher.TYPE_SERVICE_SPACE;
    }

    @Override
    protected String getResource() {
        String result = super.getResource();
        return isResource(result) ? result : CONFIG_MICROSERVICE;
    }

    @Override
    protected String getDefaultResource() {
        return CONFIG_MICROSERVICE;
    }
}
