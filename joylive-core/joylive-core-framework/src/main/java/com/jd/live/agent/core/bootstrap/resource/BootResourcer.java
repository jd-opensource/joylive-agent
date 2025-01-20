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
package com.jd.live.agent.core.bootstrap.resource;

import com.jd.live.agent.core.extension.annotation.Extensible;

import java.io.IOException;
import java.io.InputStream;

/**
 * An interface for finding resources.
 */
@Extensible("BootResourcer")
public interface BootResourcer {

    int ORDER_CLASSLOADER = 0;

    int ORDER_TOMCAT = 10;

    /**
     * Finds the resource with the given name and returns its input stream.
     *
     * @param resource The name of the resource to find.
     * @return The input stream of the resource, or null if the resource could not be found.
     */
    InputStream getResource(String resource) throws IOException;
}