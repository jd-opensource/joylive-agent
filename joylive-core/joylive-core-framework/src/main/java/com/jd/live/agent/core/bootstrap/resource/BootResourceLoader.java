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
@Extensible("BootResourceLoader")
public interface BootResourceLoader {

    int ORDER_CLASSPATH = 0;

    int ORDER_TOMCAT = ORDER_CLASSPATH + 10;

    int ORDER_TONG_WEB = ORDER_TOMCAT + 10;

    int ORDER_FILE = ORDER_TONG_WEB + 10;

    /**
     * Finds the resource with the given name and returns its input stream.
     * The resource is identified by the provided {@link BootResource} object.
     * If the resource cannot be found, this method returns {@code null}.
     *
     * @param resource The {@link BootResource} object representing the resource to find.
     * @return An {@link InputStream} for reading the resource, or {@code null} if the resource could not be found.
     * @throws IOException If an I/O error occurs while attempting to access the resource.
     */
    InputStream getResource(BootResource resource) throws IOException;

    boolean support(String schema);

}