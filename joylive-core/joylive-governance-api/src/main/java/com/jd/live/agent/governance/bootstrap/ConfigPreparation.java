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
package com.jd.live.agent.governance.bootstrap;

import com.jd.live.agent.core.bootstrap.*;
import com.jd.live.agent.core.bootstrap.AppListener.AppListenerAdapter;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.subscription.config.ConfigCenter;
import com.jd.live.agent.governance.subscription.config.Configurator;

import static com.jd.live.agent.core.util.StringUtils.getKebabToCamel;

/**
 * An extension that prepares config for the application.
 *
 * @since 1.6.0
 */
@Injectable
@Extension(value = "ConfigPreparation", order = AppListener.ORDER_BOOTSTRAP)
public class ConfigPreparation extends AppListenerAdapter {

    @Inject(value = ConfigCenter.COMPONENT_CONFIG_CENTER, component = true, nullable = true)
    private ConfigCenter configCenter;

    @Override
    public void onEnvironmentPrepared(AppBootstrapContext context, AppEnvironment environment) {
        if (configCenter != null) {
            configCenter.ifPresent(configurator -> environment.addFirst(new LivePropertySource(configurator)));
        }
    }

    @Override
    public void onStarted(AppContext context) {
        if (configCenter != null && context instanceof ConfigurableAppContext) {
            configCenter.ifPresent(configurator -> ((ConfigurableAppContext) context).subscribe(configCenter));
        }
    }

    private static class LivePropertySource implements AppPropertySource {

        private final Configurator configurator;

        LivePropertySource(Configurator configurator) {
            this.configurator = configurator;
        }

        @Override
        public String getProperty(String name) {
            Object property = configurator.getProperty(name);
            if (property == null) {
                String key = getKebabToCamel(name);
                if (key != null) {
                    property = configurator.getProperty(key);
                }
            }
            return property == null ? null : property.toString();
        }

        @Override
        public String getName() {
            return configurator.getName();
        }
    }
}
