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
package com.jd.live.agent.core.bootstrap.spring;

import com.jd.live.agent.bootstrap.classloader.ResourcerType;
import com.jd.live.agent.core.bootstrap.EnvSupplier;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.InjectLoader;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ConfigParser;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.util.type.ValuePath;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Map;

@Injectable
@Extension("SpringEnvSupplier")
public class SpringEnvSupplier implements EnvSupplier {

    private static final String KEY_SPRING_APPLICATION_NAME = "spring.application.name";

    private static final String RESOURCE_SPRINGBOOT_APPLICATION_PROPERTIES = "application.properties";

    private static final String RESOURCE_SPRINGBOOT_APPLICATION_YAML = "application.yaml";

    private static final String RESOURCE_SPRINGBOOT_APPLICATION_YML = "application.yml";

    private static final String RESOURCE_FAT_SPRINGBOOT_APPLICATION_PROPERTIES = "BOOT-INF/classes/application.properties";

    private static final String RESOURCE_FAT_SPRINGBOOT_APPLICATION_YAML = "BOOT-INF/classes/application.yaml";

    private static final String RESOURCE_FAT_SPRINGBOOT_APPLICATION_YML = "BOOT-INF/classes/application.yml";

    private static final String KEY_SPRING_APPLICATION_NAME1 = "spring-application-name";

    private static final ValuePath APP_PATH = new ValuePath(KEY_SPRING_APPLICATION_NAME);

    @Inject(ObjectParser.YAML)
    @InjectLoader(ResourcerType.CORE_IMPL)
    private ConfigParser yamlParser;

    @Inject(ConfigParser.PROPERTIES)
    @InjectLoader(ResourcerType.CORE_IMPL)
    private ConfigParser propertiesParser;

    private final ApplicationResource[] resources = new ApplicationResource[]{
            new ApplicationResource(RESOURCE_SPRINGBOOT_APPLICATION_PROPERTIES, ResourceType.PROPERTIES),
            new ApplicationResource(RESOURCE_SPRINGBOOT_APPLICATION_YAML, ResourceType.YAML),
            new ApplicationResource(RESOURCE_SPRINGBOOT_APPLICATION_YML, ResourceType.YAML),
            new ApplicationResource(RESOURCE_FAT_SPRINGBOOT_APPLICATION_PROPERTIES, ResourceType.PROPERTIES),
            new ApplicationResource(RESOURCE_FAT_SPRINGBOOT_APPLICATION_YAML, ResourceType.YAML),
            new ApplicationResource(RESOURCE_FAT_SPRINGBOOT_APPLICATION_YML, ResourceType.YAML),
    };

    @Override
    public void process(Map<String, Object> env) {
        if (!env.containsKey(Application.KEY_APPLICATION_NAME)) {
            String name = getApplicationName(env);
            if (name != null && !name.isEmpty()) {
                env.put(Application.KEY_APPLICATION_NAME, name);
            }
        }
    }

    protected String getApplicationName(Map<String, Object> env) {
        String result = (String) env.getOrDefault(KEY_SPRING_APPLICATION_NAME, (String) env.get(KEY_SPRING_APPLICATION_NAME1));
        if (result == null || result.isEmpty()) {
            try {
                ClassLoader classLoader = ClassLoader.getSystemClassLoader();
                for (ApplicationResource resource : resources) {
                    URL url = classLoader.getResource(resource.getName());
                    if (url != null) {
                        if (resource.getResourceType() == ResourceType.PROPERTIES) {
                            result = (String) parse(url.openStream(), propertiesParser).get(KEY_SPRING_APPLICATION_NAME);
                        } else {
                            result = (String) APP_PATH.get(parse(url.openStream(), yamlParser));
                        }
                        break;
                    }
                }
            } catch (Exception ignore) {
            }
        }
        return result;
    }

    private Map<String, Object> parse(InputStream inputStream, ConfigParser parser) throws Exception {
        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return parser.parse(reader);
        }
    }

    @Getter
    private static class ApplicationResource {

        private final String name;

        private final ResourceType resourceType;

        ApplicationResource(String name, ResourceType resourceType) {
            this.name = name;
            this.resourceType = resourceType;
        }

    }

    private enum ResourceType {

        PROPERTIES,

        YAML

    }
}
