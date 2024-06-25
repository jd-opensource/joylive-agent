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
package com.jd.live.agent.governance.context.bag.service;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.governance.context.bag.CargoRequire;

import java.util.Optional;

/**
 * ServiceCargoRequire is an implementation of the CargoRequire interface that provides
 * the necessary cargo requirements for microservice streaming scenarios. It uses
 * a ServiceConfig instance to determine the specific prefixes for microservice-related configurations.
 *
 * @since 1.0.0
 */
@Injectable
@Extension("ServiceCargoRequire")
public class ServiceCargoRequire implements CargoRequire {

    @Inject(ServiceConfig.COMPONENT_SERVICE_CONFIG)
    private ServiceConfig serviceConfig;

    private static final String[] EMPTY_ARRAY = new String[0];

    @Override
    public String[] getNames() {
        return Optional.ofNullable(serviceConfig)
                .map(ServiceConfig::getTransmitKeys)
                .map(list -> list.toArray(EMPTY_ARRAY))
                .orElse(EMPTY_ARRAY);
    }

    @Override
    public String[] getPrefixes() {
        return Optional.ofNullable(serviceConfig)
                .map(ServiceConfig::getTransmitPrefixes)
                .map(list -> list.toArray(EMPTY_ARRAY))
                .orElse(EMPTY_ARRAY);
    }
}
