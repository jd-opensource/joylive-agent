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
package com.jd.live.agent.plugin.application.springboot.v2.context;

import com.jd.live.agent.governance.bootstrap.ConfigurableAppContext;
import com.jd.live.agent.governance.subscription.config.ConfigCenter;
import com.jd.live.agent.plugin.application.springboot.v2.config.SpringConfigRefresher;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * An implementation of the ApplicationContext interface for Spring-based applications.
 */
public class SpringAppContext implements ConfigurableAppContext {

    private final ConfigurableApplicationContext context;

    /**
     * The refresher responsible for refreshing the environment.
     */
    private final SpringConfigRefresher refresher;

    /**
     * Constructs a new SpringApplicationContext instance.
     *
     * @param context The Spring application context.
     */
    public SpringAppContext(ConfigurableApplicationContext context) {
        this.context = context;
        this.refresher = new SpringConfigRefresher(context);
    }

    @Override
    public String getProperty(String name) {
        return context.getEnvironment().getProperty(name);
    }

    @Override
    public void subscribe(ConfigCenter configCenter) {
        refresher.subscribe(configCenter);
    }

    public ConfigurableApplicationContext getContext() {
        return context;
    }

}
