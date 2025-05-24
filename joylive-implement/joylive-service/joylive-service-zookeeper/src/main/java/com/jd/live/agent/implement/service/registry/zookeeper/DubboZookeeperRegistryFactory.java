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
package com.jd.live.agent.implement.service.registry.zookeeper;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.RegistryClusterConfig;
import com.jd.live.agent.governance.registry.RegistryFactory;
import com.jd.live.agent.governance.registry.RegistryService;

/**
 * A factory implementation for creating instances of {@link DubboZookeeperRegistry}.
 * This class is annotated with {@link Extension} to indicate it provides the "dubbo-zookeeper" extension.
 */
@Injectable
@Extension("dubbo-zookeeper")
public class DubboZookeeperRegistryFactory implements RegistryFactory {

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    @Inject(ObjectParser.JSON)
    private ObjectParser parser;

    /**
     * Creates a new instance of {@link DubboZookeeperRegistry} using the provided {@link RegistryClusterConfig}.
     *
     * @param config The configuration used to initialize the {@link DubboZookeeperRegistry}.
     * @return A new instance of {@link DubboZookeeperRegistry}.
     */
    @Override
    public RegistryService create(RegistryClusterConfig config) {
        return new DubboZookeeperRegistry(config, timer, parser);
    }
}
