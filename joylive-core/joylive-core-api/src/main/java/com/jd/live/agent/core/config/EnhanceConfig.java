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

    private String javaVersion = SUPPORT_JAVA_VERSION;

    /**
     * enable reTransform
     */
    private boolean reTransformEnabled;

    /**
     * exclude class name prefix
     */
    private Set<String> excludePrefixes;

    /**
     * exclude class names
     */
    private Set<String> excludeTypes;

    /**
     * exclude interface name
     */
    private Set<String> excludeInterfaces;

    /**
     * exclude classes which is loaded by the classloader
     */
    private Set<String> excludeClassLoaders;

    /**
     * log enhance
     */
    private boolean logEnhance;

    /**
     * output enhance class file
     */
    private boolean outputEnhance;

    public boolean isExclude(Class<?> type) {
        String name = type.getName();
        if (excludeTypes != null) {
            if (excludeTypes.contains(name)) {
                return true;
            }
        }
        if (excludePrefixes != null) {
            for (String excludePrefix : excludePrefixes) {
                if (name.startsWith(excludePrefix)) {
                    return true;
                }
            }
        }
        if (excludeInterfaces != null) {
            Class<?>[] intfs = type.getInterfaces();
            for (Class<?> intf : intfs) {
                if (excludeInterfaces.contains(intf.getName())) {
                    return true;
                }
            }
        }
        if (excludeClassLoaders != null) {
            return excludeClassLoaders.contains(type.getClassLoader().getClass().getName());
        }
        return false;
    }
}
