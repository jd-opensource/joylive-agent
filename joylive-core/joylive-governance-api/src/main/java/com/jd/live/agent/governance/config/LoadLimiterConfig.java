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
package com.jd.live.agent.governance.config;

import com.jd.live.agent.governance.policy.service.limit.LoadLimitThrottle;
import com.jd.live.agent.governance.policy.service.limit.LoadMetric;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration class load rate limiter settings.
 */
public class LoadLimiterConfig {

    public static final int DEFAULT_WINDOW = 2000;

    @Getter
    @Setter
    @Deprecated
    private Integer cpuUsage;

    @Getter
    @Setter
    @Deprecated
    private Integer loadUsage;

    @Getter
    @Setter
    private List<LoadLimitThrottle> throttles;

    @Setter
    private int cpuWindow = DEFAULT_WINDOW;

    private transient List<LoadLimitThrottle> cache;

    public int getCpuWindow() {
        return cpuWindow <= 0 ? DEFAULT_WINDOW : cpuWindow;
    }

    public boolean isEmpty() {
        return cache == null || cache.isEmpty();
    }

    public int getRatio(LoadMetric metric) {
        return LoadLimitThrottle.getRatio(metric, cache);
    }

    protected void initialize() {
        cache = throttles == null ? new ArrayList<>() : new ArrayList<>(throttles);
        if (cpuUsage != null || loadUsage != null) {
            cache.add(new LoadLimitThrottle(cpuUsage, loadUsage, 100));
        }
        Collections.sort(cache);
    }
}

