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
package com.jd.live.agent.core.bootstrap.config;

import com.jd.live.agent.bootstrap.classloader.ResourcerType;
import com.jd.live.agent.core.bootstrap.EnvSupplier;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.InjectLoader;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.parser.ConfigParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Map;

@Injectable
@Extension("ConfigEnvSupplier")
public class ConfigEnvSupplier implements EnvSupplier {

    private static final String RESOURCE_LIVE_AGENT_PROPERTIES = "liveagent.properties";

    @Inject("properties")
    @InjectLoader(ResourcerType.CORE_IMPL)
    private ConfigParser propertiesParser;

    @Override
    public void process(Map<String, Object> env) {
        URL url = ClassLoader.getSystemClassLoader().getResource(RESOURCE_LIVE_AGENT_PROPERTIES);
        if (url != null) {
            try {
                Map<String, Object> map = parse(url.openStream(), propertiesParser);
                map.forEach(env::putIfAbsent);
            } catch (Exception ignore) {
            }
        }
    }

    private Map<String, Object> parse(InputStream inputStream, ConfigParser parser) throws Exception {
        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return parser.parse(reader);
        }
    }
}
