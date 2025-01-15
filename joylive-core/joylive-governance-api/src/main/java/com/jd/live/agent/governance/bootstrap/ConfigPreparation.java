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
import com.jd.live.agent.core.bootstrap.ApplicationListener.ApplicationListenerAdapter;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.config.ConfigCenterConfig;
import com.jd.live.agent.governance.subscription.config.ConfigCenter;
import com.jd.live.agent.governance.subscription.config.Configurator;

import static com.jd.live.agent.core.util.option.Converts.getBoolean;
import static com.jd.live.agent.governance.subscription.config.ConfigListener.SYSTEM_ALL;

/**
 * An extension that prepares config for the application.
 *
 * @since 1.6.0
 */
@Injectable
@Extension(value = "ConfigPreparation", order = ApplicationListener.ORDER_BOOTSTRAP)
public class ConfigPreparation extends ApplicationListenerAdapter {

    @Inject(value = ConfigCenter.COMPONENT_CONFIG_CENTER, component = true, nullable = true)
    private ConfigCenter configCenter;

    @Override
    public void onEnvironmentPrepared(ApplicationBootstrapContext context, ApplicationEnvironment environment) {
        if (configCenter != null) {
            configCenter.ifPresent(configurator -> environment.addFirst(new LivePropertySource(configurator)));
        }
    }

    @Override
    public void onStarted(ApplicationContext context) {
        if (configCenter != null) {
            ConfigCenterConfig config = configCenter.getConfig();
            if (getBoolean(config.getProperty(ConfigCenterConfig.KEY_REFRESH_ENVIRONMENT_ENABLED), false)) {
                configCenter.ifPresent(configurator -> configurator.addListener(SYSTEM_ALL, e -> {
                    context.refreshEnvironment();
                    return true;
                }));
            }
        }
    }

    private static class LivePropertySource implements ApplicationPropertySource {

        private final Configurator configurator;

        LivePropertySource(Configurator configurator) {
            this.configurator = configurator;
        }

        @Override
        public String getProperty(String name) {
            Object property = configurator.getProperty(name);
            return property == null ? null : property.toString();
        }

        @Override
        public String getName() {
            return configurator.getName();
        }
    }
}
