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
package com.jd.live.agent.core.config;

import com.jd.live.agent.bootstrap.util.Inclusion;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * EnhanceConfig
 *
 * @since 1.0.0
 */
@Setter
@Getter
public class EnhanceConfig {

    public static final String SUPPORT_JAVA_VERSION = "[,1.8);[1.8.0_60,]";

    public static final String COMPONENT_ENHANCE_CONFIG = "enhanceConfig";

    private static final int DEFAULT_POOL_CLEAN_INTERVAL = 60 * 1000;

    private String javaVersion = SUPPORT_JAVA_VERSION;

    private long poolExpireTime = 10 * 60 * 1000;

    private long poolCleanInterval = DEFAULT_POOL_CLEAN_INTERVAL;

    /**
     * exclude class names
     */
    private Set<String> excludeTypes;

    /**
     * exclude class name prefix
     */
    private Set<String> excludePrefixes;

    /**
     * exclude classes which is loaded by the classloader
     */
    private Set<String> excludeClassLoaders;

    /**
     * handle java module
     */
    private Set<String> edgeClasses;

    private boolean shutdownOnError;

    public boolean isExclude(Class<?> type) {
        String name = type.getName();
        if (Inclusion.test(excludeTypes, excludePrefixes, false, name)) {
            return true;
        }
        if (excludeClassLoaders != null) {
            return excludeClassLoaders.contains(type.getClassLoader().getClass().getName());
        }
        return false;
    }

    public long getPoolCleanInterval() {
        return poolCleanInterval <= 0 ? DEFAULT_POOL_CLEAN_INTERVAL : poolCleanInterval;
    }
}
