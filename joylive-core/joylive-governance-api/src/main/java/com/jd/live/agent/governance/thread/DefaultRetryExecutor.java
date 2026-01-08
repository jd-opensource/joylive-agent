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
package com.jd.live.agent.governance.thread;

import com.jd.live.agent.core.extension.ExtensionInitializer;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.thread.NamedThreadFactory;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.config.RetryConfig;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of RetryExecutor.
 * Provides a shared scheduled thread pool for retry operations across the application.
 */
@Injectable
@Extension("default")
public class DefaultRetryExecutor implements RetryExecutor, ExtensionInitializer {

    private ScheduledExecutorService scheduler;

    @Inject(GovernanceConfig.COMPONENT_GOVERNANCE_CONFIG)
    private GovernanceConfig config;

    @Override
    public void submit(RetryTask task) {
        if (task != null) {
            scheduler.execute(task::execute);
        }
    }

    @Override
    public void submit(RetryTask task, long delay, TimeUnit unit) {
        if (task == null) {
            return;
        }
        if (delay <= 0) {
            scheduler.execute(task::execute);
        } else {
            scheduler.schedule(task::execute, delay, unit);
        }
    }

    @Override
    public void initialize() {
        // retry config is not null.
        RetryConfig retryConfig = config.getServiceConfig().getRetryConfig();
        scheduler = Executors.newScheduledThreadPool(retryConfig.getThreads(), new NamedThreadFactory("retry-worker"));
    }
}