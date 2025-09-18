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

import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.governance.request.HostTransformer;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

import static com.jd.live.agent.core.util.template.Template.evaluate;
import static com.jd.live.agent.governance.request.HostTransformer.KEY_UNIT;

/**
 * LiveConfig is a configuration class that holds the keys for managing live settings within a system.
 */
@Getter
@Setter
public class LiveConfig {

    private boolean fallbackLocationIfNoSpace;

    private boolean hostEnabled;

    private Set<String> hosts;

    private String hostExpression;

    private transient final LazyObject<Set<String>> hostCache = new LazyObject<>(() -> {
        if (hosts == null) {
            return new HashSet<>();
        }
        Set<String> result = new HashSet<>();
        hosts.forEach(host -> result.add(host.toLowerCase()));
        return result;
    });

    public boolean isEnabled(String host) {
        if (hostEnabled && host != null && !host.isEmpty()) {
            Set<String> cache = hostCache.get();
            return cache.isEmpty() || cache.contains(host);
        }
        return false;
    }

    public HostTransformer getHostTransformer(String host) {
        return !isEnabled(host) ? null : (h, ctx) -> evaluate(hostExpression, ctx, h, c -> c.containsKey(KEY_UNIT));
    }
}

