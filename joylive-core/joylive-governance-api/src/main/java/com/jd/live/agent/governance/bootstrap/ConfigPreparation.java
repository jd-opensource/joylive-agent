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
import com.jd.live.agent.core.bootstrap.AppPropertySource.MapSource;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.config.RefreshConfig;
import com.jd.live.agent.governance.subscription.config.ConfigCenter;
import com.jd.live.agent.governance.subscription.config.ConfiguratorSource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.jd.live.agent.core.util.StringUtils.*;

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

    @Inject(AppEnv.COMPONENT_APP_ENV)
    private AppEnv env;

    @Override
    public void onLoading(ClassLoader classLoader, Class<?> mainClass) {
        if (configCenter != null && mainClass != null) {
            RefreshConfig refreshConfig = configCenter.getConfig().getRefresh();
            if (refreshConfig.isEmpty()) {
                Set<String> prefixes = new HashSet<>();
                String name = mainClass.getPackage().getName();
                String[] packages = split(name, CHAR_DOT);
                if (packages.length > 3) {
                    packages = Arrays.copyOfRange(packages, 0, 3);
                    name = join(packages, CHAR_DOT);
                }
                prefixes.add(name);
                refreshConfig.setBeanClassPrefixes(prefixes);
            }
        }
    }

    @Override
    public void onEnvironmentPrepared(AppBootstrapContext context, AppEnvironment environment) {
        env.ifPresentRemotes(remotes -> environment.addFirst(new MapSource("LiveAgent.http", remotes)));
        if (configCenter != null) {
            configCenter.ifPresent(configurator -> environment.addFirst(new ConfiguratorSource(configurator)));
        }
    }

    @Override
    public void onStarted(AppContext context) {
        if (configCenter != null && context instanceof ConfigurableAppContext) {
            configCenter.ifPresent(configurator -> ((ConfigurableAppContext) context).subscribe(configCenter));
        }
    }
}
