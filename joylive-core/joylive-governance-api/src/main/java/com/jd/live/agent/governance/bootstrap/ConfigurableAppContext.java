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

import com.jd.live.agent.core.bootstrap.AppContext;
import com.jd.live.agent.governance.subscription.config.ConfigCenter;

/**
 * Represents the application context, which holds all the beans and their dependencies.
 *
 * @since 1.6.0
 */
public interface ConfigurableAppContext extends AppContext {

    /**
     * Subscribes the application context to the given ConfigCenter instance.
     *
     * @param configCenter The ConfigCenter instance to which the application context will be subscribed.
     */
    void subscribe(ConfigCenter configCenter);

}

