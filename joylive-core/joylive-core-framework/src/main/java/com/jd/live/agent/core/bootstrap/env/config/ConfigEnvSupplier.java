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
package com.jd.live.agent.core.bootstrap.env.config;

import com.jd.live.agent.core.bootstrap.AppEnv;
import com.jd.live.agent.core.bootstrap.EnvSupplier;
import com.jd.live.agent.core.bootstrap.env.AbstractEnvSupplier;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;

import java.util.Map;

@Injectable
@Extension(value = "ConfigEnvSupplier", order = EnvSupplier.ORDER_CONFIG_ENV_SUPPLIER)
public class ConfigEnvSupplier extends AbstractEnvSupplier {

    private static final String RESOURCE_LIVE_AGENT_PROPERTIES = "live-agent.properties";

    @Override
    public void process(AppEnv env) {
        Map<String, Object> map = loadConfigs(RESOURCE_LIVE_AGENT_PROPERTIES);
        if (map != null) {
            map.forEach(env::putIfAbsent);
        }
    }
}
