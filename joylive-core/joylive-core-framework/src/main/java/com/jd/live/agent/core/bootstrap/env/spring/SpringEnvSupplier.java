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
package com.jd.live.agent.core.bootstrap.env.spring;

import com.jd.live.agent.core.bootstrap.env.AbstractEnvSupplier;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.util.type.ValuePath;

import java.util.Map;

@Injectable
@Extension("SpringEnvSupplier")
public class SpringEnvSupplier extends AbstractEnvSupplier {

    private static final String KEY_SPRING_APPLICATION_NAME = "spring.application.name";

    private static final String KEY_SPRING_APPLICATION_NAME1 = "spring-application-name";

    private static final String RESOURCE_SPRINGBOOT_APPLICATION_PROPERTIES = "application.properties";

    private static final String RESOURCE_SPRINGBOOT_APPLICATION_YAML = "application.yaml";

    private static final String RESOURCE_SPRINGBOOT_APPLICATION_YML = "application.yml";

    private static final ValuePath APP_PATH = new ValuePath(KEY_SPRING_APPLICATION_NAME);

    public SpringEnvSupplier() {
        super(RESOURCE_SPRINGBOOT_APPLICATION_PROPERTIES,
                RESOURCE_SPRINGBOOT_APPLICATION_YAML,
                RESOURCE_SPRINGBOOT_APPLICATION_YML);
    }

    @Override
    public void process(Map<String, Object> env) {
        if (!env.containsKey(Application.KEY_APPLICATION_NAME)) {
            Map<String, Object> configs = loadConfigs();
            if (configs != null) {
                String name = (String) configs.get(KEY_SPRING_APPLICATION_NAME);
                name = name == null ? (String) configs.get(KEY_SPRING_APPLICATION_NAME1) : name;
                name = name == null ? (String) APP_PATH.get(configs) : name;
                if (name != null && !name.isEmpty()) {
                    env.put(Application.KEY_APPLICATION_NAME, name);
                }
            }
        }
    }
}
