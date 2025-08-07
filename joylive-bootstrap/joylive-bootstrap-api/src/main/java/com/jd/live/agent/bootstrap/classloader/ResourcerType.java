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
package com.jd.live.agent.bootstrap.classloader;

import lombok.Getter;

/**
 * Enum defining different types of resource or classloader types that can be used in conjunction with
 * the InjectLoader annotation. This allows for specifying the source from which a field should
 * load its resources or classes.
 */
@Getter
public enum ResourcerType {

    /**
     * Represents the core classloader type. This can be used for loading resources or classes that
     * are considered part of the core functionality of the application.
     */
    CORE("classloader-core"),

    /**
     * Represents the core implementation classloader type. This is typically used for loading
     * implementation-specific resources or classes that extend the core functionality.
     */
    CORE_IMPL("classloader-core-impl"),

    /**
     * Represents the plugin classloader type. This can be used for loading resources or classes from
     * plugins or external modules that add additional functionality to the application.
     */
    PLUGIN("classloader-plugin") {
        @Override
        boolean fallback() {
            return true;
        }
    };

    // The name associated with the classloader type.
    private final String name;

    /**
     * Constructor for the enum that sets the name associated with the classloader type.
     *
     * @param name The name that represents this classloader type.
     */
    ResourcerType(String name) {
        this.name = name;
    }

    /**
     * Indicates whether fallback is enabled.
     *
     * @return false, fallback is disabled by default
     */
    boolean fallback() {
        return false;
    }
}

